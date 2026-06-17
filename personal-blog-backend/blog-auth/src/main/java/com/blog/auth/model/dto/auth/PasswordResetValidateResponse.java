package com.blog.auth.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordResetValidateResponse {
    private boolean valid;
}
