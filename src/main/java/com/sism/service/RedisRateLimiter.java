package com.sism.service;

import com.sism.util.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis实现的频率限制器
 *
 * 使用Redis INCR命令实现计数器:
 * - 使用INCR原子递增计数器
 * - 使用EXPIRE设置时间窗口
 * - 支持分布式部署场景
 *
 * 当Redis不可用时，自动降级到InMemoryRateLimiter
 *
 * **Property P9**: 在时间窗口内，请求次数超过限制时返回 429
 * **Property 1**: Redis Storage Consistency
 *
 * **Validates: Requirements 1.2, 1.5, 1.7**
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisRateLimiter implements RateLimiter {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final InMemoryRateLimiter fallbackLimiter;
    
    // Metrics counters for monitoring fallback events
    private final AtomicLong fallbackCounter = new AtomicLong(0);
    private final AtomicLong totalRequestCounter = new AtomicLong(0);
    private final AtomicLong redisSuccessCounter = new AtomicLong(0);

    @Autowired
    public RedisRateLimiter(RedisTemplate<String, Object> redisTemplate, 
                           InMemoryRateLimiter fallbackLimiter) {
        this.redisTemplate = redisTemplate;
        this.fallbackLimiter = fallbackLimiter;
        log.info("RedisRateLimiter initialized with fallback support");
    }
    
    /**
     * Handle Redis operation failure and fallback to in-memory limiter
     * 
     * @param operation Operation name for logging
     * @param key The rate limit key
     * @param e The exception that occurred
     */
    private void handleRedisFallback(String operation, String key, Exception e) {
        long fallbackCount = fallbackCounter.incrementAndGet();
        long totalCount = totalRequestCounter.get();
        double fallbackRate = totalCount > 0 ? (fallbackCount * 100.0 / totalCount) : 0;
        
        Map<String, Object> details = Map.of(
            "operation", operation,
            "key", key,
            "fallbackCount", fallbackCount,
            "totalRequests", totalCount,
            "fallbackRate", String.format("%.2f%%", fallbackRate),
            "exceptionType", e.getClass().getSimpleName()
        );
        
        if (e instanceof RedisConnectionFailureException) {
            LoggingUtils.logError(log, 
                "Redis connection failed, falling back to in-memory rate limiter", 
                details, e);
        } else {
            LoggingUtils.logError(log, 
                "Redis operation failed, falling back to in-memory rate limiter", 
                details, e);
        }
    }

    /**
     * 检查是否允许请求
     * 
     * 使用Redis固定窗口算法:
     * 1. 使用INCR原子递增计数器
     * 2. 如果是第一次请求(count=1)，设置过期时间
     * 3. 检查计数是否超过限制
     * 
     * 降级机制:
     * - 捕获RedisConnectionFailureException和其他Redis异常
     * - 自动降级到InMemoryRateLimiter
     * - 记录详细的降级事件和metrics
     * 
     * @param key 限制 Key
     * @param limit 限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return 是否允许请求
     */
    @Override
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        totalRequestCounter.incrementAndGet();
        
        if (key == null || key.isBlank()) {
            return true; // 无效 Key 不限制
        }

        if (limit <= 0 || windowSeconds <= 0) {
            return true; // 无效配置不限制
        }

        try {
            long startTime = System.currentTimeMillis();
            String redisKey = RATE_LIMIT_KEY_PREFIX + key;
            
            // 原子递增计数器
            Long count = redisTemplate.opsForValue().increment(redisKey);
            
            if (count == null) {
                LoggingUtils.logWarning(log, 
                    "Redis INCR returned null, falling back to in-memory limiter",
                    Map.of("key", key, "operation", "isAllowed"));
                fallbackCounter.incrementAndGet();
                return fallbackLimiter.isAllowed(key, limit, windowSeconds);
            }
            
            // 如果是第一次请求，设置过期时间
            if (count == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
                log.debug("Set expiry for key: {}, window: {}s", redisKey, windowSeconds);
            }
            
            boolean allowed = count <= limit;
            redisSuccessCounter.incrementAndGet();
            
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 100) {
                LoggingUtils.logWarning(log, 
                    "Redis rate limiter operation slow",
                    Map.of("key", key, "duration", duration + "ms", "operation", "isAllowed"));
            }
            
            if (allowed) {
                log.debug("Request allowed for key: {}, count: {}/{}", key, count, limit);
            } else {
                log.debug("Rate limit exceeded for key: {}, count: {}, limit: {}", 
                        key, count, limit);
            }
            
            return allowed;
            
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("isAllowed", key, e);
            return fallbackLimiter.isAllowed(key, limit, windowSeconds);
        } catch (Exception e) {
            handleRedisFallback("isAllowed", key, e);
            return fallbackLimiter.isAllowed(key, limit, windowSeconds);
        }
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
    public int getRemainingQuota(String key, int limit, int windowSeconds) {
        if (key == null || key.isBlank() || limit <= 0 || windowSeconds <= 0) {
            return limit;
        }

        try {
            int currentCount = getCurrentCount(key, windowSeconds);
            return Math.max(0, limit - currentCount);
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("getRemainingQuota", key, e);
            return fallbackLimiter.getRemainingQuota(key, limit, windowSeconds);
        } catch (Exception e) {
            handleRedisFallback("getRemainingQuota", key, e);
            return fallbackLimiter.getRemainingQuota(key, limit, windowSeconds);
        }
    }

    /**
     * 获取重置时间（秒）
     * 
     * 返回Redis key的TTL
     * 
     * @param key 限制 Key
     * @param windowSeconds 时间窗口（秒）
     * @return 距离配额重置的秒数
     */
    @Override
    public long getResetTimeSeconds(String key, int windowSeconds) {
        if (key == null || key.isBlank() || windowSeconds <= 0) {
            return 0;
        }

        try {
            String redisKey = RATE_LIMIT_KEY_PREFIX + key;
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            
            if (ttl == null || ttl < 0) {
                return 0;
            }
            
            return ttl;
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("getResetTimeSeconds", key, e);
            return fallbackLimiter.getResetTimeSeconds(key, windowSeconds);
        } catch (Exception e) {
            handleRedisFallback("getResetTimeSeconds", key, e);
            return fallbackLimiter.getResetTimeSeconds(key, windowSeconds);
        }
    }

    /**
     * 获取当前请求计数
     * 
     * @param key 限制 Key
     * @param windowSeconds 时间窗口（秒）
     * @return 当前时间窗口内的请求次数
     */
    @Override
    public int getCurrentCount(String key, int windowSeconds) {
        if (key == null || key.isBlank() || windowSeconds <= 0) {
            return 0;
        }

        try {
            String redisKey = RATE_LIMIT_KEY_PREFIX + key;
            Object value = redisTemplate.opsForValue().get(redisKey);
            
            if (value == null) {
                return 0;
            }
            
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            
            return Integer.parseInt(value.toString());
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("getCurrentCount", key, e);
            return fallbackLimiter.getCurrentCount(key, windowSeconds);
        } catch (Exception e) {
            handleRedisFallback("getCurrentCount", key, e);
            return fallbackLimiter.getCurrentCount(key, windowSeconds);
        }
    }

    /**
     * 重置指定 Key 的计数
     * 
     * @param key 限制 Key
     */
    @Override
    public void reset(String key) {
        if (key == null) {
            return;
        }

        try {
            String redisKey = RATE_LIMIT_KEY_PREFIX + key;
            redisTemplate.delete(redisKey);
            log.debug("Reset rate limit for key: {}", key);
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("reset", key, e);
            fallbackLimiter.reset(key);
        } catch (Exception e) {
            handleRedisFallback("reset", key, e);
            fallbackLimiter.reset(key);
        }
    }

    /**
     * 清理过期的记录
     * 
     * Redis会自动清理过期的key，此方法为空实现
     */
    @Override
    public void cleanup() {
        // Redis自动清理过期key，无需手动清理
        log.debug("Redis rate limiter cleanup called (no-op, Redis handles expiry automatically)");
    }
    
    /**
     * 获取降级统计信息（用于监控和告警）
     * 
     * @return 包含降级metrics的Map
     */
    public Map<String, Object> getFallbackMetrics() {
        long totalCount = totalRequestCounter.get();
        long fallbackCount = fallbackCounter.get();
        long successCount = redisSuccessCounter.get();
        double fallbackRate = totalCount > 0 ? (fallbackCount * 100.0 / totalCount) : 0;
        double successRate = totalCount > 0 ? (successCount * 100.0 / totalCount) : 0;
        
        return Map.of(
            "totalRequests", totalCount,
            "redisSuccessCount", successCount,
            "fallbackCount", fallbackCount,
            "successRate", String.format("%.2f%%", successRate),
            "fallbackRate", String.format("%.2f%%", fallbackRate)
        );
    }
    
    /**
     * 重置metrics计数器（用于测试）
     */
    public void resetMetrics() {
        totalRequestCounter.set(0);
        fallbackCounter.set(0);
        redisSuccessCounter.set(0);
        log.debug("Reset fallback metrics counters");
    }
}
