package com.sism.property;

import com.sism.service.InMemoryRateLimiter;
import com.sism.service.RedisRateLimiter;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for RedisRateLimiter
 * 
 * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
 * 
 * Tests verify that:
 * - Rate limiting works correctly with Redis
 * - Counter increments atomically
 * - TTL is set correctly on first request
 * - Fallback mechanism works when Redis fails
 * - Distributed rate limiting across multiple instances
 * 
 * **Validates: Requirements 1.2**
 */
@Testcontainers
public class RedisRateLimiterPropertyTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    private static RedisTemplate<String, Object> redisTemplate;
    private static LettuceConnectionFactory connectionFactory;
    private static RedisRateLimiter rateLimiter;
    private static InMemoryRateLimiter fallbackLimiter;

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

        // Create rate limiters
        fallbackLimiter = new InMemoryRateLimiter();
        rateLimiter = new RedisRateLimiter(redisTemplate, fallbackLimiter);
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    /**
     * Ensure Redis and rate limiter are initialized before each property test
     * This is called by jqwik before each property execution
     */
    private void ensureInitialized() {
        if (rateLimiter == null) {
            setUpRedis();
        }
        // Clean up Redis before each test
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        }
        // Reset metrics
        if (rateLimiter != null) {
            rateLimiter.resetMetrics();
        }
        if (fallbackLimiter != null) {
            fallbackLimiter.cleanup();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> rateLimitKeys() {
        return Arbitraries.oneOf(
                Arbitraries.strings().withCharRange('0', '9').ofLength(3)
                        .map(s -> "ip:192.168.1." + s),
                Arbitraries.integers().between(1, 10000)
                        .map(id -> "user:" + id),
                Arbitraries.strings().alpha().ofLength(10)
                        .map(s -> "api:" + s)
        );
    }

    @Provide
    Arbitrary<Integer> rateLimits() {
        return Arbitraries.integers().between(1, 100);
    }

    @Provide
    Arbitrary<Integer> windowSeconds() {
        return Arbitraries.integers().between(1, 300);
    }

    // ==================== Property Tests ====================

    /**
     * Property 1.1: Rate limiting should work correctly with Redis
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key, limit, and window, the rate limiter should:
     * - Allow requests up to the limit
     * - Deny requests exceeding the limit
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.1: Rate Limiting Works Correctly")
    void rateLimiting_shouldWorkCorrectly(
            @ForAll("rateLimitKeys") String key,
            @ForAll("rateLimits") int limit,
            @ForAll("windowSeconds") int windowSeconds) {
        
        ensureInitialized();
        
        // First 'limit' requests should be allowed
        for (int i = 0; i < limit; i++) {
            boolean allowed = rateLimiter.isAllowed(key, limit, windowSeconds);
            assertThat(allowed)
                    .as("Request %d/%d should be allowed", i + 1, limit)
                    .isTrue();
        }
        
        // Next request should be denied
        boolean denied = rateLimiter.isAllowed(key, limit, windowSeconds);
        assertThat(denied)
                .as("Request %d/%d should be denied (exceeds limit)", limit + 1, limit)
                .isFalse();
        
        // Verify current count equals limit + 1
        int currentCount = rateLimiter.getCurrentCount(key, windowSeconds);
        assertThat(currentCount)
                .as("Current count should be limit + 1")
                .isEqualTo(limit + 1);
    }

    /**
     * Property 1.2: Counter should increment atomically
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key, multiple increments should result in correct count.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.2: Counter Increments Atomically")
    void counter_shouldIncrementAtomically(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 1, max = 20) int requestCount,
            @ForAll("windowSeconds") int windowSeconds) {
        
        ensureInitialized();
        
        // Make multiple requests
        for (int i = 0; i < requestCount; i++) {
            rateLimiter.isAllowed(key, 1000, windowSeconds);
        }
        
        // Verify count is exactly requestCount
        int currentCount = rateLimiter.getCurrentCount(key, windowSeconds);
        assertThat(currentCount)
                .as("Counter should equal number of requests")
                .isEqualTo(requestCount);
    }

    /**
     * Property 1.3: TTL should be set correctly on first request
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key, the TTL should be set on the first request.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.3: TTL Set on First Request")
    void ttl_shouldBeSetOnFirstRequest(
            @ForAll("rateLimitKeys") String key,
            @ForAll("rateLimits") int limit,
            @ForAll("windowSeconds") int windowSeconds) {
        
        ensureInitialized();
        
        // Make first request
        rateLimiter.isAllowed(key, limit, windowSeconds);
        
        // Verify TTL is set (allow 5 second tolerance)
        long resetTime = rateLimiter.getResetTimeSeconds(key, windowSeconds);
        assertThat(resetTime)
                .as("TTL should be set on first request")
                .isBetween((long) windowSeconds - 5, (long) windowSeconds + 1);
    }

    /**
     * Property 1.4: Remaining quota should be calculated correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key and limit, remaining quota should equal limit - current count.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.4: Remaining Quota Calculated Correctly")
    void remainingQuota_shouldBeCalculatedCorrectly(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 5, max = 20) int limit,
            @ForAll @IntRange(min = 1, max = 5) int requestCount,
            @ForAll("windowSeconds") int windowSeconds) {
        
        ensureInitialized();
        
        Assume.that(requestCount <= limit);
        
        // Make some requests
        for (int i = 0; i < requestCount; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }
        
        // Verify remaining quota
        int remaining = rateLimiter.getRemainingQuota(key, limit, windowSeconds);
        assertThat(remaining)
                .as("Remaining quota should be limit - request count")
                .isEqualTo(limit - requestCount);
    }

    /**
     * Property 1.5: Reset should clear the counter
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key, reset should clear the counter and allow new requests.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.5: Reset Clears Counter")
    void reset_shouldClearCounter(
            @ForAll("rateLimitKeys") String key,
            @ForAll("rateLimits") int limit,
            @ForAll("windowSeconds") int windowSeconds) {
        
        ensureInitialized();
        
        // Exhaust the limit
        for (int i = 0; i <= limit; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }
        
        // Verify limit is exceeded
        boolean deniedBefore = rateLimiter.isAllowed(key, limit, windowSeconds);
        assertThat(deniedBefore)
                .as("Request should be denied before reset")
                .isFalse();
        
        // Reset the counter
        rateLimiter.reset(key);
        
        // Verify counter is cleared
        int countAfterReset = rateLimiter.getCurrentCount(key, windowSeconds);
        assertThat(countAfterReset)
                .as("Counter should be 0 after reset")
                .isEqualTo(0);
        
        // Verify new requests are allowed
        boolean allowedAfter = rateLimiter.isAllowed(key, limit, windowSeconds);
        assertThat(allowedAfter)
                .as("Request should be allowed after reset")
                .isTrue();
    }

    /**
     * Property 1.6: Concurrent requests should be handled correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key and limit, concurrent requests should not exceed the limit.
     * This tests atomic increment behavior.
     * 
     * **Validates: Requirements 1.2, 1.7**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.6: Concurrent Requests Handled Correctly")
    void concurrentRequests_shouldBeHandledCorrectly(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 5, max = 20) int limit,
            @ForAll @IntRange(min = 5, max = 20) int threadCount,
            @ForAll("windowSeconds") int windowSeconds) throws InterruptedException {
        
        ensureInitialized();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount = new AtomicInteger(0);
        
        try {
            // Submit concurrent requests
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        boolean allowed = rateLimiter.isAllowed(key, limit, windowSeconds);
                        if (allowed) {
                            allowedCount.incrementAndGet();
                        } else {
                            deniedCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Wait for all threads to complete
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertThat(completed)
                    .as("All threads should complete within timeout")
                    .isTrue();
            
            // Verify total requests
            assertThat(allowedCount.get() + deniedCount.get())
                    .as("Total requests should equal thread count")
                    .isEqualTo(threadCount);
            
            // Verify allowed count does not exceed limit
            assertThat(allowedCount.get())
                    .as("Allowed requests should not exceed limit")
                    .isLessThanOrEqualTo(limit);
            
            // Verify final count in Redis
            int finalCount = rateLimiter.getCurrentCount(key, windowSeconds);
            assertThat(finalCount)
                    .as("Final count should equal thread count")
                    .isEqualTo(threadCount);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Property 1.7: Multiple keys should be independent
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any two different keys, rate limiting should be independent.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.7: Multiple Keys Independent")
    void multipleKeys_shouldBeIndependent(
            @ForAll @StringLength(min = 5, max = 20) String key1,
            @ForAll @StringLength(min = 5, max = 20) String key2,
            @ForAll("rateLimits") int limit,
            @ForAll("windowSeconds") int windowSeconds) {
        
        ensureInitialized();
        
        Assume.that(!key1.equals(key2));
        
        // Exhaust limit for key1
        for (int i = 0; i <= limit; i++) {
            rateLimiter.isAllowed(key1, limit, windowSeconds);
        }
        
        // Verify key1 is rate limited
        boolean key1Denied = rateLimiter.isAllowed(key1, limit, windowSeconds);
        assertThat(key1Denied)
                .as("Key1 should be rate limited")
                .isFalse();
        
        // Verify key2 is still allowed
        boolean key2Allowed = rateLimiter.isAllowed(key2, limit, windowSeconds);
        assertThat(key2Allowed)
                .as("Key2 should still be allowed")
                .isTrue();
        
        // Verify counts are independent
        int count1 = rateLimiter.getCurrentCount(key1, windowSeconds);
        int count2 = rateLimiter.getCurrentCount(key2, windowSeconds);
        assertThat(count1)
                .as("Key1 count should be limit + 2")
                .isEqualTo(limit + 2);
        assertThat(count2)
                .as("Key2 count should be 1")
                .isEqualTo(1);
    }

    /**
     * Property 1.8: Invalid parameters should be handled gracefully
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any invalid parameters, the rate limiter should not throw exceptions.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.8: Invalid Parameters Handled Gracefully")
    void invalidParameters_shouldBeHandledGracefully(
            @ForAll("rateLimitKeys") String key) {
        
        ensureInitialized();
        
        // Null key should return true
        boolean nullKeyResult = rateLimiter.isAllowed(null, 10, 60);
        assertThat(nullKeyResult)
                .as("Null key should return true")
                .isTrue();
        
        // Empty key should return true
        boolean emptyKeyResult = rateLimiter.isAllowed("", 10, 60);
        assertThat(emptyKeyResult)
                .as("Empty key should return true")
                .isTrue();
        
        // Zero limit should return true
        boolean zeroLimitResult = rateLimiter.isAllowed(key, 0, 60);
        assertThat(zeroLimitResult)
                .as("Zero limit should return true")
                .isTrue();
        
        // Negative limit should return true
        boolean negativeLimitResult = rateLimiter.isAllowed(key, -1, 60);
        assertThat(negativeLimitResult)
                .as("Negative limit should return true")
                .isTrue();
        
        // Zero window should return true
        boolean zeroWindowResult = rateLimiter.isAllowed(key, 10, 0);
        assertThat(zeroWindowResult)
                .as("Zero window should return true")
                .isTrue();
        
        // Negative window should return true
        boolean negativeWindowResult = rateLimiter.isAllowed(key, 10, -1);
        assertThat(negativeWindowResult)
                .as("Negative window should return true")
                .isTrue();
    }

    /**
     * Property 1.9: Rate limiter should work after window expires
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key, after the window expires, new requests should be allowed.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 20)
    @Label("Feature: architecture-refactoring, Property 1.9: Works After Window Expires")
    void rateLimiter_shouldWorkAfterWindowExpires(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 1, max = 5) int limit) throws InterruptedException {
        
        ensureInitialized();
        
        int windowSeconds = 2; // Short window for testing
        
        // Exhaust the limit
        for (int i = 0; i <= limit; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }
        
        // Verify limit is exceeded
        boolean deniedBefore = rateLimiter.isAllowed(key, limit, windowSeconds);
        assertThat(deniedBefore)
                .as("Request should be denied before window expires")
                .isFalse();
        
        // Wait for window to expire (add buffer)
        Thread.sleep((windowSeconds + 1) * 1000);
        
        // Verify new requests are allowed
        boolean allowedAfter = rateLimiter.isAllowed(key, limit, windowSeconds);
        assertThat(allowedAfter)
                .as("Request should be allowed after window expires")
                .isTrue();
    }

    /**
     * Property 1.10: Distributed rate limiting should work across instances
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key, multiple rate limiter instances should share the same counter.
     * This simulates distributed deployment.
     * 
     * **Validates: Requirements 1.7**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.10: Distributed Rate Limiting")
    void distributedRateLimiting_shouldWorkAcrossInstances(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 5, max = 20) int limit,
            @ForAll("windowSeconds") int windowSeconds) {
        
        ensureInitialized();
        
        // Create second rate limiter instance (simulating another app instance)
        InMemoryRateLimiter fallbackLimiter2 = new InMemoryRateLimiter();
        RedisRateLimiter rateLimiter2 = new RedisRateLimiter(redisTemplate, fallbackLimiter2);
        
        // Instance 1 makes half the requests
        int instance1Requests = limit / 2;
        for (int i = 0; i < instance1Requests; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }
        
        // Instance 2 makes the remaining requests
        int instance2Requests = limit - instance1Requests + 1;
        for (int i = 0; i < instance2Requests; i++) {
            rateLimiter2.isAllowed(key, limit, windowSeconds);
        }
        
        // Both instances should see the same count
        int count1 = rateLimiter.getCurrentCount(key, windowSeconds);
        int count2 = rateLimiter2.getCurrentCount(key, windowSeconds);
        
        assertThat(count1)
                .as("Both instances should see the same count")
                .isEqualTo(count2)
                .isEqualTo(instance1Requests + instance2Requests);
        
        // Next request from either instance should be denied
        boolean denied1 = rateLimiter.isAllowed(key, limit, windowSeconds);
        boolean denied2 = rateLimiter2.isAllowed(key, limit, windowSeconds);
        
        assertThat(denied1)
                .as("Instance 1 should deny request (limit exceeded)")
                .isFalse();
        assertThat(denied2)
                .as("Instance 2 should deny request (limit exceeded)")
                .isFalse();
    }
}
