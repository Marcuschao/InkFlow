package com.blog.ai.model.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiModelCreateRequest {

    @NotBlank(message = "providerId不能为空")
    private String providerId;

    private String name;

    @NotBlank(message = "apiKey不能为空")
    private String apiKey;

    @NotBlank(message = "baseUrl不能为空")
    private String baseUrl;

    @NotBlank(message = "models不能为空，多个用逗号分隔")
    private String models;

    private Integer priority;

    private Integer maxConcurrency;

    private Long timeoutMs;

    private Boolean enabled;
}
