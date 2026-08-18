package com.blog.content.gamification.sign.controller;

import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.gamification.sign.model.vo.SignCalendarVo;
import com.blog.content.gamification.sign.model.vo.SignResultVo;
import com.blog.content.gamification.sign.model.vo.SignStatusVo;
import com.blog.content.gamification.sign.service.SignService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sign")
public class SignController {

    private final SignService signService;
    private final CurrentUserService currentUserService;

    public SignController(SignService signService, CurrentUserService currentUserService) {
        this.signService = signService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/sign")
    public Result<SignResultVo> sign() {
        return Result.success(signService.sign(currentUserService.requireUserId()));
    }

    @GetMapping("/status")
    public Result<SignStatusVo> status() {
        return Result.success(signService.status(currentUserService.requireUserId()));
    }

    @GetMapping("/calendar")
    public Result<SignCalendarVo> calendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        return Result.success(signService.calendar(currentUserService.requireUserId(), y, m));
    }
}
