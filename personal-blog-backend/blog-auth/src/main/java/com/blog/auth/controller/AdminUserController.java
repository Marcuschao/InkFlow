package com.blog.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.auth.common.support.PageResult;
import com.blog.auth.common.support.Result;
import com.blog.auth.model.vo.user.AdminUserVo;
import com.blog.auth.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<PageResult<AdminUserVo>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        IPage<AdminUserVo> p = userService.adminPage(page, size, keyword, role);
        return Result.success(PageResult.build(p.getRecords(), p.getTotal(), p.getSize(), p.getCurrent()));
    }
}
