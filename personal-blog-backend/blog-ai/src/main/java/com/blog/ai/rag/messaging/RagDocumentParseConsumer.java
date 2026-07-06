package com.blog.ai.rag.messaging;

import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.mapper.KnowledgeDocumentMapper;
import com.blog.ai.model.entity.KnowledgeDocument;
import com.blog.ai.rag.chunk.ChunkingService;
import com.blog.ai.rag.embed.EmbeddingService;
import com.blog.ai.rag.model.KnowledgeChunkDoc;
import com.blog.ai.rag.parse.DocumentParserService;
import com.blog.ai.rag.search.KnowledgeChunkIndexService;
import com.blog.ai.rag.service.KnowledgeDocumentService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class RagDocumentParseConsumer {
    
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentParserService documentParserService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final KnowledgeChunkIndexService knowledgeChunkIndexService;
    private final RagProperties ragProperties;

    @RabbitListener(queues = "${blog.rag.rabbit.queue:rag.doc.parse.queue}")
    public void onParseTask(Long docId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws IOException {
        try {
            process(docId);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("[rag] doc parse task failed id={}: {}", docId, e.getMessage(), e);
            if (docId != null) {
                try {
                    knowledgeDocumentService.updateStatus(docId, "FAILED", truncate(e.getMessage(), 1000), null);
                } catch (Exception ignored) {
                }
            }
            channel.basicAck(tag, false);
        }
    }

    private void process(Long docId) throws Exception {
        if (docId == null) {
            return;
        }
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(docId);
        if (doc == null) {
            log.warn("[rag] parse task doc not found id={}", docId);
            return;
        }
        if ("COMPLETED".equals(doc.getStatus())) {
            log.info("[rag] doc {} already completed, skip", docId);
            return;
        }
        knowledgeDocumentService.updateStatus(docId, "PROCESSING", null, null);
        try (InputStream stream = knowledgeDocumentService.openObject(doc)) {
            String text = documentParserService.parse(stream);
            List<KnowledgeChunkDoc> chunks = chunkingService.chunk(
                    doc.getId(), doc.getTitle(), text,
                    ragProperties.getChunk().getSize(),
                    ragProperties.getChunk().getOverlap());
            List<String> texts = new ArrayList<>();
            for (KnowledgeChunkDoc c : chunks) {
                texts.add(c.getText());
            }
            List<float[]> vectors = embeddingService.embedForIndex(texts);
            for (int i = 0; i < chunks.size() && i < vectors.size(); i++) {
                chunks.get(i).setEmbedding(EmbeddingService.toFloatList(vectors.get(i)));
            }
            if (chunks.isEmpty()) {
                throw new IllegalStateException("文档解析后无有效分块");
            }
            knowledgeChunkIndexService.indexChunks(chunks);
            knowledgeDocumentService.updateStatus(docId, "COMPLETED", null, (long) chunks.size());
            log.info("[rag] doc {} processed chunks={}", docId, chunks.size());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
