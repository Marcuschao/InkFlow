package com.blog.ai.rag.embed;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.rag.util.OpenAiCompatibleUrls;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class EmbeddingService {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmbeddingService(RagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public float[] embedOne(String text) {
        List<float[]> list = embed(List.of(text), false);
        return list.isEmpty() ? new float[0] : list.get(0);
    }

    public List<float[]> embedForIndex(List<String> texts) {
        return embed(texts, true);
    }

    private List<float[]> embed(List<String> texts, boolean strict) {
        List<float[]> out = new ArrayList<>();
        if (texts == null || texts.isEmpty()) {
            return out;
        }
        RagProperties.Embedding cfg = properties.getEmbedding();
        if (!StringUtils.hasText(cfg.getApiKey())) {
            if (strict) {
                throw new ServiceException(500, "未配置 embedding API Key");
            }
            log.warn("[rag] embedding api key not configured");
            return dummy(texts);
        }
        int batchSize = Math.max(1, cfg.getMaxBatchSize());
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            List<float[]> batchResult = callRemote(cfg, batch, strict);
            if (strict) {
                validateBatch(batch, batchResult);
            }
            out.addAll(batchResult);
        }
        return out;
    }

    private List<float[]> callRemote(RagProperties.Embedding cfg, List<String> batch, boolean strict) {
        try {
            int dims = properties.getEs().getVectorDims();
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", cfg.getModel());
            body.put("dimensions", dims);
            ArrayNode input = body.putArray("input");
            for (String t : batch) {
                input.add(t);
            }
            String url = OpenAiCompatibleUrls.resolve(cfg.getBaseUrl(), "/embeddings");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + cfg.getApiKey())
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("[rag] embedding api failed url={} status={} body={}", url, resp.statusCode(), resp.body());
                if (strict) {
                    throw new ServiceException(500, "Embedding API 失败: HTTP " + resp.statusCode());
                }
                return dummy(batch);
            }
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode data = root.get("data");
            List<float[]> result = new ArrayList<>();
            if (data != null && data.isArray()) {
                for (JsonNode item : data) {
                    JsonNode emb = item.get("embedding");
                    if (emb != null && emb.isArray()) {
                        float[] vec = new float[emb.size()];
                        for (int j = 0; j < emb.size(); j++) {
                            vec[j] = (float) emb.get(j).asDouble();
                        }
                        result.add(vec);
                    }
                }
            }
            if (result.isEmpty()) {
                if (strict) {
                    throw new ServiceException(500, "Embedding API 返回空向量");
                }
                return dummy(batch);
            }
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[rag] embedding call failed: {}", e.getMessage());
            if (strict) {
                throw new ServiceException(500, "Embedding 调用失败: " + e.getMessage());
            }
            return dummy(batch);
        }
    }

    private void validateBatch(List<String> batch, List<float[]> vectors) {
        int expected = properties.getEs().getVectorDims();
        if (vectors.size() != batch.size()) {
            throw new ServiceException(500, "Embedding 数量与文本不一致");
        }
        for (float[] vec : vectors) {
            if (vec == null || vec.length != expected) {
                throw new ServiceException(500,
                        "向量维度 " + (vec == null ? 0 : vec.length) + " 与配置 vector-dims=" + expected + " 不一致");
            }
        }
    }

    private List<float[]> dummy(List<String> texts) {
        int dims = properties.getEs().getVectorDims();
        List<float[]> out = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            out.add(new float[dims]);
        }
        return out;
    }

    public static List<Float> toFloatList(float[] vec) {
        List<Float> list = new ArrayList<>(vec.length);
        for (float v : vec) {
            list.add(v);
        }
        return list;
    }
}
