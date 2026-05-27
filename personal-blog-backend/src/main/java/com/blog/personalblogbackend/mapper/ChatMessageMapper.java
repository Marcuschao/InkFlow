package com.blog.personalblogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.personalblogbackend.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
