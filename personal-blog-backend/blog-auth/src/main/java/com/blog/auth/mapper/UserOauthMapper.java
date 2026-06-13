package com.blog.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.auth.model.entity.UserOauth;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserOauthMapper extends BaseMapper<UserOauth> {
}
