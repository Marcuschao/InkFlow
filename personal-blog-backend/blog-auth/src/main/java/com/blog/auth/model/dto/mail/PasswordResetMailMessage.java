package com.blog.auth.model.dto.mail;

import lombok.Data;

import java.io.Serializable;

@Data
public class PasswordResetMailMessage implements Serializable {
    private String type;
    private String email;
    private String username;
    private String resetLink;
}
