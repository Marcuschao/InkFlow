package com.blog.personalblogbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipientUserId;
    private Long actorUserId;
    private String type;
    private Long targetId;
    private String targetType;
    private String content;
    private String eventId;
    private Integer isRead;
    private LocalDateTime createTime;
}
