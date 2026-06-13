package com.blog.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.content.model.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}
