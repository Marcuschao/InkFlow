package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_eval_run")
public class AiEvalRun {
    @TableId(type = IdType.AUTO) private Long id;
    private Long datasetId;
    private String status;
    private Integer totalCases;
    private Integer completedCases;
    private Integer passedCases;
    private Double recallAtK;
    private Double mrr;
    private Double citationValidity;
    private Double citationCoverage;
    private Double refusalRate;
    private Long p95LatencyMs;
    private Long totalTokens;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
