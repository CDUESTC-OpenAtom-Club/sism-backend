package com.sism.property;

import com.sism.common.ErrorResponse;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 错误响应属性测试
 * 
 * Validates: P10 - 所有错误响应包含 requestId 用于追踪
 * 
 * 验证错误响应的核心属性：
 * 1. 所有错误响应都包含必需字段 (code, message, requestId, timestamp)
 * 2. requestId 格式正确（UUID）
 * 3. timestamp 格式正确（ISO 8601）
 * 4. 错误码格式正确
 */
class ErrorResponsePropertyTest {

    // ========================================================================
    // Arbitraries (生成器)
    // ========================================================================

    @Provide
    Arbitrary<String> errorCodes() {
        return Arbitraries.of(
            "AUTH_001", "AUTH_002", "AUTH_003", "AUTH_004",
            "VAL_001", "VAL_002", "VAL_003",
            "BIZ_001", "BIZ_002", "BIZ_003",
            "SYS_001", "SYS_002", "SYS_003",
            "NET_001", "NET_002",
            "RATE_001", "RATE_002"
        );
    }

    @Provide
    Arbitrary<String> errorMessages() {
        return Arbitraries.strings()
            .ofMinLength(1)
            .ofMaxLength(200)
            .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<String> requestIds() {
        return Arbitraries.create(() -> UUID.randomUUID().toString());
    }

    @Provide
    Arbitrary<Integer> httpStatusCodes() {
        return Arbitraries.of(400, 401, 403, 404, 409, 422, 429, 500, 502, 503, 504);
    }

    // ========================================================================
    // P10: 错误响应格式一致性
    // ========================================================================

    /**
     * Validates: P10
     * 
     * 属性: 所有创建的错误响应都包含必需字段
     * ∀ error ∈ AllErrors:
     *   response(error) has { code: string, message: string, requestId: string, timestamp: ISO8601 }
     */
    @Property(tries = 100)
    void allErrorResponsesContainRequiredFields(
            @ForAll("errorCodes") String code,
            @ForAll("errorMessages") String message,
            @ForAll("requestIds") String requestId) {
        
        ErrorResponse<Void> response = ErrorResponse.of(code, message, requestId);
        
        // 验证必需字段存在
        assertThat(response.getCode()).isNotNull().isNotBlank();
        assertThat(response.getMessage()).isNotNull().isNotBlank();
        assertThat(response.getRequestId()).isNotNull().isNotBlank();
        assertThat(response.getTimestamp()).isNotNull().isNotBlank();
        
        // 验证字段值正确
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getRequestId()).isEqualTo(requestId);
    }

    /**
     * Validates: P10
     * 
     * 属性: 带详情的错误响应也包含所有必需字段
     */
    @Property(tries = 50)
    void errorResponsesWithDetailsContainRequiredFields(
            @ForAll("errorCodes") String code,
            @ForAll("errorMessages") String message,
            @ForAll("requestIds") String requestId) {
        
        Map<String, Object> details = new HashMap<>();
        details.put("field", "testField");
        details.put("value", "testValue");
        
        ErrorResponse<Map<String, Object>> response = ErrorResponse.of(code, message, details, requestId);
        
        // 验证必需字段存在
        assertThat(response.getCode()).isNotNull().isNotBlank();
        assertThat(response.getMessage()).isNotNull().isNotBlank();
        assertThat(response.getRequestId()).isNotNull().isNotBlank();
        assertThat(response.getTimestamp()).isNotNull().isNotBlank();
        
        // 验证详情字段
        assertThat(response.getDetails()).isNotNull();
        assertThat(response.getDetails()).containsKey("field");
    }

    // ========================================================================
    // requestId 格式验证
    // ========================================================================

    /**
     * Validates: P10
     * 
     * 属性: requestId 是有效的 UUID 格式
     */
    @Property(tries = 100)
    void requestIdIsValidUuidFormat(@ForAll("requestIds") String requestId) {
        ErrorResponse<Void> response = ErrorResponse.of("SYS_001", "Test error", requestId);
        
        // 验证 requestId 是有效的 UUID
        assertThat(response.getRequestId()).isNotNull();
        
        // UUID 格式验证: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        String uuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
        assertThat(response.getRequestId()).matches(uuidRegex);
    }

    /**
     * Validates: P10
     * 
     * 属性: 每次生成的 requestId 都是唯一的
     */
    @Property(tries = 20)
    void generatedRequestIdsAreUnique(@ForAll @IntRange(min = 10, max = 50) int count) {
        java.util.Set<String> ids = new java.util.HashSet<>();
        
        for (int i = 0; i < count; i++) {
            String requestId = UUID.randomUUID().toString();
            ids.add(requestId);
        }
        
        // 所有生成的 ID 都应该是唯一的
        assertThat(ids).hasSize(count);
    }

    // ========================================================================
    // timestamp 格式验证
    // ========================================================================

    /**
     * Validates: P10
     * 
     * 属性: timestamp 是有效的 ISO 8601 格式
     */
    @Property(tries = 50)
    void timestampIsValidIso8601Format(
            @ForAll("errorCodes") String code,
            @ForAll("requestIds") String requestId) {
        
        ErrorResponse<Void> response = ErrorResponse.of(code, "Test error", requestId);
        
        // 验证 timestamp 存在
        assertThat(response.getTimestamp()).isNotNull().isNotBlank();
        
        // 验证可以解析为 Instant
        Instant parsed = Instant.parse(response.getTimestamp());
        assertThat(parsed).isNotNull();
        
        // 验证时间戳是当前时间附近（1分钟内）
        Instant now = Instant.now();
        assertThat(parsed).isBetween(now.minusSeconds(60), now.plusSeconds(60));
    }

    /**
     * Validates: P10
     * 
     * 属性: timestamp 包含日期和时间部分
     */
    @Property(tries = 50)
    void timestampContainsDateAndTime(
            @ForAll("errorCodes") String code,
            @ForAll("requestIds") String requestId) {
        
        ErrorResponse<Void> response = ErrorResponse.of(code, "Test error", requestId);
        
        // ISO 8601 格式应该包含 T 分隔符
        assertThat(response.getTimestamp()).contains("T");
        
        // 应该包含日期部分（年-月-日）
        assertThat(response.getTimestamp()).containsPattern("\\d{4}-\\d{2}-\\d{2}");
    }

    // ========================================================================
    // 错误码格式验证
    // ========================================================================

    /**
     * Validates: P10
     * 
     * 属性: 错误码格式正确（模块_编号）
     */
    @Property(tries = 100)
    void errorCodeFormatIsCorrect(@ForAll("errorCodes") String code) {
        ErrorResponse<Void> response = ErrorResponse.of(code, "Test error", UUID.randomUUID().toString());
        
        // 错误码应该是非空字符串
        assertThat(response.getCode()).isNotNull().isNotBlank();
        
        // 错误码应该匹配 PREFIX_NNN 模式
        String codePattern = "^[A-Z]{2,5}_\\d{3}$";
        assertThat(response.getCode()).matches(codePattern);
    }

    /**
     * Validates: P10
     * 
     * 属性: HTTP 状态码正确映射到错误码
     */
    @Property(tries = 50)
    void httpStatusMapsToCorrectErrorCode(@ForAll("httpStatusCodes") int status) {
        String errorCode = ErrorResponse.getCodeByStatus(status);
        
        // 错误码应该是非空字符串
        assertThat(errorCode).isNotNull().isNotBlank();
        
        // 验证映射正确性
        switch (status) {
            case 400 -> assertThat(errorCode).isEqualTo("VAL_001");
            case 401 -> assertThat(errorCode).isEqualTo("AUTH_003");
            case 403 -> assertThat(errorCode).isEqualTo("AUTH_004");
            case 404 -> assertThat(errorCode).isEqualTo("BIZ_002");
            case 409 -> assertThat(errorCode).isEqualTo("BIZ_001");
            case 422 -> assertThat(errorCode).isEqualTo("VAL_002");
            case 429 -> assertThat(errorCode).isEqualTo("RATE_001");
            case 500 -> assertThat(errorCode).isEqualTo("SYS_001");
            case 502, 503, 504 -> assertThat(errorCode).isEqualTo("SYS_003");
        }
    }

    // ========================================================================
    // Builder 模式验证
    // ========================================================================

    /**
     * 属性: Builder 模式正确构建错误响应
     */
    @Property(tries = 50)
    void builderPatternWorksCorrectly(
            @ForAll("errorCodes") String code,
            @ForAll("errorMessages") String message,
            @ForAll("requestIds") String requestId) {
        
        String timestamp = Instant.now().toString();
        
        ErrorResponse<Void> response = ErrorResponse.<Void>builder()
                .code(code)
                .message(message)
                .requestId(requestId)
                .timestamp(timestamp)
                .build();
        
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getRequestId()).isEqualTo(requestId);
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getDetails()).isNull();
    }

    /**
     * 属性: Builder 模式支持可选的 details 字段
     */
    @Property(tries = 30)
    void builderSupportsOptionalDetails(
            @ForAll("errorCodes") String code,
            @ForAll("requestIds") String requestId) {
        
        Map<String, String> details = Map.of("key1", "value1", "key2", "value2");
        
        ErrorResponse<Map<String, String>> response = ErrorResponse.<Map<String, String>>builder()
                .code(code)
                .message("Test error")
                .details(details)
                .requestId(requestId)
                .timestamp(Instant.now().toString())
                .build();
        
        assertThat(response.getDetails()).isNotNull();
        assertThat(response.getDetails()).containsEntry("key1", "value1");
        assertThat(response.getDetails()).containsEntry("key2", "value2");
    }

    // ========================================================================
    // 边界情况
    // ========================================================================

    /**
     * 属性: 空详情不影响必需字段
     */
    @Property(tries = 30)
    void nullDetailsDoesNotAffectRequiredFields(
            @ForAll("errorCodes") String code,
            @ForAll("errorMessages") String message,
            @ForAll("requestIds") String requestId) {
        
        ErrorResponse<Void> response = ErrorResponse.of(code, message, requestId);
        
        // details 应该为 null
        assertThat(response.getDetails()).isNull();
        
        // 但必需字段应该存在
        assertThat(response.getCode()).isNotNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getRequestId()).isNotNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    /**
     * 属性: 长消息不会被截断
     */
    @Property(tries = 20)
    void longMessagesAreNotTruncated(
            @ForAll("errorCodes") String code,
            @ForAll("requestIds") String requestId,
            @ForAll @StringLength(min = 100, max = 500) String longMessage) {
        
        // 过滤空白字符串
        if (longMessage.isBlank()) {
            return;
        }
        
        ErrorResponse<Void> response = ErrorResponse.of(code, longMessage, requestId);
        
        // 消息应该完整保留
        assertThat(response.getMessage()).isEqualTo(longMessage);
        assertThat(response.getMessage()).hasSize(longMessage.length());
    }
}
