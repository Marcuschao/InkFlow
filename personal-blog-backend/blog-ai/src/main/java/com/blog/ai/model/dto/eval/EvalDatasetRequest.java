package com.blog.ai.model.dto.eval;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class EvalDatasetRequest { @NotBlank private String name; private String description; private Integer enabled = 1; }
