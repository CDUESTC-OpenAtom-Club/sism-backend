package com.sism.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.service.RateLimiter;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * 频率限制过滤器
 * 
 * 功能:
 * - 拦截所有 API 请求
 * - 根据 IP 地址或用户 ID 进行频率限制
 * - 不同接口可配置不同的限制策略
 * - 超过限制时返回 429 状态码
 * 
 * **Property P9**: 在时间窗口内，请求次数超过限制时返回 429
 * 
 * **Validates: Requirements 2.3.3, 2.3.4, 2.3.5**
 */
@Slf4j
@Component
@Order(5) // 在认证过滤器之前执行
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    /**
     * 是否启用频率限制
     */
    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    /**
     * 登录接口限制次数（每分钟）
     */
    @Value("${rate-limit.login.limit:5}")
    private int loginLimit;

    /**
     * 登录接口时间窗口（秒）
     */
    @Value("${rate-limit.login.window-seconds:60}")
    private int loginWindowSeconds;

    /**
     * 通用 API 限制次数（每分钟）
     */
    @Value("${rate-limit.api.limit:100}")
    private int apiLimit;

    /**
     * 通用 API 时间窗口（秒）
     */
    @Value("${rate-limit.api.window-seconds:60}")
    private int apiWindowSeconds;

    /**
     * 响应头名称
     */
    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 检查是否启用频率限制
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // 跳过不需要限制的路径
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取客户端标识符
        String clientId = getClientIdentifier(request);
        
        // 确定限制策略
        RateLimitConfig config = getRateLimitConfig(request);
        
        // 构建限制 Key
        String rateLimitKey = buildRateLimitKey(config.type, clientId);

        // 检查是否允许请求
        if (!rateLimiter.isAllowed(rateLimitKey, config.limit, config.windowSeconds)) {
            // 超过限制，返回 429
            sendRateLimitExceededResponse(response, rateLimitKey, config);
            return;
        }

        // 添加频率限制响应头
        addRateLimitHeaders(response, rateLimitKey, config);

        // 继续处理请求
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否应该跳过频率限制
     */
    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 跳过健康检查
        if (path.contains("/actuator/health")) {
            return true;
        }

        // 跳过 Swagger 文档
        if (path.contains("/swagger") || path.contains("/v3/api-docs")) {
            return true;
        }

        // 跳过 OPTIONS 请求（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        return false;
    }

    /**
     * 获取客户端标识符
     * 
     * 优先使用用户 ID（如果已认证），否则使用 IP 地址
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // 尝试从请求属性获取用户 ID（由 JWT 过滤器设置）
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }

        // 使用 IP 地址
        return "ip:" + getClientIp(request);
    }

    /**
     * 获取客户端 IP 地址
     * 
     * 支持代理服务器场景
     */
    private String getClientIp(HttpServletRequest request) {
        // 检查 X-Forwarded-For 头（代理场景）
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // 取第一个 IP（原始客户端 IP）
            return xForwardedFor.split(",")[0].trim();
        }

        // 检查 X-Real-IP 头（Nginx 代理）
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        // 使用远程地址
        return request.getRemoteAddr();
    }

    /**
     * 获取频率限制配置
     */
    private RateLimitConfig getRateLimitConfig(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 登录接口使用更严格的限制
        if (path.contains("/auth/login") || path.contains("/auth/refresh")) {
            return new RateLimitConfig("login", loginLimit, loginWindowSeconds);
        }

        // 其他 API 使用通用限制
        return new RateLimitConfig("api", apiLimit, apiWindowSeconds);
    }

    /**
     * 构建频率限制 Key
     */
    private String buildRateLimitKey(String type, String clientId) {
        return type + ":" + clientId;
    }

    /**
     * 发送频率限制超出响应
     */
    private void sendRateLimitExceededResponse(
            HttpServletResponse response,
            String rateLimitKey,
            RateLimitConfig config) throws IOException {

        long resetTimeSeconds = rateLimiter.getResetTimeSeconds(rateLimitKey, config.windowSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 添加频率限制响应头
        response.setHeader(HEADER_LIMIT, String.valueOf(config.limit));
        response.setHeader(HEADER_REMAINING, "0");
        response.setHeader(HEADER_RESET, String.valueOf(Instant.now().getEpochSecond() + resetTimeSeconds));
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(resetTimeSeconds));

        // 构建错误响应体
        Map<String, Object> errorBody = Map.of(
                "code", "RATE_LIMIT_EXCEEDED",
                "message", "请求过于频繁，请稍后再试",
                "details", Map.of(
                        "limit", config.limit,
                        "windowSeconds", config.windowSeconds,
                        "retryAfterSeconds", resetTimeSeconds
                ),
                "timestamp", Instant.now().toString()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));

        log.warn("Rate limit exceeded for key: {}, limit: {}/{} per {} seconds",
                rateLimitKey, config.limit, config.limit, config.windowSeconds);
    }

    /**
     * 添加频率限制响应头
     */
    private void addRateLimitHeaders(
            HttpServletResponse response,
            String rateLimitKey,
            RateLimitConfig config) {

        int remaining = rateLimiter.getRemainingQuota(rateLimitKey, config.limit, config.windowSeconds);
        long resetTimeSeconds = rateLimiter.getResetTimeSeconds(rateLimitKey, config.windowSeconds);

        response.setHeader(HEADER_LIMIT, String.valueOf(config.limit));
        response.setHeader(HEADER_REMAINING, String.valueOf(remaining));
        response.setHeader(HEADER_RESET, String.valueOf(Instant.now().getEpochSecond() + resetTimeSeconds));
    }

    /**
     * 频率限制配置
     */
    private record RateLimitConfig(String type, int limit, int windowSeconds) {}
}
