package com.sism.config;

import com.sism.util.JwtUtil;
import com.sism.util.TokenBlacklistService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test for JWT Authentication Filter - Token Expiration Handling
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.clearContext();
        responseWriter = new StringWriter();
    }

    @Test
    void testExpiredToken_Returns401() throws Exception {
        // Given: 请求包含过期的 token
        String expiredToken = "expired.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        when(tokenBlacklistService.isBlacklisted(expiredToken)).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // When: validateToken 抛出 ExpiredJwtException
        when(jwtUtil.validateToken(expiredToken)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        // Then: 应该返回 401 状态码
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertTrue(responseWriter.toString().contains("\"code\":401"));
        assertTrue(responseWriter.toString().contains("Token expired"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testValidToken_ContinuesFilterChain() throws Exception {
        // Given: 请求包含有效的 token
        String validToken = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenBlacklistService.isBlacklisted(validToken)).thenReturn(false);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.extractUsername(validToken)).thenReturn("testuser");
        when(jwtUtil.extractUserId(validToken)).thenReturn(1L);
        when(jwtUtil.extractOrgId(validToken)).thenReturn(100L);

        // When: 过滤器处理请求
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: 应该继续过滤链
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testBlacklistedToken_ContinuesWithoutAuth() throws Exception {
        // Given: 请求包含黑名单中的 token
        String blacklistedToken = "blacklisted.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + blacklistedToken);
        when(tokenBlacklistService.isBlacklisted(blacklistedToken)).thenReturn(true);

        // When: 过滤器处理请求
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: 应该继续过滤链但不设置认证
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).validateToken(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testNoToken_ContinuesFilterChain() throws Exception {
        // Given: 请求不包含 token
        when(request.getHeader("Authorization")).thenReturn(null);

        // When: 过滤器处理请求
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: 应该继续过滤链
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).validateToken(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testInvalidToken_Returns401() throws Exception {
        // Given: 请求包含无效的 token
        String invalidToken = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(tokenBlacklistService.isBlacklisted(invalidToken)).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(jwtUtil.validateToken(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

        // When: 过滤器处理请求
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: 应该返回 401 状态码
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertTrue(responseWriter.toString().contains("\"code\":401"));
        assertTrue(responseWriter.toString().contains("Authentication failed"));
        verify(filterChain, never()).doFilter(request, response);
    }
}
