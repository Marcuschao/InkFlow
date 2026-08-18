package com.blog.ai.runtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.ai.runtime.model.AgentToolCall;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentToolCallMapper extends BaseMapper<AgentToolCall> {}
