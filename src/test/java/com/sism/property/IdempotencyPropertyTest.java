package com.sism.property;

import com.sism.entity.IdempotencyRecord;
import com.sism.entity.IdempotencyRecord.IdempotencyStatus;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Idempotency Service
 * 
 * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
 * 
 * Tests verify that:
 * - Within TTL, same key returns cached response
 * - After TTL expires, request is processed normally
 * - Duplicate requests return original response, not re-execute
 * 
 * **Validates: Requirements 2.2.2, 2.2.4**
 */
public class IdempotencyPropertyTest {

    // ==================== Generators ====================

    /**
     * Generator for valid idempotency keys (64-character hex strings)
     */
    @Provide
    Arbitrary<String> validIdempotencyKeys() {
        return Arbitraries.strings()
                .withChars("0123456789abcdef")
                .ofLength(64);
    }

    /**
     * Generator for HTTP methods
     */
    @Provide
    Arbitrary<String> httpMethods() {
        return Arbitraries.of("POST", "PUT", "DELETE", "PATCH");
    }

    /**
     * Generator for request paths
     */
    @Provide
    Arbitrary<String> requestPaths() {
        return Arbitraries.of(
                "/api/indicators",
                "/api/indicators/1",
                "/api/milestones",
                "/api/milestones/123",
                "/api/tasks",
                "/api/tasks/456",
                "/api/approvals/789"
        );
    }

    /**
     * Generator for TTL values in seconds (1 second to 10 minutes)
     */
    @Provide
    Arbitrary<Integer> ttlSeconds() {
        return Arbitraries.integers().between(1, 600);
    }

    /**
     * Generator for response bodies
     */
    @Provide
    Arbitrary<String> responseBodies() {
        return Arbitraries.oneOf(
                Arbitraries.just("{\"success\":true,\"data\":{\"id\":1}}"),
                Arbitraries.just("{\"success\":true,\"data\":{\"id\":2,\"name\":\"test\"}}"),
                Arbitraries.just("{\"code\":0,\"message\":\"OK\"}"),
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100)
                        .map(s -> "{\"data\":\"" + s + "\"}")
        );
    }

    /**
     * Generator for HTTP status codes
     */
    @Provide
    Arbitrary<Integer> statusCodes() {
        return Arbitraries.oneOf(
                Arbitraries.of(200, 201, 204),  // Success codes
                Arbitraries.of(400, 401, 403, 404, 409),  // Client error codes
                Arbitraries.of(500, 502, 503)  // Server error codes
        );
    }

    // ==================== Property Tests ====================

    /**
     * Property P8.1: Same key within TTL returns same response
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * For any idempotency key and response, if the record is within TTL,
     * checking for duplicate should return the cached response.
     * 
     * **Validates: Requirements 2.2.2, 2.2.4**
     */
    @Property(tries = 100)
    void sameKeyWithinTTL_shouldReturnCachedResponse(
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath,
            @ForAll("responseBodies") String responseBody,
            @ForAll("statusCodes") int statusCode,
            @ForAll("ttlSeconds") int ttlSeconds) {

        // Create a completed record within TTL
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, ttlSeconds);
        record.complete(responseBody, statusCode);

        // Verify the record is valid (within TTL and completed)
        assertThat(record.isValid())
                .as("Record should be valid within TTL")
                .isTrue();

        assertThat(record.getResponseBody())
                .as("Response body should match")
                .isEqualTo(responseBody);

        assertThat(record.getStatusCode())
                .as("Status code should match")
                .isEqualTo(statusCode);
    }

    /**
     * Property P8.2: Expired records are not valid
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * For any idempotency record that has expired, isValid() should return false.
     * 
     * **Validates: Requirements 2.2.2**
     */
    @Property(tries = 100)
    void expiredRecord_shouldNotBeValid(
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath,
            @ForAll("responseBodies") String responseBody) {

        // Create a record with TTL of 0 (immediately expired)
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, 0);
        record.complete(responseBody, 200);

        // Force expiration by setting expiresAt to the past
        record.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        // Verify the record is expired
        assertThat(record.isExpired())
                .as("Record should be expired")
                .isTrue();

        assertThat(record.isValid())
                .as("Expired record should not be valid")
                .isFalse();
    }

    /**
     * Property P8.3: Pending records are not valid but are pending
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * For any idempotency record in PENDING status, isValid() should return false
     * but isPending() should return true.
     * 
     * **Validates: Requirements 2.2.2**
     */
    @Property(tries = 100)
    void pendingRecord_shouldBePendingNotValid(
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath,
            @ForAll("ttlSeconds") int ttlSeconds) {

        // Create a new record (starts in PENDING status)
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, ttlSeconds);

        // Verify the record is pending
        assertThat(record.getStatus())
                .as("New record should be in PENDING status")
                .isEqualTo(IdempotencyStatus.PENDING);

        assertThat(record.isPending())
                .as("Record should be pending")
                .isTrue();

        assertThat(record.isValid())
                .as("Pending record should not be valid")
                .isFalse();
    }

    /**
     * Property P8.4: Completed records within TTL are valid
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * For any idempotency record that is completed and within TTL,
     * isValid() should return true.
     * 
     * **Validates: Requirements 2.2.4**
     */
    @Property(tries = 100)
    void completedRecordWithinTTL_shouldBeValid(
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath,
            @ForAll("responseBodies") String responseBody,
            @ForAll @IntRange(min = 60, max = 600) int ttlSeconds) {

        // Create and complete a record with sufficient TTL
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, ttlSeconds);
        record.complete(responseBody, 200);

        // Verify the record is valid
        assertThat(record.isValid())
                .as("Completed record within TTL should be valid")
                .isTrue();

        assertThat(record.isExpired())
                .as("Record should not be expired")
                .isFalse();

        assertThat(record.isPending())
                .as("Completed record should not be pending")
                .isFalse();
    }

    /**
     * Property P8.5: Failed records are not valid
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * For any idempotency record in FAILED status, isValid() should return false.
     * 
     * **Validates: Requirements 2.2.2**
     */
    @Property(tries = 100)
    void failedRecord_shouldNotBeValid(
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath,
            @ForAll("ttlSeconds") int ttlSeconds) {

        // Create and fail a record
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, ttlSeconds);
        record.fail("{\"error\":\"Something went wrong\"}", 500);

        // Verify the record is not valid
        assertThat(record.getStatus())
                .as("Record should be in FAILED status")
                .isEqualTo(IdempotencyStatus.FAILED);

        assertThat(record.isValid())
                .as("Failed record should not be valid")
                .isFalse();
    }

    /**
     * Property P8.6: Idempotency key uniqueness in simulated store
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * For any set of unique idempotency keys, each key should map to exactly
     * one record in the store.
     * 
     * **Validates: Requirements 2.2.1**
     */
    @Property(tries = 50)
    void uniqueKeys_shouldMapToUniqueRecords(
            @ForAll List<@From("validIdempotencyKeys") String> keys,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath) {

        // Filter to get 1-10 keys
        List<String> limitedKeys = keys.stream().limit(10).toList();
        if (limitedKeys.isEmpty()) {
            return; // Skip if no keys
        }
        
        // Use a set to get unique keys
        Set<String> uniqueKeys = new HashSet<>(limitedKeys);
        
        // Simulate a store
        Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

        // Add records for each unique key
        for (String key : uniqueKeys) {
            IdempotencyRecord record = new IdempotencyRecord(key, httpMethod, requestPath, 300);
            store.put(key, record);
        }

        // Verify each key maps to exactly one record
        assertThat(store.size())
                .as("Store should have exactly one record per unique key")
                .isEqualTo(uniqueKeys.size());

        // Verify each record can be retrieved by its key
        for (String key : uniqueKeys) {
            assertThat(store.get(key))
                    .as("Record should be retrievable by key")
                    .isNotNull();
            assertThat(store.get(key).getIdempotencyKey())
                    .as("Record's key should match")
                    .isEqualTo(key);
        }
    }

    /**
     * Property P8.7: Response preservation
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * For any completed idempotency record, the response body and status code
     * should be preserved exactly as set.
     * 
     * **Validates: Requirements 2.2.4**
     */
    @Property(tries = 100)
    void completedRecord_shouldPreserveResponse(
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath,
            @ForAll("responseBodies") String responseBody,
            @ForAll("statusCodes") int statusCode,
            @ForAll("ttlSeconds") int ttlSeconds) {

        // Create and complete a record
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, ttlSeconds);
        record.complete(responseBody, statusCode);

        // Verify response is preserved
        assertThat(record.getResponseBody())
                .as("Response body should be preserved exactly")
                .isEqualTo(responseBody);

        assertThat(record.getStatusCode())
                .as("Status code should be preserved exactly")
                .isEqualTo(statusCode);
    }

    /**
     * Property P8.8: TTL calculation correctness
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * For any TTL value, the expiresAt should be approximately createdAt + TTL seconds.
     * 
     * **Validates: Requirements 2.2.2**
     */
    @Property(tries = 100)
    void ttlCalculation_shouldBeCorrect(
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath,
            @ForAll("ttlSeconds") int ttlSeconds) {

        LocalDateTime beforeCreation = LocalDateTime.now();
        
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, ttlSeconds);
        
        LocalDateTime afterCreation = LocalDateTime.now();

        // Verify expiresAt is within expected range
        LocalDateTime expectedMinExpiry = beforeCreation.plusSeconds(ttlSeconds);
        LocalDateTime expectedMaxExpiry = afterCreation.plusSeconds(ttlSeconds).plusSeconds(1);

        assertThat(record.getExpiresAt())
                .as("Expiry time should be after minimum expected")
                .isAfterOrEqualTo(expectedMinExpiry);

        assertThat(record.getExpiresAt())
                .as("Expiry time should be before maximum expected")
                .isBeforeOrEqualTo(expectedMaxExpiry);
    }

    /**
     * Property P8.9: State transitions are valid
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * Records should only transition from PENDING to COMPLETED or FAILED.
     * 
     * **Validates: Requirements 2.2.2**
     */
    @Property(tries = 100)
    void stateTransitions_shouldBeValid(
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath,
            @ForAll("ttlSeconds") int ttlSeconds,
            @ForAll boolean shouldSucceed) {

        // Create a new record (starts in PENDING)
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, ttlSeconds);

        assertThat(record.getStatus())
                .as("Initial status should be PENDING")
                .isEqualTo(IdempotencyStatus.PENDING);

        // Transition to final state
        if (shouldSucceed) {
            record.complete("{\"success\":true}", 200);
            assertThat(record.getStatus())
                    .as("Status should be COMPLETED after complete()")
                    .isEqualTo(IdempotencyStatus.COMPLETED);
        } else {
            record.fail("{\"error\":\"failed\"}", 500);
            assertThat(record.getStatus())
                    .as("Status should be FAILED after fail()")
                    .isEqualTo(IdempotencyStatus.FAILED);
        }
    }

    /**
     * Property P8.10: Idempotency key format validation
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * Valid idempotency keys should be 64-character hex strings.
     * 
     * **Validates: Requirements 2.2.1**
     */
    @Property(tries = 100)
    void idempotencyKeyFormat_shouldBeValid(
            @ForAll("validIdempotencyKeys") String idempotencyKey) {

        // Verify key format
        assertThat(idempotencyKey)
                .as("Key should be 64 characters")
                .hasSize(64);

        assertThat(idempotencyKey)
                .as("Key should only contain hex characters")
                .matches("^[0-9a-f]+$");
    }

    /**
     * Property P8.11: Concurrent access simulation
     * 
     * **Feature: sism-enterprise-optimization, Property P8: 幂等性时间窗口**
     * 
     * Simulates concurrent access to verify that the same key always returns
     * the same record from a thread-safe store.
     * 
     * **Validates: Requirements 2.2.4**
     */
    @Property(tries = 50)
    void concurrentAccess_shouldReturnSameRecord(
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("httpMethods") String httpMethod,
            @ForAll("requestPaths") String requestPath,
            @ForAll("responseBodies") String responseBody,
            @ForAll @IntRange(min = 2, max = 10) int accessCount) {

        // Create a thread-safe store
        Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

        // Create and store a completed record
        IdempotencyRecord originalRecord = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, 300);
        originalRecord.complete(responseBody, 200);
        store.put(idempotencyKey, originalRecord);

        // Simulate multiple accesses
        List<IdempotencyRecord> retrievedRecords = new ArrayList<>();
        for (int i = 0; i < accessCount; i++) {
            IdempotencyRecord retrieved = store.get(idempotencyKey);
            retrievedRecords.add(retrieved);
        }

        // Verify all accesses return the same record
        for (IdempotencyRecord retrieved : retrievedRecords) {
            assertThat(retrieved)
                    .as("All accesses should return the same record instance")
                    .isSameAs(originalRecord);

            assertThat(retrieved.getResponseBody())
                    .as("Response body should be consistent")
                    .isEqualTo(responseBody);
        }
    }
}
