package com.blog.personalblogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.personalblogbackend.model.entity.UserOauth;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserOauthMapper extends BaseMapper<UserOauth> {
}
