package com.blog.personalblogbackend.model.enums;

public final class ArticleStatus {
    public static final int DRAFT = 0;
    public static final int PUBLISHED = 1;
    public static final int PENDING = 2;
    public static final int REJECTED = 3;

    private ArticleStatus() {
    }

    public static boolean isPublished(Integer status) {
        return status != null && status == PUBLISHED;
    }

    public static boolean isPublicVisible(Integer status) {
        return isPublished(status);
    }
}
