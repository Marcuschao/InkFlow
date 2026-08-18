package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag_relationship")
public class TagRelationship {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("tag_id_1")
    private Long tagId1;
    @TableField("tag_id_2")
    private Long tagId2;
    private Double weight;
    private LocalDateTime updateTime;
}
