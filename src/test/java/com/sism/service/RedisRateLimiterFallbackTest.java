package com.sism.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 测试RedisRateLimiter的降级机制
 * 
 * 验证当Redis连接失败时，系统能够:
 * 1. 捕获RedisConnectionFailureException异常
 * 2. 自动降级到InMemoryRateLimiter
 * 3. 记录详细的降级事件和metrics
 * 4. 继续正常提供服务
 * 
 * **Validates: Requirements 1.5**
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisRateLimiter Fallback Mechanism Tests")
class RedisRateLimiterFallbackTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private InMemoryRateLimiter fallbackLimiter;

    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RedisRateLimiter(redisTemplate, fallbackLimiter);
        rateLimiter.resetMetrics();
    }

    @Test
    @DisplayName("Should fallback to in-memory limiter when Redis connection fails")
    void shouldFallbackOnRedisConnectionFailure() {
        // Given: Redis连接失败
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection refused"));
        when(fallbackLimiter.isAllowed(anyString(), anyInt(), anyInt()))
            .thenReturn(true);

        // When: 尝试检查限流
        boolean result = rateLimiter.isAllowed("test-key", 10, 60);

        // Then: 应该降级到内存实现并返回结果
        assertThat(result).isTrue();
        verify(fallbackLimiter).isAllowed("test-key", 10, 60);
        
        // 验证metrics被记录
        Map<String, Object> metrics = rateLimiter.getFallbackMetrics();
        assertThat(metrics.get("fallbackCount")).isEqualTo(1L);
        assertThat(metrics.get("totalRequests")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should fallback when Redis INCR returns null")
    void shouldFallbackWhenIncrReturnsNull() {
        // Given: Redis INCR返回null
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(null);
        when(fallbackLimiter.isAllowed(anyString(), anyInt(), anyInt()))
            .thenReturn(true);

        // When: 尝试检查限流
        boolean result = rateLimiter.isAllowed("test-key", 10, 60);

        // Then: 应该降级到内存实现
        assertThat(result).isTrue();
        verify(fallbackLimiter).isAllowed("test-key", 10, 60);
        
        Map<String, Object> metrics = rateLimiter.getFallbackMetrics();
        assertThat(metrics.get("fallbackCount")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should fallback on generic Redis exception")
    void shouldFallbackOnGenericException() {
        // Given: Redis抛出通用异常
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString()))
            .thenThrow(new RuntimeException("Redis timeout"));
        when(fallbackLimiter.isAllowed(anyString(), anyInt(), anyInt()))
            .thenReturn(false);

        // When: 尝试检查限流
        boolean result = rateLimiter.isAllowed("test-key", 10, 60);

        // Then: 应该降级到内存实现并返回结果
        assertThat(result).isFalse();
        verify(fallbackLimiter).isAllowed("test-key", 10, 60);
    }

    @Test
    @DisplayName("Should track fallback rate correctly")
    void shouldTrackFallbackRate() {
        // Given: 部分请求成功，部分失败
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString()))
            .thenReturn(1L)  // 第1次成功
            .thenReturn(2L)  // 第2次成功
            .thenThrow(new RedisConnectionFailureException("Connection lost"))  // 第3次失败
            .thenReturn(3L)  // 第4次成功
            .thenThrow(new RedisConnectionFailureException("Connection lost")); // 第5次失败
        
        when(fallbackLimiter.isAllowed(anyString(), anyInt(), anyInt()))
            .thenReturn(true);

        // When: 执行5次请求
        for (int i = 0; i < 5; i++) {
            rateLimiter.isAllowed("test-key-" + i, 10, 60);
        }

        // Then: 验证metrics统计正确
        Map<String, Object> metrics = rateLimiter.getFallbackMetrics();
        assertThat(metrics.get("totalRequests")).isEqualTo(5L);
        assertThat(metrics.get("redisSuccessCount")).isEqualTo(3L);
        assertThat(metrics.get("fallbackCount")).isEqualTo(2L);
        assertThat(metrics.get("fallbackRate")).isEqualTo("40.00%");
        assertThat(metrics.get("successRate")).isEqualTo("60.00%");
    }

    @Test
    @DisplayName("Should fallback for getRemainingQuota when Redis fails")
    void shouldFallbackForGetRemainingQuota() {
        // Given: Redis连接失败，getCurrentCount会fallback返回0
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection refused"));
        when(fallbackLimiter.getCurrentCount(anyString(), anyInt()))
            .thenReturn(3);  // fallback返回当前计数为3

        // When: 获取剩余配额
        int remaining = rateLimiter.getRemainingQuota("test-key", 10, 60);

        // Then: 应该降级到内存实现，剩余配额 = 10 - 3 = 7
        assertThat(remaining).isEqualTo(7);
        verify(fallbackLimiter).getCurrentCount("test-key", 60);
    }

    @Test
    @DisplayName("Should fallback for getResetTimeSeconds when Redis fails")
    void shouldFallbackForGetResetTimeSeconds() {
        // Given: Redis连接失败
        when(redisTemplate.getExpire(anyString(), any(TimeUnit.class)))
            .thenThrow(new RedisConnectionFailureException("Connection refused"));
        when(fallbackLimiter.getResetTimeSeconds(anyString(), anyInt()))
            .thenReturn(30L);

        // When: 获取重置时间
        long resetTime = rateLimiter.getResetTimeSeconds("test-key", 60);

        // Then: 应该降级到内存实现
        assertThat(resetTime).isEqualTo(30L);
        verify(fallbackLimiter).getResetTimeSeconds("test-key", 60);
    }

    @Test
    @DisplayName("Should fallback for getCurrentCount when Redis fails")
    void shouldFallbackForGetCurrentCount() {
        // Given: Redis连接失败
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection refused"));
        when(fallbackLimiter.getCurrentCount(anyString(), anyInt()))
            .thenReturn(7);

        // When: 获取当前计数
        int count = rateLimiter.getCurrentCount("test-key", 60);

        // Then: 应该降级到内存实现
        assertThat(count).isEqualTo(7);
        verify(fallbackLimiter).getCurrentCount("test-key", 60);
    }

    @Test
    @DisplayName("Should fallback for reset when Redis fails")
    void shouldFallbackForReset() {
        // Given: Redis连接失败
        when(redisTemplate.delete(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection refused"));
        doNothing().when(fallbackLimiter).reset(anyString());

        // When: 重置限流
        rateLimiter.reset("test-key");

        // Then: 应该降级到内存实现
        verify(fallbackLimiter).reset("test-key");
    }

    @Test
    @DisplayName("Should continue working after Redis recovers")
    void shouldRecoverAfterRedisRecovers() {
        // Given: Redis先失败后恢复
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection lost"))  // 第1次失败
            .thenReturn(1L);  // 第2次恢复
        
        when(fallbackLimiter.isAllowed(anyString(), anyInt(), anyInt()))
            .thenReturn(true);

        // When: 第一次请求失败
        boolean result1 = rateLimiter.isAllowed("test-key", 10, 60);
        assertThat(result1).isTrue();
        verify(fallbackLimiter).isAllowed("test-key", 10, 60);

        // When: 第二次请求成功
        boolean result2 = rateLimiter.isAllowed("test-key", 10, 60);
        assertThat(result2).isTrue();
        
        // Then: 验证metrics显示恢复
        Map<String, Object> metrics = rateLimiter.getFallbackMetrics();
        assertThat(metrics.get("totalRequests")).isEqualTo(2L);
        assertThat(metrics.get("fallbackCount")).isEqualTo(1L);
        assertThat(metrics.get("redisSuccessCount")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should handle null key gracefully without fallback")
    void shouldHandleNullKeyWithoutFallback() {
        // When: 使用null key
        boolean result = rateLimiter.isAllowed(null, 10, 60);

        // Then: 应该直接返回true，不触发fallback
        assertThat(result).isTrue();
        verifyNoInteractions(fallbackLimiter);
        
        Map<String, Object> metrics = rateLimiter.getFallbackMetrics();
        assertThat(metrics.get("fallbackCount")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should handle invalid parameters without fallback")
    void shouldHandleInvalidParametersWithoutFallback() {
        // When: 使用无效参数
        boolean result1 = rateLimiter.isAllowed("test-key", 0, 60);
        boolean result2 = rateLimiter.isAllowed("test-key", 10, -1);

        // Then: 应该直接返回true，不触发fallback
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        verifyNoInteractions(fallbackLimiter);
    }

    @Test
    @DisplayName("Should reset metrics correctly")
    void shouldResetMetrics() {
        // Given: 有一些fallback记录
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString()))
            .thenThrow(new RedisConnectionFailureException("Connection lost"));
        when(fallbackLimiter.isAllowed(anyString(), anyInt(), anyInt()))
            .thenReturn(true);

        rateLimiter.isAllowed("test-key", 10, 60);
        
        Map<String, Object> metricsBefore = rateLimiter.getFallbackMetrics();
        assertThat(metricsBefore.get("fallbackCount")).isEqualTo(1L);

        // When: 重置metrics
        rateLimiter.resetMetrics();

        // Then: metrics应该被清零
        Map<String, Object> metricsAfter = rateLimiter.getFallbackMetrics();
        assertThat(metricsAfter.get("totalRequests")).isEqualTo(0L);
        assertThat(metricsAfter.get("fallbackCount")).isEqualTo(0L);
        assertThat(metricsAfter.get("redisSuccessCount")).isEqualTo(0L);
    }
}
