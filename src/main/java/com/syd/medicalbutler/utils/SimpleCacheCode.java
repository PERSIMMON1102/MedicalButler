package com.syd.medicalbutler.utils;


import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 简单的内存验证码缓存（生产请替换为 Redis / 专用缓存）
 */
public class SimpleCacheCode {

    // 存放 phone -> code
    private final ConcurrentHashMap<String, String> codeMap = new ConcurrentHashMap<>();
    // 存放 phone -> expiryEpochSeconds
    private final ConcurrentHashMap<String, Long> expireMap = new ConcurrentHashMap<>();

    // 单例执行清理线程
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    // 默认过期分钟
    private final long expireSeconds;

    public SimpleCacheCode(long expireSeconds) {
        this.expireSeconds = expireSeconds;
        // 每分钟清理一次
        cleaner.scheduleAtFixedRate(this::cleanup, 60, 60, TimeUnit.SECONDS);
    }

    public void put(String phone, String code) {
        codeMap.put(phone, code);
        expireMap.put(phone, Instant.now().getEpochSecond() + expireSeconds);
    }

    public String get(String phone) {
        Long exp = expireMap.get(phone);
        if (exp == null) return null;
        if (Instant.now().getEpochSecond() > exp) {
            // 已过期，删除并返回 null
            codeMap.remove(phone);
            expireMap.remove(phone);
            return null;
        }
        return codeMap.get(phone);
    }

    public void remove(String phone) {
        codeMap.remove(phone);
        expireMap.remove(phone);
    }

    private void cleanup() {
        long now = Instant.now().getEpochSecond();
        for (Map.Entry<String, Long> e : expireMap.entrySet()) {
            if (e.getValue() < now) {
                String phone = e.getKey();
                expireMap.remove(phone);
                codeMap.remove(phone);
            }
        }
    }

    public void shutdown() {
        cleaner.shutdownNow();
    }
}