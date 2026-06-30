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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

//@Component
//@Order(20)
public class WeiboHotSearchSource implements HotSearchSource {
    private static final Logger log = LoggerFactory.getLogger(WeiboHotSearchSource.class);
    private static final String API_BASE = "https://m.weibo.cn/api/container/getIndex";
    private static final String CONTAINER_ID = "106003type=25&t=3&disable_hot=1&filter_type=realtimehot";

    private final RestTemplate restTemplate;

    public WeiboHotSearchSource(@Qualifier("hotSearchRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getId() {
        return "weibo";
    }

    @Override
    public String getName() {
        return "微博热搜";
    }

    @Override
    public List<HotItem> fetchHotList() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1");
            headers.set(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
            headers.set(HttpHeaders.REFERER, "https://m.weibo.cn/");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            URI uri = UriComponentsBuilder.fromUriString(API_BASE)
                    .queryParam("containerid", CONTAINER_ID)
                    .queryParam("extparam", "filter_type=realtimehot&mi_cid=100103&pos=0_0&c_type=30&display_time=" + System.currentTimeMillis() / 1000)
                    .queryParam("luicode", "10000011")
                    .queryParam("lfid", "231583")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, JsonNode.class);

            JsonNode root = response.getBody();
            if (root == null || root.path("ok").asInt(-1) != 1) {
                log.warn("[hot-search] weibo response invalid: ok != 1");
                return fallback();
            }

            JsonNode cards = root.path("data").path("cards");
            if (!cards.isArray() || cards.isEmpty()) {
                log.warn("[hot-search] weibo cards array is empty");
                return fallback();
            }

            JsonNode group = null;
            for (JsonNode card : cards) {
                JsonNode g = card.path("card_group");
                if (g.isArray() && !g.isEmpty()) {
                    group = g;
                    break;
                }
            }
            if (group == null) {
                log.warn("[hot-search] weibo card_group not found");
                return fallback();
            }

            LocalDateTime now = LocalDateTime.now();
            List<HotItem> items = new ArrayList<>();
            int rank = 1;
            for (JsonNode node : group) {
                String title = text(node, "desc", "word", "note");
                if (!StringUtils.hasText(title)) {
                    continue;
                }
                String url = text(node, "scheme", "url", "link");
                if (!StringUtils.hasText(url)) {
                    url = "https://s.weibo.com/weibo?q=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
                }
                String heat = text(node, "desc_extr", "num", "hot");
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
            log.warn("[hot-search] weibo fetch failed: {}", ex.toString());
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
                HotItem.builder().rank(1).title("今日要闻").url("https://s.weibo.com/weibo?q=今日要闻").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(2).title("社会热点").url("https://s.weibo.com/weibo?q=社会热点").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(3).title("娱乐动态").url("https://s.weibo.com/weibo?q=娱乐动态").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(4).title("体育赛事").url("https://s.weibo.com/weibo?q=体育赛事").heat("示例").updatedAt(now).build(),
                HotItem.builder().rank(5).title("科技资讯").url("https://s.weibo.com/weibo?q=科技资讯").heat("示例").updatedAt(now).build()
        );
    }
}
