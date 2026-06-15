package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag_relationship")
public class TagRelationship {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tagId1;
    private Long tagId2;
    private Double weight;
    private LocalDateTime updateTime;
}
