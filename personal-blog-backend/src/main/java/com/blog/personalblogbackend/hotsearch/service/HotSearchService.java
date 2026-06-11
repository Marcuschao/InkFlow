package com.blog.personalblogbackend.hotsearch.service;

import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.config.hotsearch.HotSearchProperties;
import com.blog.personalblogbackend.hotsearch.cache.HotSearchCacheService;
import com.blog.personalblogbackend.hotsearch.model.HotItem;
import com.blog.personalblogbackend.hotsearch.model.HotSearchListVo;
import com.blog.personalblogbackend.hotsearch.model.HotSearchSourceVo;
import com.blog.personalblogbackend.hotsearch.source.BaiduHotSearchSource;
import com.blog.personalblogbackend.hotsearch.source.HotSearchSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class HotSearchService {
    private final Map<String, HotSearchSource> sourceMap;
    private final HotSearchCacheService cacheService;
    private final HotSearchProperties properties;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public HotSearchService(List<HotSearchSource> sources,
                            HotSearchCacheService cacheService,
                            HotSearchProperties properties) {
        Map<String, HotSearchSource> map = new LinkedHashMap<>();
        for (HotSearchSource source : sources) {
            map.put(source.getId(), source);
        }
        this.sourceMap = map;
        this.cacheService = cacheService;
        this.properties = properties;
    }

    public List<HotSearchSourceVo> listSources() {
        return sourceMap.values().stream()
                .map(s -> new HotSearchSourceVo(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    public HotSearchListVo getHotListBySource(String sourceId, Integer limit) {
        HotSearchSource source = requireSource(sourceId);
        HotSearchListVo cached = cacheService.get(source.getId());
        if (cached != null) {
            return trim(cached, limit);
        }
        return trim(loadAndCache(source), limit);
    }

    public List<HotSearchListVo> getAllHotLists(Integer limit) {
        List<CompletableFuture<HotSearchListVo>> futures = sourceMap.values().stream()
                .map(source -> CompletableFuture.supplyAsync(() -> getHotListBySource(source.getId(), limit), executor))
                .toList();
        List<HotSearchListVo> result = new ArrayList<>();
        for (CompletableFuture<HotSearchListVo> future : futures) {
            try {
                result.add(future.join());
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private HotSearchSource requireSource(String sourceId) {
        if (!StringUtils.hasText(sourceId)) {
            throw new ServiceException(400, "热搜栏目不能为空");
        }
        HotSearchSource source = sourceMap.get(sourceId.trim().toLowerCase());
        if (source == null) {
            throw new ServiceException(404, "热搜栏目不存在");
        }
        return source;
    }

    private HotSearchListVo loadAndCache(HotSearchSource source) {
        List<HotItem> items = source.fetchHotList();
        boolean fallback = source instanceof BaiduHotSearchSource baidu && baidu.isFallbackData(items);
        HotSearchListVo vo = HotSearchListVo.builder()
                .source(source.getId())
                .sourceName(source.getName())
                .items(items)
                .fetchedAt(LocalDateTime.now())
                .fallback(fallback)
                .build();
        cacheService.put(source.getId(), vo);
        return vo;
    }

    private HotSearchListVo trim(HotSearchListVo vo, Integer limit) {
        if (limit == null || limit <= 0 || vo.getItems() == null || vo.getItems().size() <= limit) {
            return vo;
        }
        return HotSearchListVo.builder()
                .source(vo.getSource())
                .sourceName(vo.getSourceName())
                .items(vo.getItems().subList(0, limit))
                .fetchedAt(vo.getFetchedAt())
                .fallback(vo.isFallback())
                .build();
    }
}
