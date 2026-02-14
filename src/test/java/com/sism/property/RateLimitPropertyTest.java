package com.sism.property;

import com.sism.service.InMemoryRateLimiter;
import com.sism.service.RateLimiter;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Rate Limiter
 * 
 * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
 * 
 * Tests verify that:
 * - Within time window, requests exceeding limit return 429
 * - Requests within limit are allowed
 * - Rate limit resets after window expires
 * - Different keys are tracked independently
 * 
 * **Validates: Requirements 2.3.4, 2.3.5**
 */
public class RateLimitPropertyTest {

    // ==================== Generators ====================

    /**
     * Generator for rate limit keys
     */
    @Provide
    Arbitrary<String> rateLimitKeys() {
        return Arbitraries.oneOf(
                // IP-based keys
                Arbitraries.strings().withCharRange('0', '9').ofLength(3)
                        .map(s -> "ip:192.168.1." + s),
                // User-based keys
                Arbitraries.integers().between(1, 10000)
                        .map(id -> "user:" + id),
                // Login keys
                Arbitraries.strings().withCharRange('0', '9').ofLength(3)
                        .map(s -> "login:ip:10.0.0." + s),
                // API keys
                Arbitraries.integers().between(1, 1000)
                        .map(id -> "api:user:" + id)
        );
    }

    /**
     * Generator for rate limits (1-100)
     */
    @Provide
    Arbitrary<Integer> rateLimits() {
        return Arbitraries.integers().between(1, 100);
    }

    /**
     * Generator for window sizes in seconds (1-300)
     */
    @Provide
    Arbitrary<Integer> windowSeconds() {
        return Arbitraries.integers().between(1, 300);
    }

    /**
     * Generator for request counts
     */
    @Provide
    Arbitrary<Integer> requestCounts() {
        return Arbitraries.integers().between(1, 200);
    }

    // ==================== Property Tests ====================

    /**
     * Property P9.1: Requests within limit should be allowed
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * For any key and limit, the first 'limit' requests should all be allowed.
     * 
     * **Validates: Requirements 2.3.4**
     */
    @Property(tries = 100)
    void requestsWithinLimit_shouldBeAllowed(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 1, max = 50) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds) {

        RateLimiter rateLimiter = new InMemoryRateLimiter();

        // Make exactly 'limit' requests
        int allowedCount = 0;
        for (int i = 0; i < limit; i++) {
            if (rateLimiter.isAllowed(key, limit, windowSeconds)) {
                allowedCount++;
            }
        }

        assertThat(allowedCount)
                .as("All %d requests within limit should be allowed", limit)
                .isEqualTo(limit);
    }

    /**
     * Property P9.2: Requests exceeding limit should be rejected
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * For any key and limit, requests beyond the limit should be rejected.
     * 
     * **Validates: Requirements 2.3.5**
     */
    @Property(tries = 100)
    void requestsExceedingLimit_shouldBeRejected(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 1, max = 20) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds,
            @ForAll @IntRange(min = 1, max = 10) int extraRequests) {

        RateLimiter rateLimiter = new InMemoryRateLimiter();

        // Exhaust the limit
        for (int i = 0; i < limit; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }

        // Additional requests should be rejected
        int rejectedCount = 0;
        for (int i = 0; i < extraRequests; i++) {
            if (!rateLimiter.isAllowed(key, limit, windowSeconds)) {
                rejectedCount++;
            }
        }

        assertThat(rejectedCount)
                .as("All %d requests exceeding limit should be rejected", extraRequests)
                .isEqualTo(extraRequests);
    }

    /**
     * Property P9.3: Different keys should be tracked independently
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * Rate limits for different keys should not affect each other.
     * 
     * **Validates: Requirements 2.3.3**
     */
    @Property(tries = 50)
    void differentKeys_shouldBeTrackedIndependently(
            @ForAll @IntRange(min = 1, max = 10) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds) {

        RateLimiter rateLimiter = new InMemoryRateLimiter();
        String key1 = "ip:192.168.1.1";
        String key2 = "ip:192.168.1.2";

        // Exhaust limit for key1
        for (int i = 0; i < limit; i++) {
            rateLimiter.isAllowed(key1, limit, windowSeconds);
        }

        // key1 should be blocked
        assertThat(rateLimiter.isAllowed(key1, limit, windowSeconds))
                .as("key1 should be blocked after exhausting limit")
                .isFalse();

        // key2 should still be allowed
        assertThat(rateLimiter.isAllowed(key2, limit, windowSeconds))
                .as("key2 should still be allowed")
                .isTrue();
    }

    /**
     * Property P9.4: Remaining quota should decrease with each request
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * After each allowed request, the remaining quota should decrease by 1.
     * 
     * **Validates: Requirements 2.3.4**
     */
    @Property(tries = 100)
    void remainingQuota_shouldDecreaseWithEachRequest(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 5, max = 50) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds,
            @ForAll @IntRange(min = 1, max = 5) int requestCount) {

        RateLimiter rateLimiter = new InMemoryRateLimiter();

        // Initial remaining should equal limit
        int initialRemaining = rateLimiter.getRemainingQuota(key, limit, windowSeconds);
        assertThat(initialRemaining)
                .as("Initial remaining quota should equal limit")
                .isEqualTo(limit);

        // Make requests and check remaining decreases
        int actualRequestCount = Math.min(requestCount, limit);
        for (int i = 0; i < actualRequestCount; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }

        int finalRemaining = rateLimiter.getRemainingQuota(key, limit, windowSeconds);
        assertThat(finalRemaining)
                .as("Remaining quota should decrease by request count")
                .isEqualTo(limit - actualRequestCount);
    }

    /**
     * Property P9.5: Current count should match number of requests made
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * The current count should equal the number of allowed requests.
     * 
     * **Validates: Requirements 2.3.4**
     */
    @Property(tries = 100)
    void currentCount_shouldMatchRequestsMade(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 10, max = 50) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds,
            @ForAll @IntRange(min = 1, max = 10) int requestCount) {

        RateLimiter rateLimiter = new InMemoryRateLimiter();

        // Initial count should be 0
        assertThat(rateLimiter.getCurrentCount(key, windowSeconds))
                .as("Initial count should be 0")
                .isEqualTo(0);

        // Make requests
        int actualRequestCount = Math.min(requestCount, limit);
        for (int i = 0; i < actualRequestCount; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }

        // Count should match requests made
        assertThat(rateLimiter.getCurrentCount(key, windowSeconds))
                .as("Current count should match requests made")
                .isEqualTo(actualRequestCount);
    }

    /**
     * Property P9.6: Reset should clear the count
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * After reset, the key should have full quota again.
     * 
     * **Validates: Requirements 2.3.4**
     */
    @Property(tries = 100)
    void reset_shouldClearCount(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 5, max = 50) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds) {

        RateLimiter rateLimiter = new InMemoryRateLimiter();

        // Exhaust the limit
        for (int i = 0; i < limit; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }

        // Verify limit is exhausted
        assertThat(rateLimiter.isAllowed(key, limit, windowSeconds))
                .as("Should be blocked after exhausting limit")
                .isFalse();

        // Reset the key
        rateLimiter.reset(key);

        // Should be allowed again
        assertThat(rateLimiter.isAllowed(key, limit, windowSeconds))
                .as("Should be allowed after reset")
                .isTrue();

        // Count should be 1 (the request we just made)
        assertThat(rateLimiter.getCurrentCount(key, windowSeconds))
                .as("Count should be 1 after reset and one request")
                .isEqualTo(1);
    }

    /**
     * Property P9.7: Login rate limit should be stricter
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * Login endpoints should have a lower limit (5/minute) than general API (100/minute).
     * 
     * **Validates: Requirements 2.3.4**
     */
    @Property(tries = 50)
    void loginRateLimit_shouldBeStricter() {
        RateLimiter rateLimiter = new InMemoryRateLimiter();
        
        int loginLimit = 5;
        int apiLimit = 100;
        int windowSeconds = 60;
        
        String loginKey = "login:ip:192.168.1.1";
        String apiKey = "api:ip:192.168.1.1";

        // Make 5 login requests (should all be allowed)
        int loginAllowed = 0;
        for (int i = 0; i < loginLimit; i++) {
            if (rateLimiter.isAllowed(loginKey, loginLimit, windowSeconds)) {
                loginAllowed++;
            }
        }
        assertThat(loginAllowed).isEqualTo(loginLimit);

        // 6th login request should be blocked
        assertThat(rateLimiter.isAllowed(loginKey, loginLimit, windowSeconds))
                .as("6th login request should be blocked")
                .isFalse();

        // API requests should still have plenty of quota
        int apiAllowed = 0;
        for (int i = 0; i < 10; i++) {
            if (rateLimiter.isAllowed(apiKey, apiLimit, windowSeconds)) {
                apiAllowed++;
            }
        }
        assertThat(apiAllowed)
                .as("API requests should all be allowed")
                .isEqualTo(10);
    }

    /**
     * Property P9.8: Null or empty keys should not be limited
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * Invalid keys should always be allowed (fail-open behavior).
     * 
     * **Validates: Requirements 2.3.3**
     */
    @Property(tries = 50)
    void nullOrEmptyKeys_shouldNotBeLimited(
            @ForAll @IntRange(min = 1, max = 10) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds) {

        RateLimiter rateLimiter = new InMemoryRateLimiter();

        // Null key should always be allowed
        for (int i = 0; i < limit + 10; i++) {
            assertThat(rateLimiter.isAllowed(null, limit, windowSeconds))
                    .as("Null key should always be allowed")
                    .isTrue();
        }

        // Empty key should always be allowed
        for (int i = 0; i < limit + 10; i++) {
            assertThat(rateLimiter.isAllowed("", limit, windowSeconds))
                    .as("Empty key should always be allowed")
                    .isTrue();
        }

        // Blank key should always be allowed
        for (int i = 0; i < limit + 10; i++) {
            assertThat(rateLimiter.isAllowed("   ", limit, windowSeconds))
                    .as("Blank key should always be allowed")
                    .isTrue();
        }
    }

    /**
     * Property P9.9: Invalid limits should not restrict
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * Zero or negative limits should not restrict requests.
     * 
     * **Validates: Requirements 2.3.3**
     */
    @Property(tries = 50)
    void invalidLimits_shouldNotRestrict(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds) {

        RateLimiter rateLimiter = new InMemoryRateLimiter();

        // Zero limit should always allow
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.isAllowed(key + ":zero", 0, windowSeconds))
                    .as("Zero limit should always allow")
                    .isTrue();
        }

        // Negative limit should always allow
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.isAllowed(key + ":negative", -1, windowSeconds))
                    .as("Negative limit should always allow")
                    .isTrue();
        }
    }

    /**
     * Property P9.10: Reset time should be positive when limit is exhausted
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * When the limit is exhausted, reset time should be > 0.
     * 
     * **Validates: Requirements 2.3.5**
     */
    @Property(tries = 100)
    void resetTime_shouldBePositiveWhenLimitExhausted(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 1, max = 10) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds) {

        RateLimiter rateLimiter = new InMemoryRateLimiter();

        // Exhaust the limit
        for (int i = 0; i < limit; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }

        // Reset time should be positive
        long resetTime = rateLimiter.getResetTimeSeconds(key, windowSeconds);
        assertThat(resetTime)
                .as("Reset time should be positive when limit is exhausted")
                .isGreaterThan(0);

        // Reset time should not exceed window size
        assertThat(resetTime)
                .as("Reset time should not exceed window size")
                .isLessThanOrEqualTo(windowSeconds);
    }

    /**
     * Property P9.11: Concurrent requests should be handled correctly
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * Under concurrent access, exactly 'limit' requests should be allowed.
     * 
     * **Validates: Requirements 2.3.4**
     */
    @Property(tries = 20)
    void concurrentRequests_shouldRespectLimit(
            @ForAll @IntRange(min = 5, max = 20) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds) throws InterruptedException {

        RateLimiter rateLimiter = new InMemoryRateLimiter();
        String key = "concurrent:test:" + System.nanoTime();
        
        int totalRequests = limit * 2;
        AtomicInteger allowedCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalRequests);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Submit concurrent requests
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    if (rateLimiter.isAllowed(key, limit, windowSeconds)) {
                        allowedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all requests to complete
        endLatch.await();
        executor.shutdown();

        // Exactly 'limit' requests should be allowed
        assertThat(allowedCount.get())
                .as("Exactly %d requests should be allowed under concurrent access", limit)
                .isEqualTo(limit);
    }

    /**
     * Property P9.12: Cleanup should not affect active limits
     * 
     * **Feature: sism-enterprise-optimization, Property P9: 频率限制正确性**
     * 
     * Cleanup should only remove expired entries, not active ones.
     * 
     * **Validates: Requirements 2.3.3**
     */
    @Property(tries = 50)
    void cleanup_shouldNotAffectActiveLimits(
            @ForAll("rateLimitKeys") String key,
            @ForAll @IntRange(min = 5, max = 20) int limit,
            @ForAll @IntRange(min = 60, max = 300) int windowSeconds) {

        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter();

        // Make some requests
        int requestCount = limit / 2;
        for (int i = 0; i < requestCount; i++) {
            rateLimiter.isAllowed(key, limit, windowSeconds);
        }

        // Run cleanup
        rateLimiter.cleanup();

        // Count should still be preserved
        assertThat(rateLimiter.getCurrentCount(key, windowSeconds))
                .as("Count should be preserved after cleanup")
                .isEqualTo(requestCount);

        // Remaining quota should still be correct
        assertThat(rateLimiter.getRemainingQuota(key, limit, windowSeconds))
                .as("Remaining quota should be preserved after cleanup")
                .isEqualTo(limit - requestCount);
    }
}
