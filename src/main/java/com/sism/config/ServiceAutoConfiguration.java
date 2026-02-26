package com.sism.config;

import com.sism.service.*;
import com.sism.util.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 服务自动配置类
 * 
 * 根据配置属性自动注入 Redis 或内存实现:
 * - redis.enabled=true: 使用 Redis 实现
 * - redis.enabled=false: 使用内存实现
 * 
 * **Validates: Requirements 1.2, 1.3, 1.4**
 */
@Slf4j
@Configuration
public class ServiceAutoConfiguration {

    /**
     * Redis 频率限制器（当 Redis 启用时）
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true")
    public RateLimiter redisRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        log.info("Configuring RedisRateLimiter (Redis enabled)");
        // Create fallback instance for Redis to use
        InMemoryRateLimiter fallback = new InMemoryRateLimiter();
        return new RedisRateLimiter(redisTemplate, fallback);
    }

    /**
     * 内存频率限制器（当 Redis 禁用时）
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
    public RateLimiter inMemoryRateLimiter() {
        log.info("Configuring InMemoryRateLimiter (Redis disabled or not configured)");
        return new InMemoryRateLimiter();
    }

    /**
     * Redis Token 黑名单（当 Redis 启用时）
     */
    @Bean("tokenBlacklistService")
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true")
    public TokenBlacklistService redisTokenBlacklist(
            RedisTemplate<String, Object> redisTemplate,
            com.sism.util.JwtUtil jwtUtil) {
        log.info("Configuring RedisTokenBlacklist (Redis enabled)");
        // Create a fallback instance for Redis to use
        TokenBlacklistService fallback = new TokenBlacklistService();
        return new RedisTokenBlacklist(redisTemplate, fallback, jwtUtil);
    }

    /**
     * 内存 Token 黑名单（当 Redis 禁用时）
     */
    @Bean("tokenBlacklistService")
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
    public TokenBlacklistService inMemoryTokenBlacklist() {
        log.info("Configuring InMemoryTokenBlacklist (Redis disabled or not configured)");
        return new TokenBlacklistService();
    }

    // Note: IdempotencyService configuration is handled separately
    // - Database implementation: IdempotencyService (already has @Service annotation)
    // - Redis implementation: RedisIdempotencyService (already has @Service with @ConditionalOnProperty)
    // The filter will use whichever implementation is active based on redis.enabled property
}
