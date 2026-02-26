package com.sism.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 内存实现的频率限制器
 * 
 * 使用滑动窗口算法实现频率限制:
 * - 记录每个请求的时间戳
 * - 在检查时移除过期的时间戳
 * - 统计当前窗口内的请求数量
 * 
 * 适用于单机部署场景，无需额外配置。
 * 
 * 注意: 此类不再使用 @Service 注解，由 ServiceAutoConfiguration 管理
 * 
 * **Property P9**: 在时间窗口内，请求次数超过限制时返回 429
 * 
 * **Validates: Requirements 2.3.2, 2.3.3, 2.3.4, 2.3.5**
 */
@Slf4j
public class InMemoryRateLimiter implements RateLimiter {

    /**
     * 存储每个 Key 的请求时间戳队列
     * Key: 限制标识符 (如 "login:192.168.1.1" 或 "api:user123")
     * Value: 请求时间戳队列 (毫秒)
     */
    private final Map<String, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    /**
     * 存储每个 Key 的窗口开始时间（用于计算重置时间）
     */
    private final Map<String, Long> windowStartTimes = new ConcurrentHashMap<>();

    /**
     * 检查是否允许请求
     * 
     * 使用滑动窗口算法:
     * 1. 获取当前时间
     * 2. 移除窗口外的旧时间戳
     * 3. 检查当前窗口内的请求数是否超过限制
     * 4. 如果允许，记录新的时间戳
     * 
     * @param key 限制 Key
     * @param limit 限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return 是否允许请求
     */
    @Override
    public synchronized boolean isAllowed(String key, int limit, int windowSeconds) {
        if (key == null || key.isBlank()) {
            return true; // 无效 Key 不限制
        }

        if (limit <= 0 || windowSeconds <= 0) {
            return true; // 无效配置不限制
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);

        // 获取或创建时间戳队列
        Deque<Long> timestamps = requestTimestamps.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // 移除窗口外的旧时间戳
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        // 检查是否超过限制
        if (timestamps.size() >= limit) {
            log.debug("Rate limit exceeded for key: {}, count: {}, limit: {}", 
                    key, timestamps.size(), limit);
            return false;
        }

        // 记录新的时间戳
        timestamps.addLast(now);

        // 更新窗口开始时间（用于计算重置时间）
        if (!windowStartTimes.containsKey(key) || windowStartTimes.get(key) < windowStart) {
            windowStartTimes.put(key, now);
        }

        log.debug("Request allowed for key: {}, count: {}/{}", key, timestamps.size(), limit);
        return true;
    }

    /**
     * 获取剩余配额
     * 
     * @param key 限制 Key
     * @param limit 限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return 剩余可用请求次数
     */
    @Override
    public synchronized int getRemainingQuota(String key, int limit, int windowSeconds) {
        if (key == null || key.isBlank() || limit <= 0 || windowSeconds <= 0) {
            return limit;
        }

        int currentCount = getCurrentCount(key, windowSeconds);
        return Math.max(0, limit - currentCount);
    }

    /**
     * 获取重置时间（秒）
     * 
     * 返回距离最早请求过期的时间
     * 
     * @param key 限制 Key
     * @param windowSeconds 时间窗口（秒）
     * @return 距离配额重置的秒数
     */
    @Override
    public synchronized long getResetTimeSeconds(String key, int windowSeconds) {
        if (key == null || key.isBlank() || windowSeconds <= 0) {
            return 0;
        }

        Deque<Long> timestamps = requestTimestamps.get(key);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);

        // 清理过期时间戳
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.isEmpty()) {
            return 0;
        }

        // 计算最早时间戳何时过期
        Long earliestTimestamp = timestamps.peekFirst();
        if (earliestTimestamp == null) {
            return 0;
        }

        long expiryTime = earliestTimestamp + (windowSeconds * 1000L);
        long resetTimeMs = expiryTime - now;

        return Math.max(0, (resetTimeMs + 999) / 1000); // 向上取整到秒
    }

    /**
     * 获取当前请求计数
     * 
     * @param key 限制 Key
     * @param windowSeconds 时间窗口（秒）
     * @return 当前时间窗口内的请求次数
     */
    @Override
    public synchronized int getCurrentCount(String key, int windowSeconds) {
        if (key == null || key.isBlank() || windowSeconds <= 0) {
            return 0;
        }

        Deque<Long> timestamps = requestTimestamps.get(key);
        if (timestamps == null) {
            return 0;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);

        // 移除窗口外的旧时间戳
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        return timestamps.size();
    }

    /**
     * 重置指定 Key 的计数
     * 
     * @param key 限制 Key
     */
    @Override
    public synchronized void reset(String key) {
        if (key != null) {
            requestTimestamps.remove(key);
            windowStartTimes.remove(key);
            log.debug("Reset rate limit for key: {}", key);
        }
    }

    /**
     * 清理过期的记录
     * 
     * 定期执行，移除所有空的时间戳队列以释放内存
     */
    @Override
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public synchronized void cleanup() {
        long now = Instant.now().toEpochMilli();
        int cleanedCount = 0;

        // 遍历所有 Key，移除空队列
        var iterator = requestTimestamps.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Deque<Long> timestamps = entry.getValue();

            // 移除所有过期的时间戳（假设最大窗口为 1 小时）
            long maxWindowStart = now - (3600 * 1000L);
            while (!timestamps.isEmpty() && timestamps.peekFirst() < maxWindowStart) {
                timestamps.pollFirst();
            }

            // 如果队列为空，移除整个条目
            if (timestamps.isEmpty()) {
                iterator.remove();
                windowStartTimes.remove(entry.getKey());
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.debug("Cleaned up {} expired rate limit entries", cleanedCount);
        }
    }

    /**
     * 获取当前存储的 Key 数量（用于监控）
     * 
     * @return Key 数量
     */
    public int getKeyCount() {
        return requestTimestamps.size();
    }

    /**
     * 获取所有 Key 的总请求数（用于监控）
     * 
     * @return 总请求数
     */
    public long getTotalRequestCount() {
        return requestTimestamps.values().stream()
                .mapToLong(Deque::size)
                .sum();
    }
}
