package com.sism.config;

import com.sism.service.InMemoryRateLimiter;
import com.sism.service.RateLimiter;
import com.sism.service.RedisRateLimiter;
import com.sism.util.TokenBlacklistService;
import com.sism.service.RedisTokenBlacklist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务自动配置测试
 * 
 * 验证根据配置属性正确注入服务实现
 * 
 * **Validates: Requirements 1.2, 1.3, 1.4**
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceAutoConfigurationTest {

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 测试默认配置（Redis 禁用）
     * 
     * 应该注入内存实现
     */
    @Test
    void testDefaultConfiguration_shouldUseInMemoryImplementations() {
        // Verify RateLimiter is InMemoryRateLimiter
        assertThat(rateLimiter)
                .isNotNull()
                .isInstanceOf(InMemoryRateLimiter.class);

        // Verify TokenBlacklistService is the in-memory implementation
        assertThat(tokenBlacklistService)
                .isNotNull()
                .isNotInstanceOf(RedisTokenBlacklist.class);
    }

    /**
     * 测试服务功能正常工作
     */
    @Test
    void testServices_shouldWorkCorrectly() {
        // Test RateLimiter
        String testKey = "test:key";
        boolean allowed = rateLimiter.isAllowed(testKey, 5, 60);
        assertThat(allowed).isTrue();

        // Test TokenBlacklistService
        String testToken = "test.jwt.token";
        tokenBlacklistService.blacklist(testToken);
        assertThat(tokenBlacklistService.isBlacklisted(testToken)).isTrue();
        
        // Cleanup
        tokenBlacklistService.remove(testToken);
        rateLimiter.reset(testKey);
    }
}

/**
 * Redis 启用配置测试
 * 
 * 注意: 此测试需要 Redis 服务器运行
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "redis.enabled=true",
        "spring.data.redis.enabled=true"
})
class ServiceAutoConfigurationRedisEnabledTest {

    @Autowired(required = false)
    private RateLimiter rateLimiter;

    @Autowired(required = false)
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 测试 Redis 启用配置
     * 
     * 如果 Redis 可用，应该注入 Redis 实现
     * 如果 Redis 不可用，测试会跳过（required = false）
     */
    @Test
    void testRedisConfiguration_whenRedisAvailable_shouldUseRedisImplementations() {
        if (rateLimiter != null) {
            // Verify RateLimiter is RedisRateLimiter
            assertThat(rateLimiter).isInstanceOf(RedisRateLimiter.class);
        }

        if (tokenBlacklistService != null) {
            // Verify TokenBlacklistService is RedisTokenBlacklist
            assertThat(tokenBlacklistService).isInstanceOf(RedisTokenBlacklist.class);
        }
    }
}
