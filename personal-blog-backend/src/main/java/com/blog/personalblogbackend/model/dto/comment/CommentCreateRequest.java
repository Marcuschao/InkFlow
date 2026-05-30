package com.blog.personalblogbackend.model.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateRequest {
    @NotNull
    private Long articleId;
    private Long parentId;
    @NotBlank
    @Size(max = 4000)
    private String content;
    @Size(max = 64)
    private String clientRequestId;
}
