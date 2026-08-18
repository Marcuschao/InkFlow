package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.model.dto.stat.PageViewRequest;
import com.blog.content.service.StatIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stat")
public class StatController {

    @Autowired
    private StatIngestService statIngestService;

    @PostMapping("/view")
    public Result<Void> view(@RequestBody PageViewRequest body) {
        if (body == null || !StringUtils.hasText(body.getVisitorId())) {
            return Result.success();
        }
        statIngestService.recordPageView(body);
        return Result.success();
    }
}
