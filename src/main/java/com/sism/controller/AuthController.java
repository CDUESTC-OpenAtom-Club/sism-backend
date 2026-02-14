package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.LoginRequest;
import com.sism.entity.SysUser;
import com.sism.service.AuthService;
import com.sism.service.RefreshTokenService;
import com.sism.vo.LoginResponse;
import com.sism.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * Authentication Controller
 * Handles login, logout, token refresh, and current user operations
 * 
 * Requirements: 8.1, 1.2.2, 1.2.4, 1.2.5
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Refresh Token Cookie 名称
     */
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    /**
     * Refresh Token 有效期（秒）
     * 默认 7 天
     */
    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration;

    /**
     * Access Token 有效期（毫秒）
     */
    @Value("${jwt.expiration:900000}")
    private Long accessTokenExpiration;

    /**
     * User login
     * POST /api/auth/login
     * 
     * Generates both Access Token and Refresh Token.
     * Access Token is returned in response body.
     * Refresh Token is set as HttpOnly cookie.
     * 
     * @param request login credentials
     * @param httpRequest HTTP request for device info
     * @param httpResponse HTTP response for setting cookie
     * @return JWT token and user information
     * 
     * Requirements: 1.2.2 Refresh Token 使用 HttpOnly Cookie 存储
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token with refresh token cookie")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        log.info("Login request for user: {}", request.getUsername());
        
        // Authenticate and get login response with access token
        LoginResponse response = authService.login(request);
        
        // Get user entity for refresh token generation
        SysUser user = authService.getUserByUsername(request.getUsername());
        
        // Generate refresh token (may be null if feature is disabled)
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);
        String refreshToken = refreshTokenService.generateRefreshToken(user, deviceInfo, ipAddress);
        
        // Set refresh token as HttpOnly cookie only if available
        if (refreshToken != null) {
            setRefreshTokenCookie(httpResponse, refreshToken);
        } else {
            log.info("Refresh token feature disabled, login without refresh token for user: {}", 
                    request.getUsername());
        }
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * User logout
     * POST /api/auth/logout
     * 
     * Invalidates both Access Token and Refresh Token.
     * Clears the refresh token cookie.
     * 
     * @param authorization JWT token from header
     * @param httpRequest HTTP request for getting refresh token cookie
     * @param httpResponse HTTP response for clearing cookie
     * @return success message
     * 
     * Requirements: 1.2.5 登出时同时清除 Access Token 和 Refresh Token
     */
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Invalidate current JWT token and refresh token")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful")
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Bearer token", required = false)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        
        // Invalidate access token
        authService.logout(authorization);
        
        // Revoke refresh token from cookie
        String refreshToken = getRefreshTokenFromCookie(httpRequest);
        if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken);
        }
        
        // Clear refresh token cookie
        clearRefreshTokenCookie(httpResponse);
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    /**
     * Refresh Access Token
     * POST /api/auth/refresh
     * 
     * Uses the Refresh Token from HttpOnly cookie to generate a new Access Token.
     * Implements token rotation: old refresh token is revoked, new one is issued.
     * 
     * @param httpRequest HTTP request for getting refresh token cookie
     * @param httpResponse HTTP response for setting new cookie
     * @return new access token and user information
     * 
     * Requirements: 1.2.2, 1.2.4 页面刷新后能通过 Refresh Token 恢复会话
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Use refresh token to get a new access token")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        
        // Get refresh token from cookie
        String refreshToken = getRefreshTokenFromCookie(httpRequest);
        if (refreshToken == null) {
            log.warn("Refresh token not found in cookie");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "Refresh token not found"));
        }
        
        // Get device info for new token
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);
        
        // Refresh tokens (token rotation)
        RefreshTokenService.RefreshResult result = refreshTokenService.refreshTokens(
                refreshToken, deviceInfo, ipAddress
        );
        
        // Set new refresh token cookie
        setRefreshTokenCookie(httpResponse, result.refreshToken());
        
        // Build response
        LoginResponse response = LoginResponse.of(
                result.accessToken(),
                accessTokenExpiration / 1000,
                result.user()
        );
        
        log.info("Token refreshed for user: {}", result.user().getUsername());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Get current user information
     * GET /api/auth/me
     * 
     * @param authorization JWT token from header
     * @return current user information
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User information retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<UserVO>> getCurrentUser(
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader("Authorization") String authorization) {
        UserVO user = authService.getCurrentUser(authorization);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Set refresh token as HttpOnly cookie
     * 
     * Security attributes:
     * - HttpOnly: Prevents JavaScript access (XSS protection)
     * - Secure: Only sent over HTTPS (in production)
     * - SameSite=Strict: Prevents CSRF attacks
     * - Path=/api/auth: Limits cookie scope
     * 
     * @param response HTTP response
     * @param refreshToken the refresh token value
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Set to false for local development if not using HTTPS
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000)); // Convert ms to seconds
        // SameSite attribute is set via response header as Cookie class doesn't support it directly
        response.addCookie(cookie);
        // Add SameSite=Strict attribute
        response.addHeader("Set-Cookie", 
                String.format("%s=%s; Path=/api/auth; Max-Age=%d; HttpOnly; Secure; SameSite=Strict",
                        REFRESH_TOKEN_COOKIE_NAME, refreshToken, refreshTokenExpiration / 1000));
    }

    /**
     * Clear refresh token cookie
     * 
     * @param response HTTP response
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0); // Expire immediately
        response.addCookie(cookie);
        // Add SameSite=Strict attribute for clearing
        response.addHeader("Set-Cookie", 
                String.format("%s=; Path=/api/auth; Max-Age=0; HttpOnly; Secure; SameSite=Strict",
                        REFRESH_TOKEN_COOKIE_NAME));
    }

    /**
     * Get refresh token from cookie
     * 
     * @param request HTTP request
     * @return refresh token value or null if not found
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get client IP address from request
     * Handles proxy headers (X-Forwarded-For, X-Real-IP)
     * 
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
