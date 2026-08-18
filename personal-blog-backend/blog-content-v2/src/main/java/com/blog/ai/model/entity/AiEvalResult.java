package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_eval_result")
public class AiEvalResult {
    @TableId(type = IdType.AUTO) private Long id;
    private Long runId;
    private Long caseId;
    private String status;
    private String question;
    private String answer;
    private String sourcesJson;
    private String matchedDocIds;
    private Integer recallHit;
    private Double reciprocalRank;
    private Integer citationValid;
    private Integer citationCoverage;
    private Integer refusalCorrect;
    private Long latencyMs;
    private Integer totalTokens;
    private String errorMsg;
    private LocalDateTime createdAt;
}
