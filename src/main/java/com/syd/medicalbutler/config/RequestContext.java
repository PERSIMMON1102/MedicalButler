package com.syd.medicalbutler.config;

/**
 * 简单的线程级上下文，用于在请求处理流程中临时保存当前用户信息。
 * 使用 ThreadLocal 保证每个请求线程的数据隔离，并在请求结束时清理，避免内存泄漏。
 */
public final class RequestContext {
    private RequestContext() {}

    private static final ThreadLocal<Object> CURRENT_USER = new ThreadLocal<>();

    public static void setCurrentUser(Object user) {
        CURRENT_USER.set(user);
    }

    public static Object getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}