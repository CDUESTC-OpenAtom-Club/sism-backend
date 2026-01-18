package com.sism.property;

import com.sism.entity.AppUser;
import com.sism.entity.Org;
import com.sism.repository.OrgRepository;
import com.sism.repository.UserRepository;
import com.sism.util.JwtUtil;
import com.sism.util.TokenBlacklistService;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Property-based tests for Authentication Verification
 * 
 * **Feature: production-deployment-integration, Property 21: 权限验证正确性**
 * 
 * For any user and resource combination, the system SHALL correctly verify 
 * if the user has the appropriate access permission. Valid JWT tokens should 
 * grant access to protected endpoints, while missing or invalid tokens should 
 * be rejected.
 * 
 * **Validates: Requirements 12.3**
 */
@JqwikSpringSupport
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationVerificationPropertyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    // ==================== Protected Endpoints ====================
    
    /**
     * List of protected API endpoints that require authentication.
     * These endpoints should return 403 Forbidden when accessed without a valid token.
     */
    private static final List<String> PROTECTED_GET_ENDPOINTS = List.of(
            "/api/indicators",
            "/api/milestones",
            "/api/tasks",
            "/api/orgs",
            "/api/audit-logs",
            "/api/alerts",
            "/api/adhoc-tasks",
            "/api/reports"
    );

    // ==================== Helper Methods ====================

    private AppUser getTestUser() {
        return userRepository.findAll().stream()
                .filter(u -> u.getOrg() != null)
                .findFirst()
                .orElse(null);
    }

    private Org getTestOrg() {
        return orgRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Generate a valid JWT token for a user
     */
    private String generateValidToken(AppUser user) {
        return jwtUtil.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getOrg().getOrgId()
        );
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> protectedEndpoints() {
        return Arbitraries.of(PROTECTED_GET_ENDPOINTS);
    }

    @Provide
    Arbitrary<String> invalidTokens() {
        return Arbitraries.of(
                "",                                    // Empty token
                "invalid",                             // Plain invalid string
                "invalid.token.format",                // Invalid JWT format
                "eyJhbGciOiJIUzI1NiJ9.invalid.sig",   // Malformed JWT
                "Bearer invalid",                      // With Bearer prefix (shouldn't be in token)
                "null",                                // Literal null string
                "   ",                                 // Whitespace only
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxfQ.invalid" // Expired/invalid signature
        );
    }

    @Provide
    Arbitrary<String> randomStrings() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(100);
    }

    // ==================== Property Tests ====================

    /**
     * Property 21.1: Valid JWT token grants access to protected endpoints
     * 
     * **Feature: production-deployment-integration, Property 21: 权限验证正确性**
     * 
     * For any protected endpoint and valid user, a request with a valid JWT token
     * SHALL be accepted (not return 401 or 403).
     * 
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 50)
    void validToken_shouldGrantAccessToProtectedEndpoints(
            @ForAll("protectedEndpoints") String endpoint) throws Exception {

        // Get test user
        AppUser testUser = getTestUser();
        assumeThat(testUser).isNotNull();
        assumeThat(testUser.getOrg()).isNotNull();

        // Generate valid token
        String validToken = generateValidToken(testUser);

        // Act & Assert: Request with valid token should not return 401 or 403
        mockMvc.perform(get(endpoint)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should not be 401 (Unauthorized) or 403 (Forbidden)
                    assertThat(status)
                            .as("Endpoint %s should accept valid token, but returned %d", endpoint, status)
                            .isNotIn(401, 403);
                });
    }

    /**
     * Property 21.2: Missing token results in 403 Forbidden for protected endpoints
     * 
     * **Feature: production-deployment-integration, Property 21: 权限验证正确性**
     * 
     * For any protected endpoint, a request without an Authorization header
     * SHALL be rejected with 403 Forbidden status.
     * 
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 50)
    void missingToken_shouldReturn403ForProtectedEndpoints(
            @ForAll("protectedEndpoints") String endpoint) throws Exception {

        // Act & Assert: Request without token should return 403
        mockMvc.perform(get(endpoint))
                .andExpect(status().isForbidden());
    }

    /**
     * Property 21.3: Invalid token results in 401 Unauthorized
     * 
     * **Feature: production-deployment-integration, Property 21: 权限验证正确性**
     * 
     * For any protected endpoint and invalid token, the request SHALL be rejected
     * with either 401 Unauthorized or 403 Forbidden status.
     * 
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 50)
    void invalidToken_shouldBeRejectedForProtectedEndpoints(
            @ForAll("protectedEndpoints") String endpoint,
            @ForAll("invalidTokens") String invalidToken) throws Exception {

        // Act & Assert: Request with invalid token should return 401 or 403
        mockMvc.perform(get(endpoint)
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("Endpoint %s should reject invalid token '%s', but returned %d", 
                                endpoint, invalidToken, status)
                            .isIn(401, 403);
                });
    }

    /**
     * Property 21.4: Blacklisted token is rejected
     * 
     * **Feature: production-deployment-integration, Property 21: 权限验证正确性**
     * 
     * For any valid token that has been blacklisted (e.g., after logout),
     * the request SHALL be rejected with 401 or 403 status.
     * 
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 20)
    void blacklistedToken_shouldBeRejectedForProtectedEndpoints(
            @ForAll("protectedEndpoints") String endpoint) throws Exception {

        // Get test user
        AppUser testUser = getTestUser();
        assumeThat(testUser).isNotNull();
        assumeThat(testUser.getOrg()).isNotNull();

        // Generate valid token and blacklist it
        String token = generateValidToken(testUser);
        tokenBlacklistService.blacklist(token);

        try {
            // Act & Assert: Request with blacklisted token should return 401 or 403
            mockMvc.perform(get(endpoint)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertThat(status)
                                .as("Endpoint %s should reject blacklisted token, but returned %d", 
                                    endpoint, status)
                                .isIn(401, 403);
                    });
        } finally {
            // Clean up: remove from blacklist (if the service supports it)
            // Note: TokenBlacklistService may not have a remove method, 
            // tokens typically expire naturally
        }
    }

    /**
     * Property 21.5: JWT token validation is consistent
     * 
     * **Feature: production-deployment-integration, Property 21: 权限验证正确性**
     * 
     * For any valid user, the JwtUtil.validateToken method SHALL return true
     * for tokens it generates, and the extracted claims SHALL match the input.
     * 
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 100)
    void jwtTokenValidation_shouldBeConsistent() {
        // Get test user
        AppUser testUser = getTestUser();
        assumeThat(testUser).isNotNull();
        assumeThat(testUser.getOrg()).isNotNull();

        // Generate token
        String token = generateValidToken(testUser);

        // Assert: Token should be valid
        assertThat(jwtUtil.validateToken(token))
                .as("Generated token should be valid")
                .isTrue();

        // Assert: Extracted claims should match input
        assertThat(jwtUtil.extractUsername(token))
                .as("Extracted username should match")
                .isEqualTo(testUser.getUsername());

        assertThat(jwtUtil.extractUserId(token))
                .as("Extracted userId should match")
                .isEqualTo(testUser.getUserId());

        assertThat(jwtUtil.extractOrgId(token))
                .as("Extracted orgId should match")
                .isEqualTo(testUser.getOrg().getOrgId());
    }

    /**
     * Property 21.6: Random strings are not valid JWT tokens
     * 
     * **Feature: production-deployment-integration, Property 21: 权限验证正确性**
     * 
     * For any random string, the JwtUtil.validateToken method SHALL return false.
     * 
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 100)
    void randomStrings_shouldNotBeValidTokens(
            @ForAll("randomStrings") String randomString) {

        // Assert: Random strings should not be valid tokens
        assertThat(jwtUtil.validateToken(randomString))
                .as("Random string '%s' should not be a valid token", randomString)
                .isFalse();
    }

    /**
     * Property 21.7: Public endpoints are accessible without authentication
     * 
     * **Feature: production-deployment-integration, Property 21: 权限验证正确性**
     * 
     * For public endpoints (like /api/auth/login), requests without authentication
     * SHALL be accepted (not return 401 or 403).
     * 
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 10)
    void publicEndpoints_shouldBeAccessibleWithoutAuth() throws Exception {
        // Public endpoints that should be accessible without authentication
        List<String> publicEndpoints = List.of(
                "/api/auth/login",
                "/actuator/health"
        );

        for (String endpoint : publicEndpoints) {
            // Note: POST endpoints may return 400 for missing body, but not 401/403
            if (endpoint.equals("/api/auth/login")) {
                // Login endpoint requires POST with body
                mockMvc.perform(post(endpoint)
                                .contentType("application/json")
                                .content("{\"username\":\"test\",\"password\":\"test\"}"))
                        .andExpect(result -> {
                            int status = result.getResponse().getStatus();
                            // Should not be 403 Forbidden (401 is OK for wrong credentials)
                            assertThat(status)
                                    .as("Public endpoint %s should not return 403", endpoint)
                                    .isNotEqualTo(403);
                        });
            } else {
                // GET endpoints
                mockMvc.perform(get(endpoint))
                        .andExpect(result -> {
                            int status = result.getResponse().getStatus();
                            assertThat(status)
                                    .as("Public endpoint %s should be accessible, but returned %d", 
                                        endpoint, status)
                                    .isNotIn(401, 403);
                        });
            }
        }
    }

    /**
     * Property 21.8: Token expiration is properly set
     * 
     * **Feature: production-deployment-integration, Property 21: 权限验证正确性**
     * 
     * For any generated token, the expiration date SHALL be in the future
     * and within a reasonable time frame (not more than 30 days).
     * 
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 50)
    void tokenExpiration_shouldBeWithinReasonableTimeframe() {
        // Get test user
        AppUser testUser = getTestUser();
        assumeThat(testUser).isNotNull();
        assumeThat(testUser.getOrg()).isNotNull();

        // Generate token
        String token = generateValidToken(testUser);

        // Extract expiration
        java.util.Date expiration = jwtUtil.extractExpiration(token);
        java.util.Date now = new java.util.Date();
        
        // Calculate max allowed expiration (30 days from now)
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
        java.util.Date maxExpiration = new java.util.Date(now.getTime() + thirtyDaysInMillis);

        // Assert: Expiration should be in the future
        assertThat(expiration)
                .as("Token expiration should be in the future")
                .isAfter(now);

        // Assert: Expiration should not be more than 30 days
        assertThat(expiration)
                .as("Token expiration should not exceed 30 days")
                .isBefore(maxExpiration);
    }
}
