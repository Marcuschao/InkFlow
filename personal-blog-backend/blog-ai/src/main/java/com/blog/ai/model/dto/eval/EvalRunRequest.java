package com.blog.ai.model.dto.eval;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class EvalRunRequest { @NotNull private Long datasetId; private Integer topK = 5; }
