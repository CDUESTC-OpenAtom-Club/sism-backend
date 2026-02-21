package com.sism.property;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.sism.service.*;
import com.sism.util.JwtUtil;
import com.sism.util.TokenBlacklistService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-Based Tests for Distributed State Sharing
 * 
 * Tests Property 2: Distributed State Sharing
 * 
 * Verifies that when one application instance writes to Redis (rate limit, 
 * token blacklist, or idempotency), all other instances immediately see 
 * the updated state.
 * 
 * This simulates multiple application instances by creating multiple service
 * instances that share the same Redis connection.
 * 
 * **Validates: Requirements 1.7**
 */
@Testcontainers
public class DistributedStatePropertyTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    private static RedisTemplate<String, Object> redisTemplate = createRedisTemplate();
    private static LettuceConnectionFactory connectionFactory;
    private static JwtUtil jwtUtil = new JwtUtil();

    // Simulate multiple application instances
    private static List<RedisRateLimiter> rateLimiterInstances = createRateLimiterInstances();
    private static List<RedisTokenBlacklist> tokenBlacklistInstances = createTokenBlacklistInstances();
    private static List<RedisIdempotencyService> idempotencyInstances = createIdempotencyInstances();

    private static RedisTemplate<String, Object> createRedisTemplate() {
        // Wait for container to start
        if (!redis.isRunning()) {
            redis.start();
        }

        // Configure Redis connection
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redis.getHost());
        config.setPort(redis.getFirstMappedPort());
        config.setDatabase(0);

        // Create connection factory
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        // Create RedisTemplate
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    private static List<RedisRateLimiter> createRateLimiterInstances() {
        InMemoryRateLimiter fallbackRateLimiter = new InMemoryRateLimiter();
        List<RedisRateLimiter> instances = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            instances.add(new RedisRateLimiter(redisTemplate, fallbackRateLimiter));
        }
        return instances;
    }

    private static List<RedisTokenBlacklist> createTokenBlacklistInstances() {
        TokenBlacklistService fallbackTokenBlacklist = new TokenBlacklistService();
        List<RedisTokenBlacklist> instances = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            instances.add(new RedisTokenBlacklist(redisTemplate, fallbackTokenBlacklist, jwtUtil));
        }
        return instances;
    }

    private static List<RedisIdempotencyService> createIdempotencyInstances() {
        IdempotencyService fallbackIdempotency = new IdempotencyService(null);
        List<RedisIdempotencyService> instances = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            instances.add(new RedisIdempotencyService(redisTemplate, fallbackIdempotency));
        }
        return instances;
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @AfterEach
    void cleanUp() {
        // Clean up Redis after each test
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        }
    }

    /**
     * Property 2.1: Rate Limiter State Sharing
     * 
     * When one instance increments a rate limit counter, all other instances
     * immediately see the updated count.
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 2.1: Rate Limiter Distributed State")
    void rateLimiter_stateIsSharedAcrossInstances(
            @ForAll @StringLength(min = 5, max = 50) String key,
            @ForAll @IntRange(min = 5, max = 20) int limit,
            @ForAll @IntRange(min = 10, max = 60) int windowSeconds) {

        // Instance 1 makes some requests
        RedisRateLimiter instance1 = rateLimiterInstances.get(0);
        RedisRateLimiter instance2 = rateLimiterInstances.get(1);
        RedisRateLimiter instance3 = rateLimiterInstances.get(2);

        int requestsFromInstance1 = Math.min(3, limit);
        for (int i = 0; i < requestsFromInstance1; i++) {
            instance1.isAllowed(key, limit, windowSeconds);
        }

        // All instances should see the same count
        int count1 = instance1.getCurrentCount(key, windowSeconds);
        int count2 = instance2.getCurrentCount(key, windowSeconds);
        int count3 = instance3.getCurrentCount(key, windowSeconds);

        assertThat(count1).isEqualTo(requestsFromInstance1);
        assertThat(count2).isEqualTo(requestsFromInstance1);
        assertThat(count3).isEqualTo(requestsFromInstance1);

        // Instance 2 makes more requests
        int requestsFromInstance2 = Math.min(2, limit - requestsFromInstance1);
        for (int i = 0; i < requestsFromInstance2; i++) {
            instance2.isAllowed(key, limit, windowSeconds);
        }

        // All instances should see the updated count
        int totalRequests = requestsFromInstance1 + requestsFromInstance2;
        assertThat(instance1.getCurrentCount(key, windowSeconds)).isEqualTo(totalRequests);
        assertThat(instance2.getCurrentCount(key, windowSeconds)).isEqualTo(totalRequests);
        assertThat(instance3.getCurrentCount(key, windowSeconds)).isEqualTo(totalRequests);

        // All instances should agree on remaining quota
        int expectedRemaining = Math.max(0, limit - totalRequests);
        assertThat(instance1.getRemainingQuota(key, limit, windowSeconds)).isEqualTo(expectedRemaining);
        assertThat(instance2.getRemainingQuota(key, limit, windowSeconds)).isEqualTo(expectedRemaining);
        assertThat(instance3.getRemainingQuota(key, limit, windowSeconds)).isEqualTo(expectedRemaining);
    }

    /**
     * Property 2.2: Token Blacklist State Sharing
     * 
     * When one instance blacklists a token, all other instances immediately
     * see the token as blacklisted.
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 2.2: Token Blacklist Distributed State")
    void tokenBlacklist_stateIsSharedAcrossInstances(
            @ForAll("validJwtTokens") String token) {

        RedisTokenBlacklist instance1 = tokenBlacklistInstances.get(0);
        RedisTokenBlacklist instance2 = tokenBlacklistInstances.get(1);
        RedisTokenBlacklist instance3 = tokenBlacklistInstances.get(2);

        // Initially, no instance should see the token as blacklisted
        assertThat(instance1.isBlacklisted(token)).isFalse();
        assertThat(instance2.isBlacklisted(token)).isFalse();
        assertThat(instance3.isBlacklisted(token)).isFalse();

        // Instance 1 blacklists the token
        instance1.blacklist(token);

        // All instances should immediately see the token as blacklisted
        assertThat(instance1.isBlacklisted(token)).isTrue();
        assertThat(instance2.isBlacklisted(token)).isTrue();
        assertThat(instance3.isBlacklisted(token)).isTrue();

        // Instance 2 removes the token from blacklist
        instance2.remove(token);

        // All instances should immediately see the token as not blacklisted
        assertThat(instance1.isBlacklisted(token)).isFalse();
        assertThat(instance2.isBlacklisted(token)).isFalse();
        assertThat(instance3.isBlacklisted(token)).isFalse();
    }

    /**
     * Property 2.3: Idempotency State Sharing
     * 
     * When one instance acquires an idempotency lock, all other instances
     * immediately see the key as taken and cannot acquire it.
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 2.3: Idempotency Distributed State")
    void idempotency_stateIsSharedAcrossInstances(
            @ForAll @StringLength(min = 10, max = 100) String idempotencyKey) {

        RedisIdempotencyService instance1 = idempotencyInstances.get(0);
        RedisIdempotencyService instance2 = idempotencyInstances.get(1);
        RedisIdempotencyService instance3 = idempotencyInstances.get(2);

        // Initially, no instance should see the key as existing
        assertThat(instance1.exists(idempotencyKey)).isFalse();
        assertThat(instance2.exists(idempotencyKey)).isFalse();
        assertThat(instance3.exists(idempotencyKey)).isFalse();

        // Instance 1 acquires the lock
        boolean acquired1 = instance1.tryAcquire(idempotencyKey);
        assertThat(acquired1).isTrue();

        // All instances should see the key as existing
        assertThat(instance1.exists(idempotencyKey)).isTrue();
        assertThat(instance2.exists(idempotencyKey)).isTrue();
        assertThat(instance3.exists(idempotencyKey)).isTrue();

        // Instance 2 and 3 should not be able to acquire the same lock
        boolean acquired2 = instance2.tryAcquire(idempotencyKey);
        boolean acquired3 = instance3.tryAcquire(idempotencyKey);
        assertThat(acquired2).isFalse();
        assertThat(acquired3).isFalse();

        // Instance 1 releases the lock
        instance1.release(idempotencyKey);

        // All instances should see the key as not existing
        assertThat(instance1.exists(idempotencyKey)).isFalse();
        assertThat(instance2.exists(idempotencyKey)).isFalse();
        assertThat(instance3.exists(idempotencyKey)).isFalse();

        // Now instance 2 should be able to acquire the lock
        boolean acquired2Again = instance2.tryAcquire(idempotencyKey);
        assertThat(acquired2Again).isTrue();
    }

    /**
     * Property 2.4: Concurrent State Updates
     * 
     * When multiple instances update state concurrently, all instances
     * eventually see consistent state (no lost updates).
     */
    @Property(tries = 20)
    @Label("Feature: architecture-refactoring, Property 2.4: Concurrent Distributed State Updates")
    void concurrent_stateUpdatesAreConsistent(
            @ForAll @StringLength(min = 5, max = 30) String baseKey,
            @ForAll @IntRange(min = 10, max = 50) int limit,
            @ForAll @IntRange(min = 30, max = 60) int windowSeconds) throws InterruptedException {

        RedisRateLimiter instance1 = rateLimiterInstances.get(0);
        RedisRateLimiter instance2 = rateLimiterInstances.get(1);
        RedisRateLimiter instance3 = rateLimiterInstances.get(2);

        // Each instance makes requests concurrently
        int requestsPerInstance = Math.min(5, limit / 3);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        executor.submit(() -> {
            for (int i = 0; i < requestsPerInstance; i++) {
                instance1.isAllowed(baseKey, limit, windowSeconds);
            }
            latch.countDown();
        });

        executor.submit(() -> {
            for (int i = 0; i < requestsPerInstance; i++) {
                instance2.isAllowed(baseKey, limit, windowSeconds);
            }
            latch.countDown();
        });

        executor.submit(() -> {
            for (int i = 0; i < requestsPerInstance; i++) {
                instance3.isAllowed(baseKey, limit, windowSeconds);
            }
            latch.countDown();
        });

        // Wait for all threads to complete
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // All instances should see the same total count
        int expectedTotal = requestsPerInstance * 3;
        int count1 = instance1.getCurrentCount(baseKey, windowSeconds);
        int count2 = instance2.getCurrentCount(baseKey, windowSeconds);
        int count3 = instance3.getCurrentCount(baseKey, windowSeconds);

        assertThat(count1).isEqualTo(expectedTotal);
        assertThat(count2).isEqualTo(expectedTotal);
        assertThat(count3).isEqualTo(expectedTotal);
    }

    /**
     * Property 2.5: State Visibility Without Delay
     * 
     * State changes are visible across instances immediately (within milliseconds),
     * not after some delay.
     */
    @Property(tries = 30)
    @Label("Feature: architecture-refactoring, Property 2.5: Immediate State Visibility")
    void stateChanges_areVisibleImmediately(
            @ForAll @StringLength(min = 10, max = 50) String idempotencyKey) {

        RedisIdempotencyService instance1 = idempotencyInstances.get(0);
        RedisIdempotencyService instance2 = idempotencyInstances.get(1);

        // Measure time for state to propagate
        long startTime = System.nanoTime();
        
        // Instance 1 acquires lock
        instance1.tryAcquire(idempotencyKey);
        
        // Instance 2 checks immediately
        boolean exists = instance2.exists(idempotencyKey);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // State should be visible
        assertThat(exists).isTrue();
        
        // State should be visible within 100ms (generous threshold for network latency)
        assertThat(durationMs).isLessThan(100);
    }

    /**
     * Property 2.6: Reset Operations Are Shared
     * 
     * When one instance resets state, all other instances see the reset.
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 2.6: Reset Operations Distributed State")
    void reset_operationsAreSharedAcrossInstances(
            @ForAll @StringLength(min = 5, max = 50) String key,
            @ForAll @IntRange(min = 10, max = 50) int limit,
            @ForAll @IntRange(min = 30, max = 60) int windowSeconds) {

        RedisRateLimiter instance1 = rateLimiterInstances.get(0);
        RedisRateLimiter instance2 = rateLimiterInstances.get(1);
        RedisRateLimiter instance3 = rateLimiterInstances.get(2);

        // Instance 1 makes some requests
        for (int i = 0; i < 5; i++) {
            instance1.isAllowed(key, limit, windowSeconds);
        }

        // All instances should see the count
        assertThat(instance1.getCurrentCount(key, windowSeconds)).isEqualTo(5);
        assertThat(instance2.getCurrentCount(key, windowSeconds)).isEqualTo(5);
        assertThat(instance3.getCurrentCount(key, windowSeconds)).isEqualTo(5);

        // Instance 2 resets the counter
        instance2.reset(key);

        // All instances should see the reset (count = 0)
        assertThat(instance1.getCurrentCount(key, windowSeconds)).isEqualTo(0);
        assertThat(instance2.getCurrentCount(key, windowSeconds)).isEqualTo(0);
        assertThat(instance3.getCurrentCount(key, windowSeconds)).isEqualTo(0);
    }

    /**
     * Provide valid JWT tokens for testing
     */
    @Provide
    Arbitrary<String> validJwtTokens() {
        return Arbitraries.longs()
                .between(1L, 1000L)
                .flatMap(userId -> 
                    Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(5)
                        .ofMaxLength(20)
                        .map(username -> jwtUtil.generateToken(userId, username, 1L))
                );
    }
}
