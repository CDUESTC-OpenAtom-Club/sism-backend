package com.sism.service;

import com.sism.util.JwtUtil;
import com.sism.util.LoggingUtils;
import com.sism.util.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis实现的Token黑名单服务
 * 
 * 使用Redis存储失效的JWT Token:
 * - 使用token作为key存储到Redis
 * - 设置TTL为token的过期时间
 * - 支持分布式部署场景
 * 
 * 当Redis不可用时，自动降级到TokenBlacklistService（内存实现）
 * 
 * **Property 1**: Redis Storage Consistency
 * 
 * **Validates: Requirements 1.3, 1.5, 1.7**
 */
@Slf4j
public class RedisTokenBlacklist extends TokenBlacklistService {

    private static final String TOKEN_BLACKLIST_KEY_PREFIX = "token_blacklist:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenBlacklistService fallbackService;
    private final JwtUtil jwtUtil;
    
    // Metrics counters for monitoring fallback events
    private final AtomicLong fallbackCounter = new AtomicLong(0);
    private final AtomicLong totalRequestCounter = new AtomicLong(0);
    private final AtomicLong redisSuccessCounter = new AtomicLong(0);

    @Autowired
    public RedisTokenBlacklist(RedisTemplate<String, Object> redisTemplate,
                               TokenBlacklistService fallbackService,
                               JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.fallbackService = fallbackService;
        this.jwtUtil = jwtUtil;
        log.info("RedisTokenBlacklist initialized with fallback support");
    }
    
    /**
     * Handle Redis operation failure and fallback to in-memory service
     * 
     * @param operation Operation name for logging
     * @param token The token (truncated for security)
     * @param e The exception that occurred
     */
    private void handleRedisFallback(String operation, String token, Exception e) {
        long fallbackCount = fallbackCounter.incrementAndGet();
        long totalCount = totalRequestCounter.get();
        double fallbackRate = totalCount > 0 ? (fallbackCount * 100.0 / totalCount) : 0;
        
        // Truncate token for security in logs
        String tokenPreview = token != null && token.length() > 20 
            ? token.substring(0, 20) + "..." 
            : "null";
        
        Map<String, Object> details = Map.of(
            "operation", operation,
            "tokenPreview", tokenPreview,
            "fallbackCount", fallbackCount,
            "totalRequests", totalCount,
            "fallbackRate", String.format("%.2f%%", fallbackRate),
            "exceptionType", e.getClass().getSimpleName()
        );
        
        if (e instanceof RedisConnectionFailureException) {
            LoggingUtils.logError(log, 
                "Redis connection failed, falling back to in-memory token blacklist", 
                details, e);
        } else {
            LoggingUtils.logError(log, 
                "Redis operation failed, falling back to in-memory token blacklist", 
                details, e);
        }
    }

    /**
     * 将Token添加到黑名单
     * 
     * 使用token作为key存储到Redis，并设置TTL为token的过期时间。
     * 这样可以让Redis自动清理过期的黑名单记录。
     * 
     * 降级机制:
     * - 捕获RedisConnectionFailureException和其他Redis异常
     * - 自动降级到TokenBlacklistService（内存实现）
     * - 记录详细的降级事件和metrics
     * 
     * @param token JWT token to blacklist
     */
    @Override
    public void blacklist(String token) {
        totalRequestCounter.incrementAndGet();
        
        if (token == null || token.isEmpty()) {
            log.warn("Attempted to blacklist null or empty token");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            String redisKey = TOKEN_BLACKLIST_KEY_PREFIX + token;
            
            // Extract token expiration time to set appropriate TTL
            Date expiration = jwtUtil.extractExpiration(token);
            Date now = new Date();
            long ttlMillis = expiration.getTime() - now.getTime();
            
            if (ttlMillis <= 0) {
                log.debug("Token already expired, not adding to blacklist: {}", 
                    token.substring(0, Math.min(20, token.length())) + "...");
                return;
            }
            
            Duration ttl = Duration.ofMillis(ttlMillis);
            
            // Store token in Redis with TTL
            redisTemplate.opsForValue().set(redisKey, true, ttl);
            redisSuccessCounter.incrementAndGet();
            
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 100) {
                LoggingUtils.logWarning(log, 
                    "Redis token blacklist operation slow",
                    Map.of("duration", duration + "ms", "operation", "blacklist"));
            }
            
            log.debug("Token added to Redis blacklist with TTL: {}ms", ttlMillis);
            
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("blacklist", token, e);
            fallbackService.blacklist(token);
        } catch (Exception e) {
            handleRedisFallback("blacklist", token, e);
            fallbackService.blacklist(token);
        }
    }

    /**
     * 检查Token是否在黑名单中
     * 
     * 通过检查Redis中是否存在对应的key来判断token是否被列入黑名单。
     * 
     * 降级机制:
     * - 捕获RedisConnectionFailureException和其他Redis异常
     * - 自动降级到TokenBlacklistService（内存实现）
     * - 记录详细的降级事件和metrics
     * 
     * @param token JWT token to check
     * @return true if token is blacklisted
     */
    @Override
    public boolean isBlacklisted(String token) {
        totalRequestCounter.incrementAndGet();
        
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            long startTime = System.currentTimeMillis();
            String redisKey = TOKEN_BLACKLIST_KEY_PREFIX + token;
            
            Boolean exists = redisTemplate.hasKey(redisKey);
            boolean isBlacklisted = Boolean.TRUE.equals(exists);
            
            redisSuccessCounter.incrementAndGet();
            
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 100) {
                LoggingUtils.logWarning(log, 
                    "Redis token blacklist check slow",
                    Map.of("duration", duration + "ms", "operation", "isBlacklisted"));
            }
            
            if (isBlacklisted) {
                log.debug("Token found in Redis blacklist");
            }
            
            return isBlacklisted;
            
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("isBlacklisted", token, e);
            return fallbackService.isBlacklisted(token);
        } catch (Exception e) {
            handleRedisFallback("isBlacklisted", token, e);
            return fallbackService.isBlacklisted(token);
        }
    }

    /**
     * 从黑名单中移除Token（可选的清理操作）
     * 
     * @param token JWT token to remove
     */
    @Override
    public void remove(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            String redisKey = TOKEN_BLACKLIST_KEY_PREFIX + token;
            redisTemplate.delete(redisKey);
            log.debug("Token removed from Redis blacklist");
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("remove", token, e);
            fallbackService.remove(token);
        } catch (Exception e) {
            handleRedisFallback("remove", token, e);
            fallbackService.remove(token);
        }
    }

    /**
     * 清空所有黑名单Token（用于测试）
     */
    @Override
    public void clear() {
        try {
            // Note: This is a potentially expensive operation in production
            // Consider using Redis SCAN instead of KEYS for production
            var keys = redisTemplate.keys(TOKEN_BLACKLIST_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cleared {} tokens from Redis blacklist", keys.size());
            }
        } catch (RedisConnectionFailureException e) {
            handleRedisFallback("clear", "all", e);
            fallbackService.clear();
        } catch (Exception e) {
            handleRedisFallback("clear", "all", e);
            fallbackService.clear();
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
