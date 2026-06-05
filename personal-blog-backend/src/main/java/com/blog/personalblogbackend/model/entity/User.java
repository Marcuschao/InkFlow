package com.blog.personalblogbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String password;
    private String nickname;
    private String avatar;
    private String role;
    private Boolean passwordLoginEnabled;
    private String registerIp;
    private String registerRegion;
    private LocalDateTime createTime;
}
