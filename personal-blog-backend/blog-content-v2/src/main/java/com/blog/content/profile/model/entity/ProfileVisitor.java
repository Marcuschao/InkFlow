package com.blog.content.profile.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("profile_visitor")
public class ProfileVisitor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long profileOwnerId;
    private Long visitorUserId;
    private LocalDateTime visitTime;
}
