package com.blog.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.ai.common.support.PageResult;
import com.blog.ai.eval.EvalMetrics;
import com.blog.ai.mapper.*;
import com.blog.ai.model.dto.eval.*;
import com.blog.ai.model.entity.*;
import com.blog.ai.rag.dto.RagAnswerVo;
import com.blog.ai.rag.generate.RagGenerationService;
import com.blog.ai.service.EvalService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvalServiceImpl implements EvalService {
    private final AiEvalDatasetMapper datasetMapper;
    private final AiEvalCaseMapper caseMapper;
    private final AiEvalRunMapper runMapper;
    private final AiEvalResultMapper resultMapper;
    private final AiAnswerFeedbackMapper feedbackMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final RagGenerationService ragGenerationService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<AiEvalDataset> datasets(long page, long size) {
        return PageResult.build(datasetMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<AiEvalDataset>().orderByDesc("id")));
    }

    @Override
    public AiEvalDataset saveDataset(Long id, EvalDatasetRequest request) {
        AiEvalDataset row = id == null ? new AiEvalDataset() : datasetMapper.selectById(id);
        if (row == null) throw new IllegalArgumentException("评测集不存在");
        row.setName(request.getName().trim());
        row.setDescription(request.getDescription());
        row.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        if (id == null) row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        if (id == null) datasetMapper.insert(row); else datasetMapper.updateById(row);
        return row;
    }

    @Override
    @Transactional
    public void deleteDataset(Long id) {
        long runCount = runMapper.selectCount(new QueryWrapper<AiEvalRun>().eq("dataset_id", id));
        if (runCount > 0) throw new IllegalStateException("该评测集已有运行记录，不能删除");
        caseMapper.delete(new QueryWrapper<AiEvalCase>().eq("dataset_id", id));
        datasetMapper.deleteById(id);
    }

    @Override
    public PageResult<AiEvalCase> cases(Long datasetId, long page, long size) {
        return PageResult.build(caseMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<AiEvalCase>().eq("dataset_id", datasetId).orderByDesc("id")));
    }

    @Override
    public AiEvalCase saveCase(Long datasetId, Long id, EvalCaseRequest request) {
        AiEvalCase row = id == null ? new AiEvalCase() : caseMapper.selectById(id);
        if (row == null) throw new IllegalArgumentException("评测用例不存在");
        Long ownerId = datasetId != null ? datasetId : row.getDatasetId();
        if (ownerId == null || datasetMapper.selectById(ownerId) == null) throw new IllegalArgumentException("评测集不存在");
        row.setDatasetId(ownerId);
        row.setQuestion(request.getQuestion().trim());
        row.setExpectedAnswer(request.getExpectedAnswer());
        row.setExpectedDocIds(request.getExpectedDocIds());
        row.setRequiredKeywords(request.getRequiredKeywords());
        row.setForbiddenClaims(request.getForbiddenClaims());
        row.setNoAnswer(request.getNoAnswer() == null ? 0 : request.getNoAnswer());
        row.setTags(request.getTags());
        row.setRemark(request.getRemark());
        row.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        if (id == null) row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        if (id == null) caseMapper.insert(row); else caseMapper.updateById(row);
        return row;
    }

    @Override public void deleteCase(Long id) { caseMapper.deleteById(id); }

    @Override
    public Map<String, Object> importJsonl(Long datasetId, String text) {
        if (datasetMapper.selectById(datasetId) == null) throw new IllegalArgumentException("评测集不存在");
        int success = 0, failed = 0, lineNo = 0;
        List<String> errors = new ArrayList<>();
        for (String raw : (text == null ? "" : text).split("\\R")) {
            lineNo++;
            if (!StringUtils.hasText(raw)) continue;
            try {
                Map<String, Object> data = objectMapper.readValue(raw, new TypeReference<>() {});
                EvalCaseRequest request = fromJson(data);
                saveCase(datasetId, null, request);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("第" + lineNo + "行: " + e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }

    private EvalCaseRequest fromJson(Map<String, Object> data) {
        EvalCaseRequest request = new EvalCaseRequest();
        request.setQuestion(stringValue(data.get("question")));
        if (!StringUtils.hasText(request.getQuestion())) throw new IllegalArgumentException("question 为空");
        request.setExpectedAnswer(stringValue(data.get("expectedAnswer")));
        request.setExpectedDocIds(stringValue(data.get("expectedDocIds")));
        request.setRequiredKeywords(stringValue(data.get("requiredKeywords")));
        request.setForbiddenClaims(stringValue(data.get("forbiddenClaims")));
        Object noAnswer = data.get("noAnswer");
        request.setNoAnswer(Boolean.TRUE.equals(noAnswer) || noAnswer instanceof Number n && n.intValue() == 1 ? 1 : 0);
        request.setTags(stringValue(data.get("tags")));
        request.setRemark(stringValue(data.get("remark")));
        return request;
    }

    private String stringValue(Object value) {
        if (value == null) return null;
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return String.valueOf(value).trim();
    }

    @Override
    public AiEvalRun startRun(Long datasetId, Integer topK) {
        AiEvalDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null || !Integer.valueOf(1).equals(dataset.getEnabled())) {
            throw new IllegalArgumentException("评测集不存在或未启用");
        }
        List<AiEvalCase> cases = caseMapper.selectList(new QueryWrapper<AiEvalCase>()
                .eq("dataset_id", datasetId).eq("enabled", 1).orderByAsc("id"));
        if (cases.isEmpty()) throw new IllegalArgumentException("评测集中没有启用的用例");
        AiEvalRun run = new AiEvalRun();
        run.setDatasetId(datasetId);
        run.setStatus("RUNNING");
        run.setTotalCases(cases.size());
        run.setCompletedCases(0);
        run.setPassedCases(0);
        run.setCreatedAt(LocalDateTime.now());
        run.setStartedAt(LocalDateTime.now());
        runMapper.insert(run);
        eventPublisher.publishEvent(new EvalRunRequested(run.getId(), cases,
                topK == null ? 5 : Math.max(1, Math.min(topK, 20))));
        return run;
    }

    @Async
    @EventListener
    public void onEvalRunRequested(EvalRunRequested event) {
        executeRun(event.runId(), event.cases(), event.topK());
    }

    void executeRun(Long runId, List<AiEvalCase> cases, int topK) {
        List<Long> latencies = new ArrayList<>();
        int completed = 0, passed = 0, recallHits = 0, validCitations = 0, citationCoverage = 0, refusals = 0;
        double reciprocalRankTotal = 0;
        long totalTokens = 0;
        try {
            for (AiEvalCase evalCase : cases) {
                AiEvalResult result = evaluateCase(runId, evalCase, topK);
                resultMapper.insert(result);
                completed++;
                latencies.add(result.getLatencyMs());
                totalTokens += result.getTotalTokens() == null ? 0 : result.getTotalTokens();
                if ("SUCCESS".equals(result.getStatus())) {
                    recallHits += result.getRecallHit();
                    reciprocalRankTotal += result.getReciprocalRank();
                    validCitations += result.getCitationValid();
                    citationCoverage += result.getCitationCoverage();
                    refusals += result.getRefusalCorrect();
                    if (result.getRecallHit() == 1 && result.getCitationValid() == 1 && result.getRefusalCorrect() == 1) passed++;
                }
                updateProgress(runId, completed, passed);
            }
            AiEvalRun run = runMapper.selectById(runId);
            run.setStatus("SUCCESS");
            run.setCompletedCases(completed);
            run.setPassedCases(passed);
            run.setRecallAtK(average(recallHits, cases.size()));
            run.setMrr(cases.isEmpty() ? 0 : reciprocalRankTotal / cases.size());
            run.setCitationValidity(average(validCitations, cases.size()));
            run.setCitationCoverage(average(citationCoverage, cases.size()));
            run.setRefusalRate(average(refusals, cases.size()));
            run.setP95LatencyMs(percentile(latencies, .95));
            run.setTotalTokens(totalTokens);
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
        } catch (Exception e) {
            log.error("[eval] run failed runId={}", runId, e);
            AiEvalRun run = runMapper.selectById(runId);
            run.setStatus("FAILED");
            run.setCompletedCases(completed);
            run.setPassedCases(passed);
            run.setErrorMsg(truncate(e.getMessage(), 1000));
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
        }
    }

    private AiEvalResult evaluateCase(Long runId, AiEvalCase evalCase, int topK) {
        AiEvalResult result = new AiEvalResult();
        result.setRunId(runId);
        result.setCaseId(evalCase.getId());
        result.setQuestion(evalCase.getQuestion());
        long started = System.currentTimeMillis();
        try {
            RagAnswerVo answer = ragGenerationService.answer(evalCase.getQuestion(), null, null);
            result.setAnswer(answer.getAnswer());
            result.setSourcesJson(objectMapper.writeValueAsString(answer.getSources()));
            result.setTotalTokens(answer.getTotalTokens());
            List<Long> expected = parseIds(evalCase.getExpectedDocIds());
            List<Long> found = answer.getSources().stream().limit(topK)
                    .map(source -> source.getDocId()).filter(Objects::nonNull).toList();
            List<Long> matched = expected.stream().filter(found::contains).toList();
            result.setMatchedDocIds(matched.stream().map(String::valueOf).collect(Collectors.joining(",")));
            result.setRecallHit(expected.isEmpty() || !matched.isEmpty() ? 1 : 0);
            result.setReciprocalRank(EvalMetrics.reciprocalRank(expected, found));
            result.setCitationValid(EvalMetrics.citationsValid(answer.getAnswer(), answer.getSources().size()) ? 1 : 0);
            result.setCitationCoverage(EvalMetrics.hasCitation(answer.getAnswer()) ? 1 : 0);
            boolean noAnswer = Integer.valueOf(1).equals(evalCase.getNoAnswer());
            result.setRefusalCorrect(noAnswer == EvalMetrics.isRefusal(answer.getAnswer()) ? 1 : 0);
            result.setStatus("SUCCESS");
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setErrorMsg(truncate(e.getMessage(), 1000));
            result.setRecallHit(0);
            result.setReciprocalRank(0.0);
            result.setCitationValid(0);
            result.setCitationCoverage(0);
            result.setRefusalCorrect(0);
            result.setTotalTokens(0);
        }
        result.setLatencyMs(System.currentTimeMillis() - started);
        result.setCreatedAt(LocalDateTime.now());
        return result;
    }

    private void updateProgress(Long runId, int completed, int passed) {
        AiEvalRun update = new AiEvalRun();
        update.setId(runId);
        update.setCompletedCases(completed);
        update.setPassedCases(passed);
        runMapper.updateById(update);
    }

    private List<Long> parseIds(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return Arrays.stream(value.split("[,， ]"))
                .filter(StringUtils::hasText).map(Long::valueOf).toList();
    }

    private double average(int value, int count) { return count == 0 ? 0 : value * 1.0 / count; }
    private long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0;
        Collections.sort(values);
        return values.get(Math.min(values.size() - 1, (int) Math.ceil(values.size() * percentile) - 1));
    }
    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    @Override
    public PageResult<AiEvalRun> runs(long page, long size) {
        return PageResult.build(runMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<AiEvalRun>().orderByDesc("id")));
    }
    @Override public AiEvalRun run(Long id) { return runMapper.selectById(id); }
    @Override
    public PageResult<AiEvalResult> results(Long runId, long page, long size) {
        return PageResult.build(resultMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<AiEvalResult>().eq("run_id", runId).orderByAsc("case_id")));
    }

    @Override
    public AiAnswerFeedback feedback(Long userId, FeedbackRequest request) {
        ChatMessage message = messageMapper.selectById(request.getMessageId());
        if (message == null || !"assistant".equals(message.getRole())) throw new IllegalArgumentException("回答不存在");
        ChatSession session = sessionMapper.selectById(message.getSessionId());
        if (session == null || !userId.equals(session.getUserId())) throw new IllegalStateException("无权评价该回答");
        AiAnswerFeedback row = feedback(userId, request.getMessageId());
        if (row == null) {
            row = new AiAnswerFeedback();
            row.setUserId(userId);
            row.setMessageId(request.getMessageId());
            row.setSessionId(message.getSessionId());
            row.setCreatedAt(LocalDateTime.now());
        }
        row.setVote(request.getVote());
        row.setReason(request.getReason());
        row.setUpdatedAt(LocalDateTime.now());
        if (row.getId() == null) feedbackMapper.insert(row); else feedbackMapper.updateById(row);
        return row;
    }

    @Override
    public AiAnswerFeedback feedback(Long userId, Long messageId) {
        return feedbackMapper.selectOne(new QueryWrapper<AiAnswerFeedback>()
                .eq("user_id", userId).eq("message_id", messageId));
    }

    @Override
    public Map<String, Long> feedbackStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("UP", 0L);
        stats.put("DOWN", 0L);
        for (Map<String, Object> row : feedbackMapper.countByVote()) {
            stats.put(String.valueOf(row.get("vote")), ((Number) row.get("count")).longValue());
        }
        return stats;
    }

    public record EvalRunRequested(Long runId, List<AiEvalCase> cases, int topK) {}
}
