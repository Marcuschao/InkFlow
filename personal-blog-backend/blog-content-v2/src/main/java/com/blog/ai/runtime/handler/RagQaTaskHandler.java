package com.blog.ai.runtime.handler;

import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.gateway.model.ModelStreamChunk;
import com.blog.ai.gateway.service.AIGatewayService;
import com.blog.ai.model.dto.agent.ChatResponse;
import com.blog.ai.model.dto.agent.ChatRequest;
import com.blog.ai.model.dto.agent.ChatSourceDto;
import com.blog.ai.model.entity.ChatMessage;
import com.blog.ai.rag.RagChatOrchestrator;
import com.blog.ai.rag.dto.SourceChunkDto;
import com.blog.ai.rag.generate.RagGenerationService;
import com.blog.ai.rag.session.ChatSessionService;
import com.blog.ai.runtime.model.AgentEventType;
import com.blog.ai.runtime.model.AgentExecutionContext;
import com.blog.ai.runtime.model.AgentRequest;
import com.blog.ai.runtime.model.AgentResult;
import com.blog.ai.runtime.model.AgentRunStatus;
import com.blog.ai.runtime.model.AgentTaskType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.blog.ai.service.AgentService;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RagQaTaskHandler implements AgentTaskHandler {
    private final RagChatOrchestrator orchestrator;
    private final ChatSessionService sessions;
    private final RagGenerationService generation;
    private final AIGatewayService gateway;
    private final AgentService agentService;

    public RagQaTaskHandler(RagChatOrchestrator orchestrator, ChatSessionService sessions,
                            RagGenerationService generation, AIGatewayService gateway, AgentService agentService) {
        this.orchestrator = orchestrator; this.sessions = sessions; this.generation = generation;
        this.gateway = gateway; this.agentService = agentService;
    }

    @Override public AgentTaskType supports() { return AgentTaskType.RAG_QA; }

    @Override
    public AgentResult execute(AgentRequest request, AgentExecutionContext context) {
        requireQuestion(request);
        ChatResponse response;
        if (request.getArticleId() != null) {
            ChatRequest legacy = new ChatRequest(); legacy.setQuestion(request.getQuestion());
            legacy.setArticleId(request.getArticleId()); legacy.setSessionId(request.getSessionId());
            response = agentService.chat(legacy);
        } else {
            response = orchestrator.chat(request.getQuestion(), request.getSessionId(), context.chatPrincipal());
        }
        return fromResponse(response);
    }

    @Override
    public Flux<AgentTaskChunk> stream(AgentRequest request, AgentExecutionContext context) {
        if (request.getArticleId() != null) return AgentTaskHandler.super.stream(request, context);
        return Flux.defer(() -> {
            requireQuestion(request);
            var session = sessions.ensureSession(request.getSessionId(), context.chatPrincipal(), request.getQuestion());
            String recent = sessions.formatRecentHistory(session.getId(), 12);
            String summary = sessions.compressHistory(session.getId(), request.getQuestion());
            sessions.saveMessage(session.getId(), "user", request.getQuestion(), null);
            RagGenerationService.PreparedRagPrompt prepared = generation.prepare(request.getQuestion(), summary, recent);
            Flux<AgentTaskChunk> retrieval = Flux.concat(
                    Flux.just(AgentTaskChunk.event(AgentEventType.RETRIEVAL_COMPLETED,
                            Map.of("sourceCount", prepared.sources().size(), "degraded", prepared.degraded()))),
                    Flux.fromIterable(prepared.sources()).map(source -> AgentTaskChunk.event(AgentEventType.CITATION,
                            Map.of("source", toChatSource(source)))));
            if (prepared.immediateResult() != null) {
                ChatMessage message = sessions.saveMessage(session.getId(), "assistant",
                        prepared.immediateResult().getAnswer(), prepared.immediateResult().getSources());
                AgentResult result = new AgentResult();
                result.setStatus(AgentRunStatus.COMPLETED); result.setAnswer(prepared.immediateResult().getAnswer());
                result.setSessionId(session.getId()); result.setMessageId(message.getId());
                result.setGrounded(false); result.setConfidence(0); result.setRefusalReason(prepared.immediateResult().getRefusalReason());
                result.setDegraded(prepared.degraded());
                return retrieval.concatWithValues(
                        AgentTaskChunk.event(AgentEventType.DELTA, Map.of("delta", result.getAnswer())),
                        AgentTaskChunk.result(result));
            }
            StringBuilder answer = new StringBuilder();
            int[] inputTokens = {0}; int[] outputTokens = {0}; double[] cost = {0D}; String[] model = {null};
            Flux<AgentTaskChunk> generated = gateway.stream(AiTaskType.RAG, prepared.systemPrompt(), prepared.userPrompt(),
                            context.userId(), context.username())
                    .doOnNext(chunk -> {
                        inputTokens[0] = chunk.getInputTokens(); outputTokens[0] = chunk.getOutputTokens();
                        cost[0] = chunk.getCost(); model[0] = chunk.getModel();
                    })
                    .filter(chunk -> StringUtils.hasText(chunk.getDelta()))
                    .map(chunk -> {
                        answer.append(chunk.getDelta());
                        return AgentTaskChunk.event(AgentEventType.DELTA, Map.of("delta", chunk.getDelta()));
                    })
                    .concatWith(Flux.defer(() -> {
                        List<ChatSourceDto> sources = prepared.sources().stream().map(this::toChatSource).toList();
                        ChatMessage message = sessions.saveMessage(session.getId(), "assistant", answer.toString(), prepared.sources());
                        AgentResult result = new AgentResult(); result.setStatus(AgentRunStatus.COMPLETED); result.setAnswer(answer.toString());
                        result.setSources(sources); result.setSessionId(session.getId()); result.setMessageId(message.getId());
                        result.setGrounded(true); result.setConfidence(prepared.confidence()); result.setDegraded(prepared.degraded());
                        result.setInputTokens(inputTokens[0]); result.setOutputTokens(outputTokens[0]); result.setCost(cost[0]); result.setModel(model[0]);
                        return Flux.just(AgentTaskChunk.result(result));
                    }));
            return retrieval.concatWith(generated);
        });
    }

    private AgentResult fromResponse(ChatResponse response) {
        AgentResult result = new AgentResult(); result.setStatus(AgentRunStatus.COMPLETED);
        result.setAnswer(response.getAnswer()); result.setSources(response.getSources()); result.setSessionId(response.getSessionId());
        result.setMessageId(response.getMessageId()); result.setGrounded(response.isGrounded()); result.setConfidence(response.getConfidence());
        result.setRefusalReason(response.getRefusalReason()); result.setDegraded(response.isDegraded()); return result;
    }
    private ChatSourceDto toChatSource(SourceChunkDto source) {
        ChatSourceDto item = new ChatSourceDto(); item.setId(source.getDocId()); item.setTitle(source.getDocTitle());
        item.setChunkId(source.getChunkId()); item.setOrdinal(source.getOrdinal()); item.setSnippet(source.getSnippet());
        item.setScore(source.getScore()); item.setLink(source.getLink()); return item;
    }
    private void requireQuestion(AgentRequest request) {
        if (!StringUtils.hasText(request.getQuestion())) throw new IllegalArgumentException("question must not be blank");
    }
}
