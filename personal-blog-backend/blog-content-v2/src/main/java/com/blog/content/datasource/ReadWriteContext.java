package com.blog.content.datasource;

public final class ReadWriteContext {
    private static final ThreadLocal<Boolean> READ = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ReadWriteContext() {
    }

    public static void markReadOnly() {
        READ.set(Boolean.TRUE);
    }

    public static void clear() {
        READ.remove();
    }

    public static boolean isReadOnly() {
        return Boolean.TRUE.equals(READ.get());
    }
}
