package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("about")
public class About {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content; // Markdown内容
    private LocalDateTime updateTime;
}
