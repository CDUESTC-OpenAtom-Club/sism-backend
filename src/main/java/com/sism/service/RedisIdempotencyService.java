package com.sism.service;

import com.sism.util.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis实现的幂等性服务
 *
 * 使用Redis SETNX命令实现幂等性检查:
 * - 使用SETNX原子操作确保同一key只能被设置一次
 * - 设置TTL为24小时，自动清理过期记录
 * - 支持分布式部署场景
 *
 * 当Redis不可用时，自动降级到IdempotencyService（数据库实现）
 *
 * **Property 1**: Redis Storage Consistency
 *
 * **Validates: Requirements 1.4, 1.5, 1.7**
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisIdempotencyService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final int DEFAULT_TTL_HOURS = 24;
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final IdempotencyService fallbackService;
    
    // Metrics counters for monitoring fallback events
    private final AtomicLong fallbackCounter = new AtomicLong(0);
    private final AtomicLong totalRequestCounter = new AtomicLong(0);
    private final AtomicLong redisSuccessCounter = new AtomicLong(0);

    @Autowired
    public RedisIdempotencyService(RedisTemplate<String, Object> redisTemplate,
                                   IdempotencyService fallbackService) {
        this.redisTemplate = redisTemplate;
        this.fallbackService = fallbackService;
        log.info("RedisIdempotencyService initialized with fallback support");
    }
    
    /**
     * Handle Redis operation failure and fallback to database service
     * 
     * @param operation Operation name for logging
     * @param key The idempotency key (truncated for security)
     * @param e The exception that occurred
     */
    private void handleRedisFallback(String operation, String key, Exception e) {
        long fallbackCount = fallbackCounter.incrementAndGet();
        long totalCount = totalRequestCounter.get();
        double fallbackRate = totalCount > 0 ? (fallbackCount * 100.0 / totalCount) : 0;
        
        // Truncate key for security in logs
        String keyPreview = key != null && key.length() > 20 
            ? key.substring(0, 20) + "..." 
            : "null";
        
        Map<String, Object> details = Map.of(
            "operation", operation,
            "keyPreview", keyPreview,
            "fallbackCount", fallbackCount,
            "totalRequests", totalCount,
            "fallbackRate", String.format("%.2f%%", fallbackRate),
            "exceptionType", e.getClass().getSimpleName()
        );
        
        if (e instanceof RedisConnectionFailureException) {
            LoggingUtils.logError(log, 
                "Redis connection failed, falling back to database idempotency service", 
                details, e);
        } else {
            LoggingUtils.logError(log, 
                "Redis operation failed, falling back to database idempotency service", 
                details, e);
        }
    }

    /**
     * 尝试获取幂等性锁
     * 
     * 使用Redis SETNX命令原子性地设置key:
     * - 如果key不存在，设置成功并返回true（允许处理请求）
     * - 如果key已存在，设置失败并返回false（拒绝重复请求）
     * - 设置TTL为24小时，自动清理过期记录
     * 
     * 降级机制:
     * - 捕获RedisConnectionFailureException和其他Redis异常
     * - 自动降级到IdempotencyService（数据库实现）
     * - 记录详细的降级事件和metrics
     * 
     * @param idempotencyKey 幂等性Key
     * @return true if lock acquired (first request), false if duplicate
     */
    public boolean tryAcquire(String idempotencyKey) {
        return tryAcquire(idempotencyKey, Duration.ofHours(DEFAULT_TTL_HOURS));
    }

    /**
     * 尝试获取幂等性锁（自定义TTL）
     * 
     * @param idempotencyKey 幂等性Key
     * @param ttl 过期时间
     * @return true if lock acquired (first request), false if duplicate
     */
    public boolean tryAcquire(String idempotencyKey, Duration ttl) {
        totalRequestCounter.incrementAndGet();
        
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("Attempted to acquire lock with null or empty idempotency key");
            return true; // 无效key不限制
        }

        try {
            long startTime = System.currentTimeMillis();
            String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            
            // Use SETNX (SET if Not eXists) to atomically acquire lock
            Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, true, ttl);
            
            if (success == null) {
                LoggingUtils.logWarning(log, 
                    "Redis SETNX returned null, falling back to database service",
                    Map.of("key", idempotencyKey, "operation", "tryAcquire"));
                fallbackCounter.incrementAndGet();
                // Fallback: check if duplicate exists in database
                return fallbackService.checkDuplicate(idempotencyKey).isEmpty();
            }
            
            redisSuccessCounter.incrementAndGet();
            
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 100) {
                LoggingUtils.logWarning(log, 
                    "Redis idempotency operation slow",
                    Map.of("key", idempotencyKey, "duration", duration + "ms", 
                           "operation", "tryAcquire"));
            }
            
            if (success) {
                log.debug("Idempotency lock acquired for key: {}, TTL: {}", 
                    idempotencyKey.substring(0, Math.min(20, idempotencyKey.length())) + "...", 
                    ttl);
            } else {
                log.debug("Duplicate request detected for key: {}", 
                    idempotencyKey.substring(0, Math.min(20, idempotencyKey.length())) + "...");
            }
            
            return success;
            
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("tryAcquire", idempotencyKey, e);
            // Fallback: check if duplicate exists in database
            return fallbackService.checkDuplicate(idempotencyKey).isEmpty();
        } catch (Exception e) {
            handleRedisFallback("tryAcquire", idempotencyKey, e);
            // Fallback: check if duplicate exists in database
            return fallbackService.checkDuplicate(idempotencyKey).isEmpty();
        }
    }

    /**
     * 释放幂等性锁
     * 
     * 从Redis中删除对应的key，允许后续请求重新处理。
     * 通常用于请求处理失败需要回滚的场景。
     * 
     * 降级机制:
     * - 捕获RedisConnectionFailureException和其他Redis异常
     * - 自动降级到IdempotencyService（数据库实现）
     * - 记录详细的降级事件和metrics
     * 
     * @param idempotencyKey 幂等性Key
     */
    public void release(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        try {
            String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            redisTemplate.delete(redisKey);
            log.debug("Released idempotency lock for key: {}", 
                idempotencyKey.substring(0, Math.min(20, idempotencyKey.length())) + "...");
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("release", idempotencyKey, e);
            fallbackService.deleteRecord(idempotencyKey);
        } catch (Exception e) {
            handleRedisFallback("release", idempotencyKey, e);
            fallbackService.deleteRecord(idempotencyKey);
        }
    }

    /**
     * 检查幂等性Key是否存在
     * 
     * @param idempotencyKey 幂等性Key
     * @return true if key exists (duplicate request)
     */
    public boolean exists(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }

        try {
            String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            Boolean exists = redisTemplate.hasKey(redisKey);
            return Boolean.TRUE.equals(exists);
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("exists", idempotencyKey, e);
            return fallbackService.exists(idempotencyKey);
        } catch (Exception e) {
            handleRedisFallback("exists", idempotencyKey, e);
            return fallbackService.exists(idempotencyKey);
        }
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
