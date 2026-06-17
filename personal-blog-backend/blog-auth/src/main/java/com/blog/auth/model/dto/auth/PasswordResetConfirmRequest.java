package com.blog.auth.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetConfirmRequest {
    @NotBlank(message = "Token不能为空")
    private String token;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
