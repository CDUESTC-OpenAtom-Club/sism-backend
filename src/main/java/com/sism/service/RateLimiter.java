package com.sism.service;

/**
 * 频率限制器接口
 * 
 * 功能:
 * - 检查请求是否允许
 * - 获取剩余配额
 * - 获取重置时间
 * 
 * 当前实现:
 * - InMemoryRateLimiter: 单机内存存储（默认）
 * 
 * 可选实现（需要添加 spring-boot-starter-data-redis 依赖）:
 * - RedisRateLimiter: 分布式 Redis 存储
 * 
 * 要启用 Redis 支持:
 * 1. 在 pom.xml 中添加 spring-boot-starter-data-redis 依赖
 * 2. 创建 RedisRateLimiter 实现类
 * 3. 配置 rate-limit.storage=redis
 * 
 * **Property P9**: 在时间窗口内，请求次数超过限制时返回 429
 * 
 * **Validates: Requirements 2.3.1, 2.3.2, 2.3.3, 2.3.4, 2.3.5**
 */
public interface RateLimiter {

    /**
     * 检查是否允许请求
     * 
     * @param key 限制 Key（如 IP 地址或用户 ID）
     * @param limit 限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return 是否允许请求
     */
    boolean isAllowed(String key, int limit, int windowSeconds);

    /**
     * 获取剩余配额
     * 
     * @param key 限制 Key
     * @param limit 限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return 剩余可用请求次数
     */
    int getRemainingQuota(String key, int limit, int windowSeconds);

    /**
     * 获取重置时间（秒）
     * 
     * @param key 限制 Key
     * @param windowSeconds 时间窗口（秒）
     * @return 距离配额重置的秒数
     */
    long getResetTimeSeconds(String key, int windowSeconds);

    /**
     * 获取当前请求计数
     * 
     * @param key 限制 Key
     * @param windowSeconds 时间窗口（秒）
     * @return 当前时间窗口内的请求次数
     */
    int getCurrentCount(String key, int windowSeconds);

    /**
     * 重置指定 Key 的计数（用于测试或管理）
     * 
     * @param key 限制 Key
     */
    void reset(String key);

    /**
     * 清理过期的记录（用于内存管理）
     */
    void cleanup();
}
