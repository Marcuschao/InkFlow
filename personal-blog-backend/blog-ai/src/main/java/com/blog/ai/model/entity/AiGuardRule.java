package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_guard_rule")
public class AiGuardRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleType;
    private String pattern;
    private String action;
    private Integer enabled;
}
