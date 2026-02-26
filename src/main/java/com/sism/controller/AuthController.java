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
 * Authentication Controller for SISM (Strategic Indicator Management System).
 * 
 * <p>This controller handles all authentication and authorization operations including:
 * <ul>
 *   <li>User login with JWT token generation</li>
 *   <li>User logout with token invalidation</li>
 *   <li>Access token refresh using refresh tokens</li>
 *   <li>Current user information retrieval</li>
 * </ul>
 * 
 * <h2>Security Features</h2>
 * <ul>
 *   <li><b>JWT Authentication</b>: Access tokens for API authentication</li>
 *   <li><b>Refresh Token Rotation</b>: Automatic token rotation on refresh</li>
 *   <li><b>HttpOnly Cookies</b>: Secure refresh token storage</li>
 *   <li><b>CSRF Protection</b>: SameSite=Strict cookie attribute</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li>POST /api/auth/login - User authentication</li>
 *   <li>POST /api/auth/logout - Session termination</li>
 *   <li>POST /api/auth/refresh - Token refresh</li>
 *   <li>GET /api/auth/me - Current user info</li>
 * </ul>
 * 
 * <h2>Token Management</h2>
 * <p>Access tokens are short-lived (15 minutes) and returned in response body.
 * Refresh tokens are long-lived (7 days) and stored in HttpOnly cookies for security.
 * 
 * @author SISM Development Team
 * @version 1.0
 * @since 1.0
 * @see com.sism.service.AuthService
 * @see com.sism.service.RefreshTokenService
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
     * Authenticates a user and generates JWT tokens.
     * 
     * <p>This endpoint validates user credentials and generates both an access token
     * and a refresh token. The access token is returned in the response body for
     * use in subsequent API calls. The refresh token is set as an HttpOnly cookie
     * for enhanced security.
     * 
     * <h3>Security Features</h3>
     * <ul>
     *   <li>Password validation using BCrypt</li>
     *   <li>Access token (15 min expiry) in response body</li>
     *   <li>Refresh token (7 days expiry) in HttpOnly cookie</li>
     *   <li>Device fingerprinting via User-Agent</li>
     *   <li>IP address tracking for security audit</li>
     * </ul>
     * 
     * <h3>Response Structure</h3>
     * <pre>
     * {
     *   "code": 200,
     *   "message": "Login successful",
     *   "data": {
     *     "token": "eyJhbGciOiJIUzI1NiIs...",
     *     "expiresIn": 900,
     *     "user": {
     *       "id": 1,
     *       "username": "admin",
     *       "role": "strategic_dept"
     *     }
     *   }
     * }
     * </pre>
     * 
     * @param request the login credentials containing username and password
     * @param httpRequest the HTTP request for extracting device info and IP address
     * @param httpResponse the HTTP response for setting refresh token cookie
     * @return ResponseEntity containing JWT token and user information
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     * @see com.sism.dto.LoginRequest
     * @see com.sism.vo.LoginResponse
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
     * Logs out the current user and invalidates all tokens.
     * 
     * <p>This endpoint performs a complete logout by:
     * <ul>
     *   <li>Invalidating the access token (added to blacklist)</li>
     *   <li>Revoking the refresh token from database</li>
     *   <li>Clearing the refresh token cookie</li>
     * </ul>
     * 
     * <p>After logout, both the access token and refresh token become unusable.
     * The user must login again to obtain new tokens.
     * 
     * <h3>Security Notes</h3>
     * <ul>
     *   <li>Access token is blacklisted until its natural expiration</li>
     *   <li>Refresh token is permanently revoked in database</li>
     *   <li>Cookie is cleared with Max-Age=0</li>
     * </ul>
     * 
     * @param authorization the Bearer token from Authorization header (optional)
     * @param httpRequest the HTTP request for extracting refresh token cookie
     * @param httpResponse the HTTP response for clearing the cookie
     * @return ResponseEntity with success message
     * @see com.sism.service.AuthService#logout(String)
     * @see com.sism.service.RefreshTokenService#revokeToken(String)
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
     * Refreshes the access token using a valid refresh token.
     * 
     * <p>This endpoint implements token rotation for enhanced security:
     * <ol>
     *   <li>Validates the refresh token from HttpOnly cookie</li>
     *   <li>Revokes the old refresh token</li>
     *   <li>Generates a new access token</li>
     *   <li>Generates a new refresh token</li>
     *   <li>Sets the new refresh token in cookie</li>
     * </ol>
     * 
     * <h3>Token Rotation Benefits</h3>
     * <ul>
     *   <li>Limits the window of opportunity for token theft</li>
     *   <li>Detects token reuse (potential security breach)</li>
     *   <li>Provides automatic session extension</li>
     * </ul>
     * 
     * <h3>Use Cases</h3>
     * <ul>
     *   <li>Access token expired (15 minutes)</li>
     *   <li>Page refresh/reload</li>
     *   <li>Session recovery after browser restart</li>
     * </ul>
     * 
     * <h3>Error Scenarios</h3>
     * <ul>
     *   <li>401: Refresh token not found in cookie</li>
     *   <li>401: Refresh token expired or revoked</li>
     *   <li>401: Refresh token reuse detected</li>
     * </ul>
     * 
     * @param httpRequest the HTTP request for extracting refresh token cookie
     * @param httpResponse the HTTP response for setting new refresh token cookie
     * @return ResponseEntity containing new access token and user information
     * @throws com.sism.exception.UnauthorizedException if refresh token is invalid or expired
     * @see com.sism.service.RefreshTokenService#refreshTokens(String, String, String)
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
     * Retrieves information about the currently authenticated user.
     * 
     * <p>This endpoint extracts the user information from the JWT token
     * in the Authorization header and returns the user's profile data.
     * 
     * <h3>Required Header</h3>
     * <pre>
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
     * </pre>
     * 
     * <h3>Response Structure</h3>
     * <pre>
     * {
     *   "code": 200,
     *   "data": {
     *     "id": 1,
     *     "username": "admin",
     *     "realName": "Administrator",
     *     "role": "strategic_dept",
     *     "orgId": 1,
     *     "orgName": "Strategic Development Department"
     *   }
     * }
     * </pre>
     * 
     * @param authorization the Bearer token from Authorization header (required)
     * @return ResponseEntity containing current user information
     * @throws com.sism.exception.UnauthorizedException if token is invalid or expired
     * @see com.sism.vo.UserVO
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
