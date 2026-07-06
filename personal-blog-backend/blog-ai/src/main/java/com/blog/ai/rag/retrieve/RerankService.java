package com.blog.ai.rag.retrieve;

import com.blog.ai.config.properties.RagProperties;
import com.blog.ai.rag.model.RetrievedChunk;
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
public class RerankService {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RerankService(RagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (!properties.getRerank().isEnabled() || !StringUtils.hasText(properties.getRerank().getApiKey())) {
            return truncate(candidates, topN);
        }
        try {
            RagProperties.Rerank cfg = properties.getRerank();
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", cfg.getModel());
            body.put("query", query);
            ArrayNode docs = body.putArray("documents");
            for (RetrievedChunk c : candidates) {
                docs.add(c.getText());
            }
            body.put("top_n", Math.min(topN, candidates.size()));
            String url = OpenAiCompatibleUrls.resolve(cfg.getBaseUrl(), "/rerank");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + cfg.getApiKey())
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("[rag] rerank api failed status={} body={}", resp.statusCode(), resp.body());
                return truncate(candidates, topN);
            }
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                JsonNode output = root.get("output");
                if (output != null) {
                    results = output.get("results");
                }
            }
            List<RetrievedChunk> out = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode item : results) {
                    int idx = item.has("index") ? item.get("index").asInt() : item.get("document_index").asInt(-1);
                    if (idx < 0 || idx >= candidates.size()) {
                        continue;
                    }
                    double score = item.has("relevance_score") ? item.get("relevance_score").asDouble()
                            : item.path("score").asDouble(0);
                    RetrievedChunk rc = candidates.get(idx);
                    rc.setRerankScore(score);
                    out.add(rc);
                }
            }
            return out.isEmpty() ? truncate(candidates, topN) : out;
        } catch (Exception e) {
            log.warn("[rag] rerank call failed: {}", e.getMessage());
            return truncate(candidates, topN);
        }
    }

    private List<RetrievedChunk> truncate(List<RetrievedChunk> list, int topN) {
        return list.size() <= topN ? list : new ArrayList<>(list.subList(0, topN));
    }
}
