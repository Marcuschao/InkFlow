package com.blog.ai.config.security;
import com.blog.ai.gateway.context.GatewayUserContext;
import org.springframework.stereotype.Service;
@Service public class CurrentUserService {
 public Long requireUserId(){ Long id=GatewayUserContext.getUserId(); if(id==null)throw new IllegalStateException("请先登录"); return id; }
}
