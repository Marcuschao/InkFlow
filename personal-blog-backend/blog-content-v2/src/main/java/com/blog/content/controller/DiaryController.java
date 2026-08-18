package com.blog.content.controller;

import com.blog.content.model.dto.diary.DiarySaveRequest;
import com.blog.content.model.vo.diary.DiaryVo;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.service.DiaryService;
import com.blog.content.common.support.PageResult;
import com.blog.content.common.support.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<DiaryVo>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String tag) {
        Long uid = currentUserService.requireUserId();
        return Result.success(diaryService.pageMine(uid, page, size, month, tag));
    }

    @GetMapping("/{id:\\d+}")
    public Result<DiaryVo> get(@PathVariable Long id) {
        Long uid = currentUserService.requireUserId();
        return Result.success(diaryService.getMine(uid, id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody DiarySaveRequest body) {
        Long uid = currentUserService.requireUserId();
        return Result.success(diaryService.create(uid, body));
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DiarySaveRequest body) {
        Long uid = currentUserService.requireUserId();
        diaryService.update(uid, id, body);
        return Result.success();
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable Long id) {
        Long uid = currentUserService.requireUserId();
        diaryService.delete(uid, id);
        return Result.success();
    }
}
