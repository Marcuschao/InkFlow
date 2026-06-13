package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.model.entity.FriendLink;
import com.blog.content.service.FriendLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/links")
public class AdminFriendLinkController {

    @Autowired
    private FriendLinkService friendLinkService;

    @GetMapping
    public Result<List<FriendLink>> all() {
        return Result.success(friendLinkService.lambdaQuery().orderByAsc(FriendLink::getSortOrder).orderByAsc(FriendLink::getId).list());
    }

    @PostMapping
    public Result<Void> create(@RequestBody FriendLink body) {
        if (body.getStatus() == null) {
            body.setStatus(1);
        }
        if (body.getSortOrder() == null) {
            body.setSortOrder(0);
        }
        body.setCreateTime(LocalDateTime.now());
        friendLinkService.save(body);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody FriendLink body) {
        body.setId(id);
        friendLinkService.updateById(body);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        friendLinkService.removeById(id);
        return Result.success(null);
    }
}
