package com.sism.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.entity.IdempotencyRecord;
import com.sism.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 幂等性过滤器
 * 
 * 功能:
 * - 拦截写操作请求 (POST, PUT, DELETE, PATCH)
 * - 检查 X-Idempotency-Key 请求头
 * - 对于重复请求，返回缓存的响应
 * - 对于新请求，保存响应结果
 * 
 * **Property P8**: 重复请求返回缓存的响应而非重新执行
 * 
 * **Validates: Requirements 2.2.2, 2.2.3, 2.2.4**
 */
@Slf4j
@Component
@Order(10) // 在认证过滤器之后执行
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    /**
     * 幂等性 Key 请求头名称
     */
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    /**
     * 是否启用幂等性检查
     */
    @Value("${idempotency.enabled:true}")
    private boolean enabled;

    /**
     * 需要幂等性检查的路径前缀列表
     */
    @Value("${idempotency.protected-paths:/api/indicators,/api/milestones,/api/tasks,/api/approvals,/api/progress}")
    private List<String> protectedPaths;

    /**
     * 需要幂等性检查的 HTTP 方法
     */
    private static final List<String> WRITE_METHODS = List.of("POST", "PUT", "DELETE", "PATCH");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 检查是否启用幂等性检查
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // 检查是否是需要幂等性检查的请求
        if (!requiresIdempotencyCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取幂等性 Key
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        
        // 如果没有提供幂等性 Key，继续处理请求（不强制要求）
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.debug("No idempotency key provided for {} {}", 
                    request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // 验证幂等性 Key 格式（应该是 64 字符的十六进制字符串）
        if (!isValidIdempotencyKey(idempotencyKey)) {
            sendErrorResponse(response, HttpStatus.BAD_REQUEST, 
                    "Invalid idempotency key format. Expected 64-character hex string.");
            return;
        }

        // 检查是否是重复请求
        Optional<IdempotencyRecord> existingRecord = idempotencyService.checkDuplicate(idempotencyKey);
        
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            
            if (record.isValid()) {
                // 返回缓存的响应
                log.info("Returning cached response for idempotency key: {}...", 
                        idempotencyKey.substring(0, 8));
                sendCachedResponse(response, record);
                return;
            }
            
            if (record.isPending()) {
                // 请求正在处理中，返回 409 Conflict
                log.warn("Request with idempotency key {}... is still being processed", 
                        idempotencyKey.substring(0, 8));
                sendErrorResponse(response, HttpStatus.CONFLICT, 
                        "A request with this idempotency key is currently being processed. Please retry later.");
                return;
            }
        }

        // 创建新的幂等性记录
        try {
            idempotencyService.startProcessing(
                    idempotencyKey, 
                    request.getMethod(), 
                    request.getRequestURI());
        } catch (Exception e) {
            // 如果创建记录失败（可能是并发冲突），检查是否已存在
            log.warn("Failed to create idempotency record, checking for existing: {}", e.getMessage());
            existingRecord = idempotencyService.checkDuplicate(idempotencyKey);
            if (existingRecord.isPresent() && existingRecord.get().isValid()) {
                sendCachedResponse(response, existingRecord.get());
                return;
            }
            // 如果仍然失败，继续处理请求但不保存幂等性记录
            filterChain.doFilter(request, response);
            return;
        }

        // 使用 ContentCachingResponseWrapper 来捕获响应内容
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            // 继续处理请求
            filterChain.doFilter(request, responseWrapper);

            // 保存响应结果
            int statusCode = responseWrapper.getStatus();
            String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

            if (statusCode >= 200 && statusCode < 300) {
                idempotencyService.saveSuccess(idempotencyKey, responseBody, statusCode);
            } else if (statusCode >= 400) {
                idempotencyService.saveFailure(idempotencyKey, responseBody, statusCode);
            }

            // 将响应内容写回原始响应
            responseWrapper.copyBodyToResponse();

        } catch (Exception e) {
            // 请求处理失败，删除幂等性记录以允许重试
            log.error("Request processing failed, removing idempotency record: {}", e.getMessage());
            idempotencyService.deleteRecord(idempotencyKey);
            throw e;
        }
    }

    /**
     * 检查请求是否需要幂等性检查
     */
    private boolean requiresIdempotencyCheck(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // 只检查写操作
        if (!WRITE_METHODS.contains(method)) {
            return false;
        }

        // 检查路径是否在保护列表中
        return protectedPaths.stream().anyMatch(path::startsWith);
    }

    /**
     * 验证幂等性 Key 格式
     */
    private boolean isValidIdempotencyKey(String key) {
        if (key == null || key.length() != 64) {
            return false;
        }
        return key.matches("^[0-9a-fA-F]+$");
    }

    /**
     * 发送缓存的响应
     */
    private void sendCachedResponse(HttpServletResponse response, IdempotencyRecord record) 
            throws IOException {
        response.setStatus(record.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.addHeader("X-Idempotency-Replayed", "true");
        
        if (record.getResponseBody() != null) {
            response.getWriter().write(record.getResponseBody());
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) 
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> errorBody = Map.of(
                "code", "IDEMPOTENCY_ERROR",
                "message", message,
                "timestamp", java.time.Instant.now().toString()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
