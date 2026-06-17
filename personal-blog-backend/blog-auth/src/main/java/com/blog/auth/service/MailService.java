package com.blog.auth.service;

public interface MailService {
    void sendPasswordResetLink(String email, String username, String resetLink);

    void sendPasswordResetSuccess(String email, String username);
}
