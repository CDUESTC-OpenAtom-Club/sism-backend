package com.sism.property;

import com.sism.service.RedisTokenBlacklist;
import com.sism.util.JwtUtil;
import com.sism.util.TokenBlacklistService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
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

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for RedisTokenBlacklist
 * 
 * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
 * 
 * Tests verify that:
 * - Tokens can be added to blacklist
 * - Blacklisted tokens are correctly identified
 * - TTL is set based on token expiration
 * - Tokens are automatically removed after expiration
 * - Fallback mechanism works when Redis fails
 * - Distributed token blacklist across multiple instances
 * 
 * **Validates: Requirements 1.3**
 */
@Testcontainers
public class RedisTokenBlacklistPropertyTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    private static RedisTemplate<String, Object> redisTemplate;
    private static LettuceConnectionFactory connectionFactory;
    private static RedisTokenBlacklist tokenBlacklist;
    private static TokenBlacklistService fallbackService;
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

        // Create JwtUtil for token generation
        jwtUtil = new JwtUtil();
        // Use reflection to set the secret and expiration
        try {
            var secretField = JwtUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(jwtUtil, "test-secret-key-for-property-based-testing-minimum-256-bits");
            
            var expirationField = JwtUtil.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(jwtUtil, 3600000L); // 1 hour
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JwtUtil", e);
        }

        // Create fallback service
        fallbackService = new TokenBlacklistService();

        // Create token blacklist
        tokenBlacklist = new RedisTokenBlacklist(redisTemplate, fallbackService, jwtUtil);
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
        // Reset metrics
        if (tokenBlacklist != null) {
            tokenBlacklist.resetMetrics();
        }
        if (fallbackService != null) {
            fallbackService.clear();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<String> usernames() {
        return Arbitraries.strings().alpha().ofLength(10);
    }

    // ==================== Property Tests ====================

    /**
     * Property 1.1: Tokens can be added to blacklist
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any valid token, after adding it to the blacklist, it should be retrievable.
     * 
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.1: Tokens Can Be Added to Blacklist")
    void tokens_canBeAddedToBlacklist(
            @ForAll("userIds") long userId,
            @ForAll("usernames") String username) {
        
        // Generate token
        String token = jwtUtil.generateToken(userId, username, 1L);
        
        // Add token to blacklist
        tokenBlacklist.blacklist(token);
        
        // Verify token is blacklisted
        boolean isBlacklisted = tokenBlacklist.isBlacklisted(token);
        assertThat(isBlacklisted)
                .as("Token should be blacklisted after adding")
                .isTrue();
    }

    /**
     * Property 1.2: Blacklisted tokens are correctly identified
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any token, isBlacklisted should return true only if the token was added.
     * 
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.2: Blacklisted Tokens Correctly Identified")
    void blacklistedTokens_areCorrectlyIdentified(
            @ForAll("userIds") long userId1,
            @ForAll("usernames") String username1,
            @ForAll("userIds") long userId2,
            @ForAll("usernames") String username2) {
        
        // Generate two different tokens
        String blacklistedToken = jwtUtil.generateToken(userId1, username1, 1L);
        String nonBlacklistedToken = jwtUtil.generateToken(userId2, username2, 1L);
        
        Assume.that(!blacklistedToken.equals(nonBlacklistedToken));
        
        // Add only one token to blacklist
        tokenBlacklist.blacklist(blacklistedToken);
        
        // Verify blacklisted token is identified
        assertThat(tokenBlacklist.isBlacklisted(blacklistedToken))
                .as("Blacklisted token should return true")
                .isTrue();
        
        // Verify non-blacklisted token is not identified
        assertThat(tokenBlacklist.isBlacklisted(nonBlacklistedToken))
                .as("Non-blacklisted token should return false")
                .isFalse();
    }

    /**
     * Property 1.3: TTL is set based on token expiration
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any token, the Redis TTL should match the token's expiration time.
     * 
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.3: TTL Set Based on Token Expiration")
    void ttl_isSetBasedOnTokenExpiration(
            @ForAll("userIds") long userId,
            @ForAll("usernames") String username) {
        
        // Generate token
        String token = jwtUtil.generateToken(userId, username, 1L);
        
        // Get token expiration
        Date expiration = jwtUtil.extractExpiration(token);
        Date now = new Date();
        long expectedTtlSeconds = (expiration.getTime() - now.getTime()) / 1000;
        
        // Add token to blacklist
        tokenBlacklist.blacklist(token);
        
        // Verify TTL is set correctly (allow 5 second tolerance)
        String redisKey = "token_blacklist:" + token;
        Long actualTtl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        
        assertThat(actualTtl)
                .as("TTL should match token expiration time")
                .isBetween(expectedTtlSeconds - 5, expectedTtlSeconds + 5);
    }

    /**
     * Property 1.4: Tokens are automatically removed after expiration
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any token with short TTL, after expiration, it should not be in blacklist.
     * 
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 20)
    @Label("Feature: architecture-refactoring, Property 1.4: Tokens Removed After Expiration")
    void tokens_areRemovedAfterExpiration(
            @ForAll @LongRange(min = 1, max = 1000) long userId,
            @ForAll @StringLength(min = 5, max = 20) String username) throws InterruptedException {
        
        // Create token with short expiration (2 seconds)
        JwtUtil shortExpirationJwtUtil = new JwtUtil();
        try {
            var secretField = JwtUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(shortExpirationJwtUtil, "test-secret-key-for-property-based-testing-minimum-256-bits");
            
            var expirationField = JwtUtil.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(shortExpirationJwtUtil, 2000L); // 2 seconds
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JwtUtil", e);
        }
        
        String token = shortExpirationJwtUtil.generateToken(userId, username, 1L);
        
        // Create blacklist with short expiration JwtUtil
        RedisTokenBlacklist shortTtlBlacklist = new RedisTokenBlacklist(
                redisTemplate, fallbackService, shortExpirationJwtUtil);
        
        // Add token to blacklist
        shortTtlBlacklist.blacklist(token);
        
        // Verify token is blacklisted
        assertThat(shortTtlBlacklist.isBlacklisted(token))
                .as("Token should be blacklisted initially")
                .isTrue();
        
        // Wait for token to expire (add buffer)
        Thread.sleep(3000);
        
        // Verify token is no longer blacklisted
        assertThat(shortTtlBlacklist.isBlacklisted(token))
                .as("Token should not be blacklisted after expiration")
                .isFalse();
    }

    /**
     * Property 1.5: Fallback mechanism works when Redis fails
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any token, when Redis is unavailable, fallback service should be used.
     * 
     * **Validates: Requirements 1.3, 1.5**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.5: Fallback Mechanism Works")
    void fallback_worksWhenRedisFails(
            @ForAll("userIds") long userId,
            @ForAll("usernames") String username) {
        
        // Generate token
        String token = jwtUtil.generateToken(userId, username, 1L);
        
        // Stop Redis container to simulate failure
        redis.stop();
        
        try {
            // Add token to blacklist (should fallback)
            tokenBlacklist.blacklist(token);
            
            // Verify token is blacklisted via fallback
            boolean isBlacklisted = tokenBlacklist.isBlacklisted(token);
            assertThat(isBlacklisted)
                    .as("Token should be blacklisted via fallback")
                    .isTrue();
            
            // Verify fallback metrics
            var metrics = tokenBlacklist.getFallbackMetrics();
            long fallbackCount = (long) metrics.get("fallbackCount");
            assertThat(fallbackCount)
                    .as("Fallback should be triggered")
                    .isGreaterThan(0);
            
        } finally {
            // Restart Redis for next test
            redis.start();
            // Wait for Redis to be ready
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Property 1.6: Distributed token blacklist across multiple instances
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any token, multiple instances should share the same blacklist.
     * 
     * **Validates: Requirements 1.3, 1.7**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.6: Distributed Blacklist Across Instances")
    void distributedBlacklist_worksAcrossInstances(
            @ForAll("userIds") long userId,
            @ForAll("usernames") String username) {
        
        // Generate token
        String token = jwtUtil.generateToken(userId, username, 1L);
        
        // Create second instance (simulating another app instance)
        TokenBlacklistService fallbackService2 = new TokenBlacklistService();
        RedisTokenBlacklist tokenBlacklist2 = new RedisTokenBlacklist(
                redisTemplate, fallbackService2, jwtUtil);
        
        // Instance 1 adds token to blacklist
        tokenBlacklist.blacklist(token);
        
        // Instance 2 should see the blacklisted token
        boolean isBlacklistedInInstance2 = tokenBlacklist2.isBlacklisted(token);
        assertThat(isBlacklistedInInstance2)
                .as("Instance 2 should see token blacklisted by Instance 1")
                .isTrue();
        
        // Instance 2 removes token
        tokenBlacklist2.remove(token);
        
        // Instance 1 should see token removed
        boolean isBlacklistedInInstance1 = tokenBlacklist.isBlacklisted(token);
        assertThat(isBlacklistedInInstance1)
                .as("Instance 1 should see token removed by Instance 2")
                .isFalse();
    }

    /**
     * Property 1.7: Concurrent blacklist operations are handled correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any set of tokens, concurrent blacklist operations should work correctly.
     * 
     * **Validates: Requirements 1.3, 1.7**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.7: Concurrent Operations Handled Correctly")
    void concurrentOperations_areHandledCorrectly(
            @ForAll @IntRange(min = 5, max = 20) int threadCount) throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Generate unique tokens for each thread
        String[] tokens = new String[threadCount];
        for (int i = 0; i < threadCount; i++) {
            tokens[i] = jwtUtil.generateToken((long) i, "user" + i, 1L);
        }
        
        try {
            // Submit concurrent blacklist operations
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        tokenBlacklist.blacklist(tokens[index]);
                        if (tokenBlacklist.isBlacklisted(tokens[index])) {
                            successCount.incrementAndGet();
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
            
            // Verify all tokens were blacklisted
            assertThat(successCount.get())
                    .as("All tokens should be blacklisted")
                    .isEqualTo(threadCount);
            
            // Verify each token individually
            for (String token : tokens) {
                assertThat(tokenBlacklist.isBlacklisted(token))
                        .as("Token should be blacklisted")
                        .isTrue();
            }
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Property 1.8: Invalid tokens are handled gracefully
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any invalid token, operations should not throw exceptions.
     * 
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.8: Invalid Tokens Handled Gracefully")
    void invalidTokens_areHandledGracefully() {
        // Null token
        tokenBlacklist.blacklist(null);
        assertThat(tokenBlacklist.isBlacklisted(null))
                .as("Null token should return false")
                .isFalse();
        
        // Empty token
        tokenBlacklist.blacklist("");
        assertThat(tokenBlacklist.isBlacklisted(""))
                .as("Empty token should return false")
                .isFalse();
        
        // Invalid token format
        String invalidToken = "not-a-valid-jwt-token";
        // Should not throw exception
        try {
            tokenBlacklist.blacklist(invalidToken);
            // If it doesn't throw, that's acceptable
        } catch (Exception e) {
            // If it throws, verify it's handled gracefully
            assertThat(e)
                    .as("Exception should be handled gracefully")
                    .isNotInstanceOf(NullPointerException.class);
        }
    }

    /**
     * Property 1.9: Clear operation removes all tokens
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any set of tokens, clear should remove all of them.
     * 
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 50)
    @Label("Feature: architecture-refactoring, Property 1.9: Clear Removes All Tokens")
    void clear_removesAllTokens(@ForAll @IntRange(min = 3, max = 10) int tokenCount) {
        // Generate and blacklist multiple tokens
        String[] tokens = new String[tokenCount];
        for (int i = 0; i < tokenCount; i++) {
            tokens[i] = jwtUtil.generateToken((long) i, "user" + i, 1L);
            tokenBlacklist.blacklist(tokens[i]);
        }
        
        // Verify all tokens are blacklisted
        for (String token : tokens) {
            assertThat(tokenBlacklist.isBlacklisted(token))
                    .as("Token should be blacklisted before clear")
                    .isTrue();
        }
        
        // Clear all tokens
        tokenBlacklist.clear();
        
        // Verify all tokens are removed
        for (String token : tokens) {
            assertThat(tokenBlacklist.isBlacklisted(token))
                    .as("Token should not be blacklisted after clear")
                    .isFalse();
        }
    }

    /**
     * Property 1.10: Remove operation works correctly
     * 
     * **Feature: architecture-refactoring, Property 1: Redis Storage Consistency**
     * 
     * For any blacklisted token, remove should make it non-blacklisted.
     * 
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    @Label("Feature: architecture-refactoring, Property 1.10: Remove Operation Works Correctly")
    void remove_worksCorrectly(
            @ForAll("userIds") long userId,
            @ForAll("usernames") String username) {
        
        // Generate token
        String token = jwtUtil.generateToken(userId, username, 1L);
        
        // Add token to blacklist
        tokenBlacklist.blacklist(token);
        
        // Verify token is blacklisted
        assertThat(tokenBlacklist.isBlacklisted(token))
                .as("Token should be blacklisted after adding")
                .isTrue();
        
        // Remove token
        tokenBlacklist.remove(token);
        
        // Verify token is no longer blacklisted
        assertThat(tokenBlacklist.isBlacklisted(token))
                .as("Token should not be blacklisted after removal")
                .isFalse();
    }
}
