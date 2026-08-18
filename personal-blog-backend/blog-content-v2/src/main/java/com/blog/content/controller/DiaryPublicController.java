package com.blog.content.controller;

import com.blog.content.model.vo.diary.DiaryPublicDetailVo;
import com.blog.content.model.vo.diary.DiaryPublicListItemVo;
import com.blog.content.service.DiaryService;
import com.blog.content.common.support.PageResult;
import com.blog.content.common.support.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diary/public")
public class DiaryPublicController {

    @Autowired
    private DiaryService diaryService;

    @GetMapping
    public Result<PageResult<DiaryPublicListItemVo>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(diaryService.pagePublic(page, size));
    }

    @GetMapping("/{id:\\d+}")
    public Result<DiaryPublicDetailVo> get(@PathVariable Long id) {
        return Result.success(diaryService.getPublic(id));
    }
}
