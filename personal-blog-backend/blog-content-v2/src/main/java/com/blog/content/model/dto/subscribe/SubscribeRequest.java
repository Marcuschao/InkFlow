package com.blog.content.model.dto.subscribe;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubscribeRequest {
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
}
