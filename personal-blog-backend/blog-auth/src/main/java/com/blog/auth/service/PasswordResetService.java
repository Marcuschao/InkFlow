package com.blog.auth.service;

public interface PasswordResetService {
    void requestReset(String email, String ip);

    boolean validateToken(String token);

    void resetPassword(String token, String newPassword, String ip);
}
