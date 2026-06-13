package com.blog.common.feign;

import com.blog.common.dto.UserBatchRequest;
import com.blog.common.dto.UserInternalDto;
import com.blog.common.dto.UserProfileDto;
import com.blog.common.dto.UserPublicDto;
import com.blog.common.support.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "blog-auth", contextId = "userFeignClient")
public interface UserFeignClient {

    @GetMapping("/internal/users/{id}")
    Result<UserInternalDto> getUser(@PathVariable("id") Long id);

    @PostMapping("/internal/users/batch")
    Result<Map<Long, UserProfileDto>> batchProfiles(@RequestBody UserBatchRequest request);

    @GetMapping("/internal/users/{id}/public")
    Result<UserPublicDto> getPublicProfile(@PathVariable("id") Long id);
}
