package com.blog.content.gamification.badge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.gamification.badge.model.entity.Badge;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BadgeMapper extends BaseMapper<Badge> {
}
