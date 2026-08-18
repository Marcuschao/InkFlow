package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.knowledge.service.KnowledgeGraphService;
import com.blog.content.knowledge.service.KnowledgeSubscriptionService;
import com.blog.content.model.dto.knowledge.UserLandscapeRequest;
import com.blog.content.model.vo.knowledge.HotTagVo;
import com.blog.content.model.vo.knowledge.KnowledgeGraphVo;
import com.blog.content.model.vo.knowledge.TagNodeDetailVo;
import com.blog.content.model.vo.knowledge.UserLandscapeVo;
import com.blog.content.config.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    @Autowired
    private KnowledgeSubscriptionService subscriptionService;

    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/graph")
    public Result<KnowledgeGraphVo> graph() {
        return Result.success(knowledgeGraphService.getGraph());
    }

    @GetMapping("/node")
    public Result<TagNodeDetailVo> node(@RequestParam Long tagId,
                                        @RequestParam(required = false, defaultValue = "10") Integer limit) {
        Long viewerId = currentUserService.optionalUserId();
        return Result.success(knowledgeGraphService.getNodeDetail(tagId, limit, viewerId));
    }

    @GetMapping("/subgraph")
    public Result<KnowledgeGraphVo> subgraph(@RequestParam Long articleId) {
        return Result.success(knowledgeGraphService.getSubgraph(articleId));
    }

    @GetMapping("/hot-tags")
    public Result<List<HotTagVo>> hotTags(@RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(knowledgeGraphService.getHotTags(limit));
    }

    @GetMapping("/user-landscape")
    public Result<UserLandscapeVo> userLandscape(@RequestParam Long userId) {
        return Result.success(knowledgeGraphService.getUserLandscape(userId, null));
    }

    @PostMapping("/user-landscape")
    public Result<UserLandscapeVo> userLandscapePost(@RequestParam Long userId,
                                                     @RequestBody(required = false) UserLandscapeRequest request) {
        return Result.success(knowledgeGraphService.getUserLandscape(userId, request));
    }

    @PostMapping("/subscribe")
    public Result<Void> subscribe(@RequestParam Long tagId) {
        Long userId = currentUserService.requireUserId();
        subscriptionService.subscribe(userId, tagId);
        return Result.success(null);
    }

    @DeleteMapping("/subscribe")
    public Result<Void> unsubscribe(@RequestParam Long tagId) {
        Long userId = currentUserService.requireUserId();
        subscriptionService.unsubscribe(userId, tagId);
        return Result.success(null);
    }
}
