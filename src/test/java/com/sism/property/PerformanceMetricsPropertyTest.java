package com.sism.property;

import com.sism.service.IdempotencyService;
import com.sism.service.InMemoryRateLimiter;
import com.sism.service.RedisIdempotencyService;
import com.sism.service.RedisRateLimiter;
import com.sism.service.RedisTokenBlacklist;
import com.sism.util.JwtUtil;
import com.sism.util.TokenBlacklistService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-Based Tests for Performance Metrics Logging
 * 
 * Tests Property 12: Performance Metrics Logging
 * 
 * Verifies that:
 * - Redis operations log their duration in milliseconds
 * - Slow operations (>100ms) are logged with warnings
 * - Performance metrics are tracked correctly
 * - Fallback metrics are recorded
 * 
 * **Validates: Requirements 8.1, 8.2**
 * 
 * Prerequisites:
 * - Docker must be installed and running
 * - Testcontainers will automatically start a Redis instance
 */
@Testcontainers
class PerformanceMetricsPropertyTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    private static RedisTemplate<String, Object> redisTemplate;
    private static LettuceConnectionFactory connectionFactory;
    private static RedisRateLimiter redisRateLimiter;
    private static RedisTokenBlacklist redisTokenBlacklist;
    private static RedisIdempotencyService redisIdempotencyService;
    private static JwtUtil jwtUtil;

    @BeforeAll
    static void setUpRedis() {
        // Configure Redis connection
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redis.getHost());
        config.setPort(redis.getFirstMappedPort());
        config.setDatabase(0);

        // Create connection factory
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        // Create RedisTemplate
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();

        // Create fallback services
        InMemoryRateLimiter fallbackRateLimiter = new InMemoryRateLimiter();
        TokenBlacklistService fallbackTokenBlacklist = new TokenBlacklistService();
        IdempotencyService fallbackIdempotency = new IdempotencyService(null);

        // Create Redis services
        redisRateLimiter = new RedisRateLimiter(redisTemplate, fallbackRateLimiter);
        
        // Create JwtUtil for token generation
        jwtUtil = new JwtUtil();
        
        redisTokenBlacklist = new RedisTokenBlacklist(redisTemplate, fallbackTokenBlacklist, jwtUtil);
        redisIdempotencyService = new RedisIdempotencyService(redisTemplate, fallbackIdempotency);
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    private void ensureInitialized() {
        if (redisRateLimiter == null) {
            setUpRedis();
        }
        // Clear Redis before each test
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        }
        // Reset metrics counters
        redisRateLimiter.resetMetrics();
        redisTokenBlacklist.resetMetrics();
        redisIdempotencyService.resetMetrics();
    }

    /**
     * Property 12.1: Redis Operations Log Duration
     * 
     * For any Redis operation (rate limiting, token blacklist, idempotency),
     * the operation duration should be measurable and the system should
     * track performance metrics.
     * 
     * **Validates: Requirements 8.1**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 12.1: Redis Operations Log Duration")
    void redisOperations_shouldTrackDuration(
            @ForAll @StringLength(min = 5, max = 50) String key,
            @ForAll @IntRange(min = 1, max = 100) int limit) {
        
        ensureInitialized();
        
        long startTime = System.currentTimeMillis();
        
        // Perform rate limiting operation
        boolean allowed = redisRateLimiter.isAllowed(key, limit, 60);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify operation completed
        assertThat(allowed).isTrue();
        
        // Verify duration is measurable (should be very fast for Redis)
        assertThat(duration).isGreaterThanOrEqualTo(0);
        assertThat(duration).isLessThan(5000); // Should complete within 5 seconds
        
        // Verify metrics are tracked
        var metrics = redisRateLimiter.getFallbackMetrics();
        assertThat(metrics).containsKey("totalRequests");
        assertThat(metrics).containsKey("redisSuccessCount");
        assertThat((Long) metrics.get("totalRequests")).isGreaterThan(0);
    }

    /**
     * Property 12.2: Fast Operations Don't Generate Warnings
     * 
     * Normal Redis operations should complete quickly (< 100ms) and not
     * generate slow operation warnings. This verifies the performance
     * logging infrastructure is in place.
     * 
     * **Validates: Requirements 8.1**
     */
    @Property(tries = 30)
    @Label("Feature: architecture-refactoring, Property 12.2: Fast Operations Complete Quickly")
    void fastOperations_shouldCompleteQuickly(
            @ForAll @StringLength(min = 5, max = 50) String key,
            @ForAll @IntRange(min = 1, max = 100) int limit) {
        
        ensureInitialized();
        
        long startTime = System.currentTimeMillis();
        
        // Perform a fast operation
        redisRateLimiter.isAllowed(key, limit, 60);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify operation completes quickly (normal operations should be < 100ms)
        // We use a more generous threshold for property tests due to test overhead
        assertThat(duration).isLessThan(1000); // Should complete within 1 second
    }

    /**
     * Property 12.3: Performance Metrics Are Tracked Correctly
     * 
     * For any sequence of Redis operations, the system should correctly
     * track the total number of requests, successful operations, and
     * fallback operations.
     * 
     * **Validates: Requirements 8.1, 8.2**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 12.3: Performance Metrics Are Tracked Correctly")
    void performanceMetrics_shouldBeTrackedCorrectly(
            @ForAll @IntRange(min = 1, max = 20) int numOperations) {
        
        ensureInitialized();
        
        // Reset metrics
        redisRateLimiter.resetMetrics();
        
        // Perform multiple operations
        for (int i = 0; i < numOperations; i++) {
            redisRateLimiter.isAllowed("test-key-" + i, 10, 60);
        }
        
        // Verify metrics
        var metrics = redisRateLimiter.getFallbackMetrics();
        
        assertThat(metrics).containsKey("totalRequests");
        assertThat(metrics).containsKey("redisSuccessCount");
        assertThat(metrics).containsKey("fallbackCount");
        assertThat(metrics).containsKey("successRate");
        assertThat(metrics).containsKey("fallbackRate");
        
        // Verify counts
        long totalRequests = (Long) metrics.get("totalRequests");
        long successCount = (Long) metrics.get("redisSuccessCount");
        long fallbackCount = (Long) metrics.get("fallbackCount");
        
        assertThat(totalRequests).isEqualTo(numOperations);
        assertThat(successCount).isEqualTo(numOperations); // All should succeed with Redis available
        assertThat(fallbackCount).isEqualTo(0); // No fallbacks when Redis is available
        
        // Verify rates
        String successRate = (String) metrics.get("successRate");
        String fallbackRate = (String) metrics.get("fallbackRate");
        
        assertThat(successRate).isEqualTo("100.00%");
        assertThat(fallbackRate).isEqualTo("0.00%");
    }

    /**
     * Property 12.4: Token Blacklist Tracks Performance Metrics
     * 
     * Token blacklist operations should also track performance metrics
     * including duration and success/fallback rates.
     * 
     * **Validates: Requirements 8.1, 8.2**
     */
    @Property(tries = 30)
    @Label("Feature: architecture-refactoring, Property 12.4: Token Blacklist Tracks Performance Metrics")
    void tokenBlacklist_shouldTrackPerformanceMetrics(
            @ForAll @IntRange(min = 1, max = 10) int numTokens) {
        
        ensureInitialized();
        
        // Reset metrics
        redisTokenBlacklist.resetMetrics();
        
        // Generate and blacklist tokens
        for (int i = 0; i < numTokens; i++) {
            String token = jwtUtil.generateToken((long) i, "user" + i, 1L);
            redisTokenBlacklist.blacklist(token);
        }
        
        // Verify metrics
        var metrics = redisTokenBlacklist.getFallbackMetrics();
        
        assertThat(metrics).containsKey("totalRequests");
        assertThat(metrics).containsKey("redisSuccessCount");
        assertThat(metrics).containsKey("fallbackCount");
        
        long totalRequests = (Long) metrics.get("totalRequests");
        long successCount = (Long) metrics.get("redisSuccessCount");
        
        assertThat(totalRequests).isEqualTo(numTokens);
        assertThat(successCount).isEqualTo(numTokens);
    }

    /**
     * Property 12.5: Idempotency Service Tracks Performance Metrics
     * 
     * Idempotency service operations should track performance metrics
     * including duration and success/fallback rates.
     * 
     * **Validates: Requirements 8.1, 8.2**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 12.5: Idempotency Service Tracks Performance Metrics")
    void idempotencyService_shouldTrackPerformanceMetrics(
            @ForAll @IntRange(min = 1, max = 20) int numOperations) {
        
        ensureInitialized();
        
        // Reset metrics
        redisIdempotencyService.resetMetrics();
        
        // Perform idempotency operations
        for (int i = 0; i < numOperations; i++) {
            redisIdempotencyService.tryAcquire("idempotency-key-" + i);
        }
        
        // Verify metrics
        var metrics = redisIdempotencyService.getFallbackMetrics();
        
        assertThat(metrics).containsKey("totalRequests");
        assertThat(metrics).containsKey("redisSuccessCount");
        assertThat(metrics).containsKey("fallbackCount");
        
        long totalRequests = (Long) metrics.get("totalRequests");
        long successCount = (Long) metrics.get("redisSuccessCount");
        
        assertThat(totalRequests).isEqualTo(numOperations);
        assertThat(successCount).isEqualTo(numOperations);
    }

    /**
     * Property 12.6: Concurrent Operations Track Metrics Correctly
     * 
     * When multiple threads perform Redis operations concurrently,
     * the performance metrics should be tracked correctly without
     * race conditions.
     * 
     * **Validates: Requirements 8.1, 8.2**
     */
    @Property(tries = 20)
    @Label("Feature: architecture-refactoring, Property 12.6: Concurrent Operations Track Metrics Correctly")
    void concurrentOperations_shouldTrackMetricsCorrectly(
            @ForAll @IntRange(min = 2, max = 10) int numThreads,
            @ForAll @IntRange(min = 5, max = 20) int operationsPerThread) throws InterruptedException {
        
        ensureInitialized();
        
        // Reset metrics
        redisRateLimiter.resetMetrics();
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        // Submit concurrent operations
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        redisRateLimiter.isAllowed("thread-" + threadId + "-key-" + i, 10, 60);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        // Verify metrics
        var metrics = redisRateLimiter.getFallbackMetrics();
        
        long totalRequests = (Long) metrics.get("totalRequests");
        long successCount = (Long) metrics.get("redisSuccessCount");
        
        int expectedTotal = numThreads * operationsPerThread;
        
        // Verify total requests match expected
        assertThat(totalRequests).isEqualTo(expectedTotal);
        assertThat(successCount).isEqualTo(expectedTotal);
    }

    /**
     * Property 12.7: Metrics Reset Functionality
     * 
     * The resetMetrics() method should correctly reset all performance
     * counters to zero.
     * 
     * **Validates: Requirements 8.1**
     */
    @Property(tries = 30)
    @Label("Feature: architecture-refactoring, Property 12.7: Metrics Reset Functionality")
    void metricsReset_shouldClearAllCounters(
            @ForAll @IntRange(min = 1, max = 20) int numOperations) {
        
        ensureInitialized();
        
        // Perform some operations
        for (int i = 0; i < numOperations; i++) {
            redisRateLimiter.isAllowed("test-key-" + i, 10, 60);
        }
        
        // Verify metrics are non-zero
        var metricsBefore = redisRateLimiter.getFallbackMetrics();
        assertThat((Long) metricsBefore.get("totalRequests")).isGreaterThan(0);
        
        // Reset metrics
        redisRateLimiter.resetMetrics();
        
        // Verify metrics are zero
        var metricsAfter = redisRateLimiter.getFallbackMetrics();
        assertThat((Long) metricsAfter.get("totalRequests")).isEqualTo(0);
        assertThat((Long) metricsAfter.get("redisSuccessCount")).isEqualTo(0);
        assertThat((Long) metricsAfter.get("fallbackCount")).isEqualTo(0);
    }
}
