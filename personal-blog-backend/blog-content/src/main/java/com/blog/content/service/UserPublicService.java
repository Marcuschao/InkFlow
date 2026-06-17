package com.blog.content.service;

import com.blog.content.model.vo.user.PublicUserVo;

public interface UserPublicService {

    PublicUserVo getPublicProfile(Long userId);
}
