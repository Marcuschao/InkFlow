package com.blog.content.controller;

import com.blog.common.dto.ChatProfileBroadcastRequest;
import com.blog.common.support.Result;
import com.blog.content.service.ChatProfileBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/chat")
@RequiredArgsConstructor
public class InternalChatController {

    private final ChatProfileBroadcastService chatProfileBroadcastService;

    @PostMapping("/profile-broadcast")
    public Result<Void> broadcastProfile(@RequestBody ChatProfileBroadcastRequest request) {
        if (request == null || request.getUserId() == null) {
            return Result.success();
        }
        chatProfileBroadcastService.broadcastProfileUpdate(
                request.getUserId(),
                request.getNickname(),
                request.getAvatar());
        return Result.success();
    }
}
