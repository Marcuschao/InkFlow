package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_failed_queue")
public class ChatFailedQueue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long userId;
    private String content;
    private Integer retryCount;
    private String status;
    private LocalDateTime createTime;
}
