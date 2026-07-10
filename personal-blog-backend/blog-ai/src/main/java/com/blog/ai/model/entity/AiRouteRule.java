package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_route_rule")
public class AiRouteRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskPattern;
    private String primaryModel;
    private String fallbackChain;
    private Integer enabled;
    private Integer sortOrder;
}
