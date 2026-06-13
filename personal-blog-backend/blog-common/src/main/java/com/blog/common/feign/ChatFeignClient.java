package com.blog.common.feign;

import com.blog.common.dto.ChatProfileBroadcastRequest;
import com.blog.common.support.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "blog-content", contextId = "chatFeignClient")
public interface ChatFeignClient {

    @PostMapping("/internal/chat/profile-broadcast")
    Result<Void> broadcastProfile(@RequestBody ChatProfileBroadcastRequest request);
}
