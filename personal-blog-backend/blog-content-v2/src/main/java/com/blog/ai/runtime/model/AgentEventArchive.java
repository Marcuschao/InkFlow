package com.blog.ai.runtime.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_agent_event_archive")
public class AgentEventArchive {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String runId;
    private Long sequence;
    private String eventType;
    private String payloadJson;
    private LocalDateTime createdAt;
}
