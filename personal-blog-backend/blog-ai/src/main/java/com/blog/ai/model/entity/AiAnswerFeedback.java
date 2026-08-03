package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_answer_feedback")
public class AiAnswerFeedback {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long messageId;
    private Long sessionId;
    private String vote;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
