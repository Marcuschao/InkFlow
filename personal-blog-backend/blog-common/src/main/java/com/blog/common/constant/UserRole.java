package com.blog.common.constant;

public final class UserRole {

    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    private UserRole() {
    }

    public static String toSpringAuthority(String role) {
        if (ADMIN.equals(role)) {
            return "ROLE_ADMIN";
        }
        return "ROLE_USER";
    }
}
