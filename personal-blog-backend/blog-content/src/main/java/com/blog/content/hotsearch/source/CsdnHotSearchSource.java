package com.blog.content.hotsearch.source;

import com.blog.content.hotsearch.model.HotItem;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(30)
public class CsdnHotSearchSource implements HotSearchSource {
    private static final Logger log = LoggerFactory.getLogger(CsdnHotSearchSource.class);
    private static final String API_URL = "https://api.02gk.com/api/csdnhot";

    private final RestTemplate restTemplate;

    public CsdnHotSearchSource(@Qualifier("hotSearchRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getId() {
        return "csdn";
    }

    @Override
    public String getName() {
        return "CSDN热搜";
    }

    @Override
    public List<HotItem> fetchHotList() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set(HttpHeaders.ACCEPT, "application/json");
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    API_URL, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            JsonNode root = response.getBody();
            if (root == null) {
                log.warn("[hot-search] csdn response body is null");
                return fallback();
            }
            int code = root.path("code").asInt(-1);
            JsonNode list = root.path("data");
            if (code != 200 || !list.isArray() || list.isEmpty()) {
                log.warn("[hot-search] csdn response invalid: code={}, hasData={}", code, list.isArray());
                return fallback();
            }
            LocalDateTime now = LocalDateTime.now();
            List<HotItem> items = new ArrayList<>();
            int rank = 1;
            for (JsonNode node : list) {
                String title = text(node, "title", "articleTitle", "nickName");
                if (!StringUtils.hasText(title)) {
                    continue;
                }
                String url = text(node, "url", "articleUrl", "link");
                if (!StringUtils.hasText(url)) {
                    url = "https://so.csdn.net/so/search?q=" + title + "&t=all";
                }
                String heat = text(node, "hot", "hotRankScore", "viewCount");
                items.add(HotItem.builder()
                        .rank(rank++)
                        .title(title.trim())
                        .url(url.trim())
                        .heat(StringUtils.hasText(heat) ? heat.trim() : null)
                        .updatedAt(now)
                        .build());
            }
            return items.isEmpty() ? fallback() : items;
        } catch (Exception ex) {
            log.warn("[hot-search] csdn fetch failed: {}", ex.toString());
            return fallback();
        }
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode v = node.get(field);
            if (v != null && !v.isNull()) {
                String t = v.asText();
                if (StringUtils.hasText(t)) {
                    return t;
                }
            }
        }
        return null;
    }

    private static final List<HotItem> FALLBACK_ITEMS = buildFallback();

    static List<HotItem> fallback() {
        return FALLBACK_ITEMS;
    }

    @Override
    public boolean isFallbackData(List<HotItem> items) {
        return items == FALLBACK_ITEMS;
    }

    private static List<HotItem> buildFallback() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                HotItem.builder().rank(1).title("Java技术实践").url("https://so.csdn.net/so/search?q=Java技术实践&t=all").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(2).title("SpringBoot教程").url("https://so.csdn.net/so/search?q=SpringBoot教程&t=all").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(3).title("前端框架对比").url("https://so.csdn.net/so/search?q=前端框架对比&t=all").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(4).title("数据库优化").url("https://so.csdn.net/so/search?q=数据库优化&t=all").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(5).title("云原生实践").url("https://so.csdn.net/so/search?q=云原生实践&t=all").heat("示例").updatedAt(now).build()
        );
    }
}
