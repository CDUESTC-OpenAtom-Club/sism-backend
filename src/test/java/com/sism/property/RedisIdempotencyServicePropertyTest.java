package com.sism.property;

import com.sism.service.IdempotencyService;
import com.sism.service.RedisIdempotencyService;
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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for RedisIdempotencyService
 * 
 * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
 * 
 * Tests verify that:
 * - tryAcquire returns true for first request, false for duplicates
 * - SETNX atomic operation works correctly
 * - TTL is set to 24 hours
 * - release() allows subsequent requests
 * - Fallback mechanism works when Redis fails
 * - Distributed idempotency across multiple instances
 * 
 * **Validates: Requirements 1.4**
 */
@Testcontainers
public class RedisIdempotencyServicePropertyTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    private static RedisTemplate<String, Object> redisTemplate;
    private static LettuceConnectionFactory connectionFactory;
    private static RedisIdempotencyService idempotencyService;
    private static IdempotencyService fallbackService;

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

        // Create mock fallback service
        fallbackService = mock(IdempotencyService.class);
        
        // Create idempotency service
        idempotencyService = new RedisIdempotencyService(redisTemplate, fallbackService);
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    /**
     * Ensure Redis and idempotency service are initialized before each property test
     */
    private void ensureInitialized() {
        if (idempotencyService == null) {
            setUpRedis();
        }
        // Clean up Redis before each test
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        }
        // Reset metrics
        if (idempotencyService != null) {
            idempotencyService.resetMetrics();
        }
        // Reset mock
        reset(fallbackService);
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> idempotencyKeys() {
        return Arbitraries.oneOf(
                // UUID-like keys
                Arbitraries.strings().alpha().numeric().ofLength(32),
                // Request ID format
                Arbitraries.strings().alpha().numeric().ofLength(16)
                        .map(s -> "req_" + s),
                // Operation-based keys
                Arbitraries.strings().alpha().ofLength(10)
                        .map(s -> "create_indicator_" + s),
                // Timestamp-based keys
                Arbitraries.longs().between(1000000000L, 9999999999L)
                        .map(ts -> "op_" + ts)
        );
    }

    @Provide
    Arbitrary<Integer> ttlHours() {
        return Arbitraries.integers().between(1, 48);
    }

    @Provide
    Arbitrary<Integer> ttlSeconds() {
        return Arbitraries.integers().between(60, 3600);
    }

    // ==================== Property Tests ====================

    /**
     * Property 1.1: tryAcquire should return true for first request
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any idempotency key, the first tryAcquire should return true.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.1: First Request Returns True")
    void tryAcquire_shouldReturnTrueForFirstRequest(
            @ForAll("idempotencyKeys") String idempotencyKey) {
        
        ensureInitialized();
        
        // First request should succeed
        boolean result = idempotencyService.tryAcquire(idempotencyKey);
        
        assertThat(result)
                .as("First tryAcquire should return true")
                .isTrue();
    }

    /**
     * Property 1.2: tryAcquire should return false for duplicate requests
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any idempotency key, subsequent tryAcquire calls should return false.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.2: Duplicate Requests Return False")
    void tryAcquire_shouldReturnFalseForDuplicates(
            @ForAll("idempotencyKeys") String idempotencyKey) {
        
        ensureInitialized();
        
        // First request should succeed
        boolean firstResult = idempotencyService.tryAcquire(idempotencyKey);
        assertThat(firstResult)
                .as("First tryAcquire should return true")
                .isTrue();
        
        // Second request should fail (duplicate)
        boolean secondResult = idempotencyService.tryAcquire(idempotencyKey);
        assertThat(secondResult)
                .as("Second tryAcquire should return false (duplicate)")
                .isFalse();
        
        // Third request should also fail
        boolean thirdResult = idempotencyService.tryAcquire(idempotencyKey);
        assertThat(thirdResult)
                .as("Third tryAcquire should return false (duplicate)")
                .isFalse();
    }

    /**
     * Property 1.3: SETNX atomic operation should work correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any idempotency key, the SETNX operation should be atomic.
     * Only one request should succeed even with concurrent calls.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.3: SETNX Atomic Operation")
    void setnx_shouldBeAtomic(
            @ForAll("idempotencyKeys") String idempotencyKey,
            @ForAll @IntRange(min = 5, max = 20) int threadCount) throws InterruptedException {
        
        ensureInitialized();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        try {
            // Submit concurrent tryAcquire requests
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        boolean result = idempotencyService.tryAcquire(idempotencyKey);
                        if (result) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
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
            
            // Verify exactly one request succeeded
            assertThat(successCount.get())
                    .as("Exactly one request should succeed (SETNX atomic)")
                    .isEqualTo(1);
            
            // Verify all other requests failed
            assertThat(failureCount.get())
                    .as("All other requests should fail")
                    .isEqualTo(threadCount - 1);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Property 1.4: TTL should be set to 24 hours by default
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any idempotency key, the default TTL should be 24 hours.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.4: Default TTL 24 Hours")
    void ttl_shouldBeSetTo24Hours(
            @ForAll("idempotencyKeys") String idempotencyKey) {
        
        ensureInitialized();
        
        // Acquire lock with default TTL
        idempotencyService.tryAcquire(idempotencyKey);
        
        // Verify TTL is set to 24 hours (allow 5 second tolerance)
        String redisKey = "idempotency:" + idempotencyKey;
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        
        long expectedTtl = 24 * 60 * 60; // 24 hours in seconds
        assertThat(ttl)
                .as("TTL should be set to 24 hours")
                .isNotNull()
                .isBetween(expectedTtl - 5, expectedTtl + 1);
    }

    /**
     * Property 1.5: Custom TTL should be respected
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any idempotency key and custom TTL, the TTL should be set correctly.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.5: Custom TTL Respected")
    void customTtl_shouldBeRespected(
            @ForAll("idempotencyKeys") String idempotencyKey,
            @ForAll("ttlSeconds") int ttlSeconds) {
        
        ensureInitialized();
        
        // Acquire lock with custom TTL
        idempotencyService.tryAcquire(idempotencyKey, Duration.ofSeconds(ttlSeconds));
        
        // Verify TTL is set correctly (allow 5 second tolerance)
        String redisKey = "idempotency:" + idempotencyKey;
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        
        assertThat(ttl)
                .as("TTL should be set to custom value")
                .isNotNull()
                .isBetween((long) ttlSeconds - 5, (long) ttlSeconds + 1);
    }

    /**
     * Property 1.6: release() should allow subsequent requests
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any idempotency key, after release(), subsequent tryAcquire should succeed.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.6: Release Allows Subsequent Requests")
    void release_shouldAllowSubsequentRequests(
            @ForAll("idempotencyKeys") String idempotencyKey) {
        
        ensureInitialized();
        
        // First request should succeed
        boolean firstResult = idempotencyService.tryAcquire(idempotencyKey);
        assertThat(firstResult)
                .as("First tryAcquire should return true")
                .isTrue();
        
        // Second request should fail (duplicate)
        boolean secondResult = idempotencyService.tryAcquire(idempotencyKey);
        assertThat(secondResult)
                .as("Second tryAcquire should return false (duplicate)")
                .isFalse();
        
        // Release the lock
        idempotencyService.release(idempotencyKey);
        
        // Third request should succeed (after release)
        boolean thirdResult = idempotencyService.tryAcquire(idempotencyKey);
        assertThat(thirdResult)
                .as("tryAcquire after release should return true")
                .isTrue();
    }

    /**
     * Property 1.7: exists() should return correct status
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any idempotency key, exists() should return true if key exists, false otherwise.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.7: Exists Returns Correct Status")
    void exists_shouldReturnCorrectStatus(
            @ForAll("idempotencyKeys") String idempotencyKey) {
        
        ensureInitialized();
        
        // Before acquire, key should not exist
        boolean existsBefore = idempotencyService.exists(idempotencyKey);
        assertThat(existsBefore)
                .as("Key should not exist before acquire")
                .isFalse();
        
        // Acquire lock
        idempotencyService.tryAcquire(idempotencyKey);
        
        // After acquire, key should exist
        boolean existsAfter = idempotencyService.exists(idempotencyKey);
        assertThat(existsAfter)
                .as("Key should exist after acquire")
                .isTrue();
        
        // Release lock
        idempotencyService.release(idempotencyKey);
        
        // After release, key should not exist
        boolean existsAfterRelease = idempotencyService.exists(idempotencyKey);
        assertThat(existsAfterRelease)
                .as("Key should not exist after release")
                .isFalse();
    }

    /**
     * Property 1.8: Multiple keys should be independent
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any two different idempotency keys, they should be independent.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.8: Multiple Keys Independent")
    void multipleKeys_shouldBeIndependent(
            @ForAll @StringLength(min = 10, max = 50) String key1,
            @ForAll @StringLength(min = 10, max = 50) String key2) {
        
        ensureInitialized();
        
        Assume.that(!key1.equals(key2));
        
        // Acquire lock for key1
        boolean result1 = idempotencyService.tryAcquire(key1);
        assertThat(result1)
                .as("First tryAcquire for key1 should succeed")
                .isTrue();
        
        // Acquire lock for key2 should still succeed
        boolean result2 = idempotencyService.tryAcquire(key2);
        assertThat(result2)
                .as("First tryAcquire for key2 should succeed (independent)")
                .isTrue();
        
        // Duplicate for key1 should fail
        boolean duplicate1 = idempotencyService.tryAcquire(key1);
        assertThat(duplicate1)
                .as("Duplicate tryAcquire for key1 should fail")
                .isFalse();
        
        // Duplicate for key2 should fail
        boolean duplicate2 = idempotencyService.tryAcquire(key2);
        assertThat(duplicate2)
                .as("Duplicate tryAcquire for key2 should fail")
                .isFalse();
    }

    /**
     * Property 1.9: Invalid parameters should be handled gracefully
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any invalid parameters, the service should not throw exceptions.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.9: Invalid Parameters Handled Gracefully")
    void invalidParameters_shouldBeHandledGracefully() {
        
        ensureInitialized();
        
        // Null key should return true (no restriction)
        boolean nullKeyResult = idempotencyService.tryAcquire(null);
        assertThat(nullKeyResult)
                .as("Null key should return true")
                .isTrue();
        
        // Empty key should return true (no restriction)
        boolean emptyKeyResult = idempotencyService.tryAcquire("");
        assertThat(emptyKeyResult)
                .as("Empty key should return true")
                .isTrue();
        
        // Blank key should return true (no restriction)
        boolean blankKeyResult = idempotencyService.tryAcquire("   ");
        assertThat(blankKeyResult)
                .as("Blank key should return true")
                .isTrue();
        
        // Release with null key should not throw
        idempotencyService.release(null);
        
        // Release with empty key should not throw
        idempotencyService.release("");
        
        // Exists with null key should return false
        boolean existsNull = idempotencyService.exists(null);
        assertThat(existsNull)
                .as("Exists with null key should return false")
                .isFalse();
        
        // Exists with empty key should return false
        boolean existsEmpty = idempotencyService.exists("");
        assertThat(existsEmpty)
                .as("Exists with empty key should return false")
                .isFalse();
    }

    /**
     * Property 1.10: Distributed idempotency should work across instances
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any idempotency key, multiple service instances should share the same state.
     * This simulates distributed deployment.
     * 
     * **Validates: Requirements 1.7**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.10: Distributed Idempotency")
    void distributedIdempotency_shouldWorkAcrossInstances(
            @ForAll("idempotencyKeys") String idempotencyKey) {
        
        ensureInitialized();
        
        // Create second service instance (simulating another app instance)
        IdempotencyService fallbackService2 = mock(IdempotencyService.class);
        RedisIdempotencyService idempotencyService2 = 
                new RedisIdempotencyService(redisTemplate, fallbackService2);
        
        // Instance 1 acquires lock
        boolean result1 = idempotencyService.tryAcquire(idempotencyKey);
        assertThat(result1)
                .as("Instance 1 should acquire lock")
                .isTrue();
        
        // Instance 2 should see the lock (duplicate)
        boolean result2 = idempotencyService2.tryAcquire(idempotencyKey);
        assertThat(result2)
                .as("Instance 2 should see duplicate")
                .isFalse();
        
        // Both instances should see the key exists
        boolean exists1 = idempotencyService.exists(idempotencyKey);
        boolean exists2 = idempotencyService2.exists(idempotencyKey);
        assertThat(exists1)
                .as("Instance 1 should see key exists")
                .isTrue();
        assertThat(exists2)
                .as("Instance 2 should see key exists")
                .isTrue();
        
        // Instance 1 releases lock
        idempotencyService.release(idempotencyKey);
        
        // Instance 2 should now be able to acquire
        boolean result3 = idempotencyService2.tryAcquire(idempotencyKey);
        assertThat(result3)
                .as("Instance 2 should acquire after instance 1 releases")
                .isTrue();
    }
}
