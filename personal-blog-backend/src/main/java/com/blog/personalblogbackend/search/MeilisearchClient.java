package com.blog.personalblogbackend.search;

import com.blog.personalblogbackend.config.properties.SearchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Lazy
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class MeilisearchClient {

    private final RestTemplate restTemplate;
    private final SearchProperties properties;
    private final ObjectMapper objectMapper;

    public MeilisearchClient(RestTemplate meilisearchRestTemplate,
                             SearchProperties properties,
                             ObjectMapper objectMapper) {
        this.restTemplate = meilisearchRestTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        try {
            restTemplate.exchange(baseUrl() + "/health", HttpMethod.GET, jsonEntity(null), String.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void ensureIndex() {
        ensureIndex(properties.getIndex());
    }

    public void ensureIndex(String indexUid) {
        try {
            restTemplate.exchange(baseUrl() + "/indexes/" + indexUid, HttpMethod.GET, jsonEntity(null), String.class);
        } catch (Exception ex) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("uid", indexUid);
            body.put("primaryKey", "id");
            restTemplate.exchange(baseUrl() + "/indexes", HttpMethod.POST, jsonEntity(body), String.class);
        }
    }

    public void updateSettings() {
        updateSettings(properties.getIndex());
    }

    public void updateSettings(String indexUid) {
        ObjectNode settings = objectMapper.createObjectNode();
        settings.set("searchableAttributes", arrayOf("title", "summary", "content"));
        settings.set("filterableAttributes", arrayOf("status", "categoryId", "authorId"));
        settings.set("sortableAttributes", arrayOf("createTime", "updateTime", "viewCount"));
        String url = baseUrl() + "/indexes/" + indexUid + "/settings";
        restTemplate.exchange(url, HttpMethod.PATCH, jsonEntity(settings), String.class);
    }

    public void addOrReplaceDocuments(List<ArticleSearchDocument> documents) {
        addOrReplaceDocuments(properties.getIndex(), documents);
    }

    public void addOrReplaceDocuments(String indexUid, List<ArticleSearchDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        String url = baseUrl() + "/indexes/" + indexUid + "/documents";
        restTemplate.exchange(url, HttpMethod.POST, jsonEntity(documents), String.class);
    }

    public void deleteDocument(Long id) {
        if (id == null) {
            return;
        }
        String url = baseUrl() + "/indexes/" + properties.getIndex() + "/documents/" + id;
        restTemplate.exchange(url, HttpMethod.DELETE, jsonEntity(null), String.class);
    }

    public long getNumberOfDocuments() {
        return getNumberOfDocuments(properties.getIndex());
    }

    public long getNumberOfDocuments(String indexUid) {
        try {
            String url = baseUrl() + "/indexes/" + indexUid + "/stats";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, jsonEntity(null), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("numberOfDocuments").asLong(0);
        } catch (Exception ex) {
            return 0;
        }
    }

    public Set<Long> fetchExistingIds(String indexUid, List<Long> ids) {
        Set<Long> found = new HashSet<>();
        if (ids == null || ids.isEmpty()) {
            return found;
        }
        try {
            ArrayNode idArr = objectMapper.createArrayNode();
            for (Long id : ids) {
                idArr.add(String.valueOf(id));
            }
            String url = baseUrl() + "/indexes/" + indexUid + "/documents/fetch";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, jsonEntity(idArr), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.isArray()) {
                for (JsonNode doc : root) {
                    if (!doc.isNull() && doc.has("id")) {
                        found.add(doc.get("id").asLong());
                    }
                }
            } else if (root.isObject()) {
                JsonNode results = root.path("results");
                if (results.isArray()) {
                    for (JsonNode doc : results) {
                        if (doc != null && !doc.isNull() && doc.has("id")) {
                            found.add(doc.get("id").asLong());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            return found;
        }
        return found;
    }

    public void swapIndexes(String uidA, String uidB) {
        ArrayNode body = objectMapper.createArrayNode();
        body.add(uidA);
        body.add(uidB);
        restTemplate.exchange(baseUrl() + "/swap-indexes", HttpMethod.POST, jsonEntity(body), String.class);
    }

    public void deleteIndex(String indexUid) {
        try {
            restTemplate.exchange(baseUrl() + "/indexes/" + indexUid, HttpMethod.DELETE, jsonEntity(null), String.class);
        } catch (Exception ex) {
            // index may already be gone
        }
    }

    public JsonNode search(String query, int limit, int offset) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("q", query != null ? query : "");
        body.put("limit", limit);
        body.put("offset", offset);
        body.put("filter", "status = 1");
        body.set("attributesToHighlight", arrayOf("title", "summary"));
        body.put("highlightPreTag", "<mark>");
        body.put("highlightPostTag", "</mark>");
        String url = baseUrl() + "/indexes/" + properties.getIndex() + "/search";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, jsonEntity(body), String.class);
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid Meilisearch response", ex);
        }
    }

    private String baseUrl() {
        String host = properties.getHost();
        return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }

    private HttpEntity<String> jsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.setBearerAuth(properties.getApiKey());
        }
        String json = body == null ? "" : toJson(body);
        return new HttpEntity<>(json.isEmpty() && body == null ? null : json, headers);
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ArrayNode arrayOf(String... values) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (String v : values) {
            arr.add(v);
        }
        return arr;
    }
}
