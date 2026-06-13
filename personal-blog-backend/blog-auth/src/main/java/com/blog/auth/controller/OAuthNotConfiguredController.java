package com.blog.auth.controller;

import com.blog.auth.common.support.Result;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnMissingBean(ClientRegistrationRepository.class)
public class OAuthNotConfiguredController {

    @GetMapping("/oauth2/authorization/{provider}")
    public Result<Void> authorizationDisabled(@PathVariable String provider) {
        return Result.fail(503,
                "GitHub OAuth 未就绪：请在 application-local.yml 配置 blog.oauth.client-id 与 client-secret（或设置环境变量 GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET）后重启");
    }
}
