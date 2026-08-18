package com.blog.content.gamification.badge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.gamification.badge.model.entity.UserBadge;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBadgeMapper extends BaseMapper<UserBadge> {
}
