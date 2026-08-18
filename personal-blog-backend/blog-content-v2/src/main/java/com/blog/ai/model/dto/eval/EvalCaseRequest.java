package com.blog.ai.model.dto.eval;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class EvalCaseRequest { private Long datasetId; @NotBlank private String question; private String expectedAnswer; private String expectedDocIds; private String requiredKeywords; private String forbiddenClaims; private Integer noAnswer = 0; private String tags; private String remark; private Integer enabled = 1; }
