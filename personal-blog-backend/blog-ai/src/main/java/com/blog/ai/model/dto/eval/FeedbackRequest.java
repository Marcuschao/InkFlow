package com.blog.ai.model.dto.eval;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
@Data public class FeedbackRequest { @NotNull private Long messageId; @NotNull @Pattern(regexp="UP|DOWN") private String vote; private String reason; }
