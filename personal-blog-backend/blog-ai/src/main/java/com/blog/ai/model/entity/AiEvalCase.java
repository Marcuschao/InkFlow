package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_eval_case")
public class AiEvalCase {
    @TableId(type = IdType.AUTO) private Long id;
    private Long datasetId;
    private String question;
    private String expectedAnswer;
    private String expectedDocIds;
    private String requiredKeywords;
    private String forbiddenClaims;
    private Integer noAnswer;
    private String tags;
    private String remark;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
