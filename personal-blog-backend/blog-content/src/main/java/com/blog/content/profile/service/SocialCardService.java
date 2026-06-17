package com.blog.content.profile.service;

import com.blog.content.profile.model.vo.SocialCardVo;

public interface SocialCardService {

    SocialCardVo getSocialCard(Long userId, Long viewerId);
}
