package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.model.entity.FriendLink;
import com.blog.content.service.FriendLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/links")
public class FriendLinkPublicController {

    @Autowired
    private FriendLinkService friendLinkService;

    @GetMapping
    public Result<List<FriendLink>> list() {
        return Result.success(friendLinkService.listEnabledPublic());
    }
}
