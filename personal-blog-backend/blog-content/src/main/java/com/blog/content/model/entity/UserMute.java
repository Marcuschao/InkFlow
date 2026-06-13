package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_mute")
public class UserMute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDateTime muteUntil;
    private LocalDateTime createTime;
}
