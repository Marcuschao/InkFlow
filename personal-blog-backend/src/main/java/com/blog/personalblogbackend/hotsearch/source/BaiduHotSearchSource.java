package com.blog.personalblogbackend.hotsearch.source;

import com.blog.personalblogbackend.hotsearch.model.HotItem;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class BaiduHotSearchSource implements HotSearchSource {
    private static final Logger log = LoggerFactory.getLogger(BaiduHotSearchSource.class);
    private static final String API_URL = "https://top.baidu.com/api/board?platform=pc&tab=realtime";

    private final RestTemplate restTemplate;

    public BaiduHotSearchSource(@Qualifier("hotSearchRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getId() {
        return "baidu";
    }

    @Override
    public String getName() {
        return "百度热搜";
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
                return fallback();
            }
            JsonNode cards = root.path("data").path("cards");
            if (!cards.isArray() || cards.isEmpty()) {
                return fallback();
            }
            JsonNode content = cards.get(0).path("content");
            if (!content.isArray() || content.isEmpty()) {
                return fallback();
            }
            LocalDateTime now = LocalDateTime.now();
            List<HotItem> items = new ArrayList<>();
            int rank = 1;
            for (JsonNode node : content) {
                String title = text(node, "word", "query", "title");
                if (!StringUtils.hasText(title)) {
                    continue;
                }
                String url = text(node, "rawUrl", "url", "link");
                if (!StringUtils.hasText(url)) {
                    url = "https://www.baidu.com/s?wd=" + title;
                }
                String heat = text(node, "hotScore", "hotTag", "desc");
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
            log.warn("[hot-search] baidu fetch failed: {}", ex.toString());
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

    public boolean isFallbackData(List<HotItem> items) {
        return items == FALLBACK_ITEMS;
    }

    private static List<HotItem> buildFallback() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                HotItem.builder().rank(1).title("科技前沿动态").url("https://www.baidu.com/s?wd=科技前沿动态").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(2).title("人工智能应用").url("https://www.baidu.com/s?wd=人工智能应用").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(3).title("开源项目推荐").url("https://www.baidu.com/s?wd=开源项目推荐").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(4).title("编程语言趋势").url("https://www.baidu.com/s?wd=编程语言趋势").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(5).title("开发者工具").url("https://www.baidu.com/s?wd=开发者工具").heat("示例").updatedAt(now).build()
        );
    }
}
