package com.blog.personalblogbackend.hotsearch.controller;

import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.config.hotsearch.HotSearchProperties;
import com.blog.personalblogbackend.hotsearch.model.HotSearchListVo;
import com.blog.personalblogbackend.hotsearch.model.HotSearchSourceVo;
import com.blog.personalblogbackend.hotsearch.service.HotSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hot-search")
public class HotSearchController {

    private final HotSearchService hotSearchService;
    private final HotSearchProperties properties;

    public HotSearchController(HotSearchService hotSearchService, HotSearchProperties properties) {
        this.hotSearchService = hotSearchService;
        this.properties = properties;
    }

    @GetMapping("/sources")
    public Result<List<HotSearchSourceVo>> sources() {
        return Result.success(hotSearchService.listSources());
    }

    @GetMapping("/list")
    public Result<HotSearchListVo> list(@RequestParam String source,
                                        @RequestParam(required = false) Integer limit) {
        return Result.success(hotSearchService.getHotListBySource(source, limit));
    }

    @GetMapping("/all")
    public Result<List<HotSearchListVo>> all(@RequestParam(required = false) Integer limit) {
        int size = limit != null && limit > 0 ? limit : properties.getHomePreviewSize();
        return Result.success(hotSearchService.getAllHotLists(size));
    }
}
