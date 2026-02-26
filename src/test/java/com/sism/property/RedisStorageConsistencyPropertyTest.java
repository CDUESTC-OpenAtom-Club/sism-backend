package com.sism.property;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Redis Storage Consistency
 * 
 * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
 * 
 * Tests verify that:
 * - Redis connection is established successfully
 * - Data can be stored and retrieved from Redis
 * - TTL (Time To Live) is set correctly
 * - Key format is correct for rate limiting, token blacklist, and idempotency
 * 
 * **Validates: Requirements 1.2, 1.3, 1.4**
 */
@Testcontainers
public class RedisStorageConsistencyPropertyTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    private static RedisTemplate<String, Object> redisTemplate;
    private static LettuceConnectionFactory connectionFactory;

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

        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        // Clean up Redis before each test
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        }
    }

    // ==================== Generators ====================

    /**
     * Generator for Redis keys with valid prefixes
     */
    @Provide
    Arbitrary<String> redisKeys() {
        return Arbitraries.oneOf(
                // Rate limit keys
                Arbitraries.strings().withCharRange('0', '9').ofLength(3)
                        .map(s -> "rate_limit:ip:192.168.1." + s),
                Arbitraries.integers().between(1, 10000)
                        .map(id -> "rate_limit:user:" + id),
                // Token blacklist keys
                Arbitraries.strings().alpha().ofLength(20)
                        .map(token -> "token_blacklist:" + token),
                // Idempotency keys
                Arbitraries.strings().alpha().numeric().ofLength(16)
                        .map(key -> "idempotency:" + key)
        );
    }

    /**
     * Generator for simple string values
     */
    @Provide
    Arbitrary<String> stringValues() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(100);
    }

    /**
     * Generator for integer values
     */
    @Provide
    Arbitrary<Integer> integerValues() {
        return Arbitraries.integers().between(1, 10000);
    }

    /**
     * Generator for TTL in seconds (1-3600)
     */
    @Provide
    Arbitrary<Integer> ttlSeconds() {
        return Arbitraries.integers().between(1, 3600);
    }

    // ==================== Property Tests ====================

    /**
     * Property 1.1: Redis connection should be established successfully
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * The system should successfully connect to Redis on startup.
     * 
     * **Validates: Requirements 1.1**
     */
    @Property(tries = 10)
    @Label("Feature: architecture-refactoring, Property 1.1: Redis Connection Established")
    void redisConnection_shouldBeEstablished() {
        // Verify RedisTemplate is created
        assertThat(redisTemplate)
                .as("RedisTemplate should be created")
                .isNotNull();

        // Verify connection factory is available
        assertThat(connectionFactory)
                .as("RedisConnectionFactory should be created")
                .isNotNull();

        // Verify connection is alive
        String pingResponse = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
        
        assertThat(pingResponse)
                .as("Redis should respond to PING with PONG")
                .isEqualTo("PONG");
    }

    /**
     * Property 1.2: String data should be stored and retrieved correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key-value pair, data stored in Redis should be retrievable.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.2: String Storage Consistency")
    void stringData_shouldBeStoredAndRetrieved(
            @ForAll("redisKeys") String key,
            @ForAll("stringValues") String value) {
        
        // Store value in Redis
        redisTemplate.opsForValue().set(key, value);
        
        // Retrieve value from Redis
        Object retrieved = redisTemplate.opsForValue().get(key);
        
        // Verify value matches
        assertThat(retrieved)
                .as("Retrieved value should match stored value")
                .isEqualTo(value);
    }

    /**
     * Property 1.3: Integer data should be stored and retrieved correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key-integer pair, data stored in Redis should be retrievable.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.3: Integer Storage Consistency")
    void integerData_shouldBeStoredAndRetrieved(
            @ForAll("redisKeys") String key,
            @ForAll("integerValues") Integer value) {
        
        // Store value in Redis
        redisTemplate.opsForValue().set(key, value);
        
        // Retrieve value from Redis
        Object retrieved = redisTemplate.opsForValue().get(key);
        
        // Verify value matches
        assertThat(retrieved)
                .as("Retrieved value should match stored value")
                .isEqualTo(value);
    }

    /**
     * Property 1.4: TTL should be set correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any key with TTL, the expiration time should be set correctly.
     * 
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.4: TTL Set Correctly")
    void ttl_shouldBeSetCorrectly(
            @ForAll("redisKeys") String key,
            @ForAll("stringValues") String value,
            @ForAll("ttlSeconds") int ttlSeconds) {
        
        // Store value with TTL
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        
        // Get TTL
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        
        // Verify TTL is set (allow 5 second tolerance for test execution time)
        assertThat(ttl)
                .as("TTL should be set correctly")
                .isNotNull()
                .isBetween((long) ttlSeconds - 5, (long) ttlSeconds + 1);
    }

    /**
     * Property 1.5: Rate limit key format should be correct
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * Rate limit keys should follow the format: rate_limit:{identifier}
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.5: Rate Limit Key Format")
    void rateLimitKey_shouldFollowCorrectFormat(
            @ForAll @StringLength(min = 1, max = 50) String identifier,
            @ForAll @IntRange(min = 1, max = 100) int count,
            @ForAll @IntRange(min = 1, max = 3600) int ttlSeconds) {
        
        String key = "rate_limit:" + identifier;
        
        // Store rate limit counter
        redisTemplate.opsForValue().set(key, count, Duration.ofSeconds(ttlSeconds));
        
        // Verify key exists
        Boolean exists = redisTemplate.hasKey(key);
        assertThat(exists)
                .as("Rate limit key should exist")
                .isTrue();
        
        // Verify value is correct
        Object retrieved = redisTemplate.opsForValue().get(key);
        assertThat(retrieved)
                .as("Rate limit counter should match")
                .isEqualTo(count);
        
        // Verify TTL is set
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl)
                .as("Rate limit TTL should be set")
                .isGreaterThan(0L);
    }

    /**
     * Property 1.6: Token blacklist key format should be correct
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * Token blacklist keys should follow the format: token_blacklist:{token}
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.6: Token Blacklist Key Format")
    void tokenBlacklistKey_shouldFollowCorrectFormat(
            @ForAll @StringLength(min = 10, max = 100) String token,
            @ForAll @IntRange(min = 60, max = 3600) int ttlSeconds) {
        
        String key = "token_blacklist:" + token;
        
        // Store token in blacklist
        redisTemplate.opsForValue().set(key, true, Duration.ofSeconds(ttlSeconds));
        
        // Verify key exists
        Boolean exists = redisTemplate.hasKey(key);
        assertThat(exists)
                .as("Token blacklist key should exist")
                .isTrue();
        
        // Verify value is true
        Object retrieved = redisTemplate.opsForValue().get(key);
        assertThat(retrieved)
                .as("Token blacklist value should be true")
                .isEqualTo(true);
        
        // Verify TTL is set
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl)
                .as("Token blacklist TTL should be set")
                .isGreaterThan(0L);
    }

    /**
     * Property 1.7: Idempotency key format should be correct
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * Idempotency keys should follow the format: idempotency:{idempotencyKey}
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.7: Idempotency Key Format")
    void idempotencyKey_shouldFollowCorrectFormat(
            @ForAll @StringLength(min = 10, max = 50) String idempotencyKey,
            @ForAll @IntRange(min = 60, max = 86400) int ttlSeconds) {
        
        String key = "idempotency:" + idempotencyKey;
        
        // Store idempotency record
        redisTemplate.opsForValue().set(key, true, Duration.ofSeconds(ttlSeconds));
        
        // Verify key exists
        Boolean exists = redisTemplate.hasKey(key);
        assertThat(exists)
                .as("Idempotency key should exist")
                .isTrue();
        
        // Verify value is true
        Object retrieved = redisTemplate.opsForValue().get(key);
        assertThat(retrieved)
                .as("Idempotency value should be true")
                .isEqualTo(true);
        
        // Verify TTL is set
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl)
                .as("Idempotency TTL should be set")
                .isGreaterThan(0L);
    }

    /**
     * Property 1.8: Increment operation should work correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * Increment operations should increase the counter correctly.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.8: Increment Operation")
    void increment_shouldWorkCorrectly(
            @ForAll("redisKeys") String key,
            @ForAll @IntRange(min = 1, max = 10) int incrementCount) {
        
        // Perform increments
        for (int i = 0; i < incrementCount; i++) {
            redisTemplate.opsForValue().increment(key);
        }
        
        // Verify final count
        Object retrieved = redisTemplate.opsForValue().get(key);
        assertThat(retrieved)
                .as("Counter should equal increment count")
                .isEqualTo(incrementCount);
    }

    /**
     * Property 1.9: Key deletion should work correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * Deleted keys should not be retrievable.
     * 
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.9: Key Deletion")
    void keyDeletion_shouldWorkCorrectly(
            @ForAll("redisKeys") String key,
            @ForAll("stringValues") String value) {
        
        // Store value
        redisTemplate.opsForValue().set(key, value);
        
        // Verify key exists
        Boolean existsBefore = redisTemplate.hasKey(key);
        assertThat(existsBefore)
                .as("Key should exist before deletion")
                .isTrue();
        
        // Delete key
        Boolean deleted = redisTemplate.delete(key);
        assertThat(deleted)
                .as("Delete operation should return true")
                .isTrue();
        
        // Verify key no longer exists
        Boolean existsAfter = redisTemplate.hasKey(key);
        assertThat(existsAfter)
                .as("Key should not exist after deletion")
                .isFalse();
        
        // Verify value is null
        Object retrieved = redisTemplate.opsForValue().get(key);
        assertThat(retrieved)
                .as("Retrieved value should be null after deletion")
                .isNull();
    }

    /**
     * Property 1.10: SetIfAbsent should work correctly for idempotency
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * SetIfAbsent should only set the value if the key doesn't exist.
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.10: SetIfAbsent for Idempotency")
    void setIfAbsent_shouldWorkCorrectlyForIdempotency(
            @ForAll @StringLength(min = 10, max = 50) String idempotencyKey,
            @ForAll @IntRange(min = 60, max = 86400) int ttlSeconds) {
        
        String key = "idempotency:" + idempotencyKey;
        
        // First setIfAbsent should succeed
        Boolean firstSet = redisTemplate.opsForValue()
                .setIfAbsent(key, true, Duration.ofSeconds(ttlSeconds));
        assertThat(firstSet)
                .as("First setIfAbsent should succeed")
                .isTrue();
        
        // Second setIfAbsent should fail (key already exists)
        Boolean secondSet = redisTemplate.opsForValue()
                .setIfAbsent(key, true, Duration.ofSeconds(ttlSeconds));
        assertThat(secondSet)
                .as("Second setIfAbsent should fail")
                .isFalse();
        
        // Verify key still exists
        Boolean exists = redisTemplate.hasKey(key);
        assertThat(exists)
                .as("Key should still exist")
                .isTrue();
    }
}
