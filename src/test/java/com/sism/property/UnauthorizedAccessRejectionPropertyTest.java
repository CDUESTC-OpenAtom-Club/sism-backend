package com.sism.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.dto.MilestoneCreateRequest;
import com.sism.dto.MilestoneUpdateRequest;
import com.sism.entity.AppUser;
import com.sism.entity.Indicator;
import com.sism.entity.Milestone;
import com.sism.entity.Org;
import com.sism.entity.StrategicTask;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.MilestoneStatus;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.MilestoneRepository;
import com.sism.repository.OrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.repository.UserRepository;
import com.sism.util.JwtUtil;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Property-based tests for Unauthorized Access Rejection
 * 
 * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
 * 
 * For any unauthorized access attempt (missing token, invalid token, or expired token),
 * the system SHALL return 403 Forbidden or 401 Unauthorized status code.
 * 
 * **Validates: Requirements 12.4**
 */
@JqwikSpringSupport
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UnauthorizedAccessRejectionPropertyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private TaskRepository taskRepository;

    // ==================== Protected Endpoints ====================

    /**
     * Protected GET endpoints that require authentication
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

    /**
     * Protected POST endpoints that require authentication
     */
    private static final List<String> PROTECTED_POST_ENDPOINTS = List.of(
            "/api/indicators",
            "/api/milestones",
            "/api/adhoc-tasks",
            "/api/reports"
    );

    /**
     * Invalid token patterns that should be rejected
     */
    private static final List<String> INVALID_TOKEN_PATTERNS = List.of(
            "",                                    // Empty token
            "invalid",                             // Plain invalid string
            "invalid.token.format",                // Invalid JWT format
            "eyJhbGciOiJIUzI1NiJ9.invalid.sig",   // Malformed JWT
            "null",                                // Literal null string
            "   ",                                 // Whitespace only
            "Bearer invalid",                      // With Bearer prefix (shouldn't be in token)
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxfQ.invalid", // Expired/invalid signature
            "abc123xyz",                           // Random alphanumeric
            "SELECT * FROM users",                 // SQL injection attempt
            "<script>alert('xss')</script>"        // XSS attempt
    );

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> protectedGetEndpoints() {
        return Arbitraries.of(PROTECTED_GET_ENDPOINTS);
    }

    @Provide
    Arbitrary<String> protectedPostEndpoints() {
        return Arbitraries.of(PROTECTED_POST_ENDPOINTS);
    }

    @Provide
    Arbitrary<String> invalidTokenPatterns() {
        return Arbitraries.of(INVALID_TOKEN_PATTERNS);
    }

    @Provide
    Arbitrary<String> randomInvalidTokens() {
        return Arbitraries.oneOf(
                // Random alphanumeric strings
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                // Random strings with special characters
                Arbitraries.strings().ascii().ofMinLength(1).ofMaxLength(100),
                // Fake JWT-like strings (3 parts separated by dots)
                Arbitraries.strings().alpha().ofLength(10)
                        .flatMap(part1 -> Arbitraries.strings().alpha().ofLength(10)
                                .flatMap(part2 -> Arbitraries.strings().alpha().ofLength(10)
                                        .map(part3 -> part1 + "." + part2 + "." + part3)))
        );
    }

    @Provide
    Arbitrary<String> httpMethods() {
        return Arbitraries.of("GET", "POST", "PUT", "DELETE");
    }

    // ==================== Helper Methods ====================

    private AppUser getTestUser() {
        return userRepository.findAll().stream()
                .filter(u -> u.getOrg() != null && u.getIsActive())
                .findFirst()
                .orElse(null);
    }

    private Org getTestOrg() {
        return orgRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }

    private StrategicTask getTestTask() {
        return taskRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }

    private Indicator getTestIndicator() {
        return indicatorRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }

    private Milestone getTestMilestone() {
        return milestoneRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }

    // ==================== Property Tests ====================

    /**
     * Property 22.1: Missing Authorization header results in 403 Forbidden for GET endpoints
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any protected GET endpoint, a request without an Authorization header
     * SHALL be rejected with 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 50)
    void missingAuthHeader_shouldReturn403ForGetEndpoints(
            @ForAll("protectedGetEndpoints") String endpoint) throws Exception {

        // Act & Assert: Request without Authorization header should return 403
        mockMvc.perform(get(endpoint))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("GET %s without auth should return 403, but returned %d", endpoint, status)
                            .isEqualTo(403);
                });
    }

    /**
     * Property 22.2: Missing Authorization header results in 403 Forbidden for POST endpoints
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any protected POST endpoint, a request without an Authorization header
     * SHALL be rejected with 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 50)
    void missingAuthHeader_shouldReturn403ForPostEndpoints(
            @ForAll("protectedPostEndpoints") String endpoint) throws Exception {

        // Act & Assert: Request without Authorization header should return 403
        mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("POST %s without auth should return 403, but returned %d", endpoint, status)
                            .isEqualTo(403);
                });
    }

    /**
     * Property 22.3: Invalid token patterns result in 401 or 403 for GET endpoints
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any protected GET endpoint and known invalid token pattern,
     * the request SHALL be rejected with 401 Unauthorized or 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 100)
    void invalidTokenPatterns_shouldBeRejectedForGetEndpoints(
            @ForAll("protectedGetEndpoints") String endpoint,
            @ForAll("invalidTokenPatterns") String invalidToken) throws Exception {

        // Act & Assert: Request with invalid token should return 401 or 403
        mockMvc.perform(get(endpoint)
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("GET %s with invalid token '%s' should return 401 or 403, but returned %d",
                                    endpoint, invalidToken, status)
                            .isIn(401, 403);
                });
    }

    /**
     * Property 22.4: Random invalid tokens result in 401 or 403 for GET endpoints
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any protected GET endpoint and randomly generated invalid token,
     * the request SHALL be rejected with 401 Unauthorized or 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 100)
    void randomInvalidTokens_shouldBeRejectedForGetEndpoints(
            @ForAll("protectedGetEndpoints") String endpoint,
            @ForAll("randomInvalidTokens") String randomToken) throws Exception {

        // Act & Assert: Request with random invalid token should return 401 or 403
        mockMvc.perform(get(endpoint)
                        .header("Authorization", "Bearer " + randomToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("GET %s with random token should return 401 or 403, but returned %d",
                                    endpoint, status)
                            .isIn(401, 403);
                });
    }

    /**
     * Property 22.5: Invalid token patterns result in 401 or 403 for POST endpoints
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any protected POST endpoint and known invalid token pattern,
     * the request SHALL be rejected with 401 Unauthorized or 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 100)
    void invalidTokenPatterns_shouldBeRejectedForPostEndpoints(
            @ForAll("protectedPostEndpoints") String endpoint,
            @ForAll("invalidTokenPatterns") String invalidToken) throws Exception {

        // Act & Assert: Request with invalid token should return 401 or 403
        mockMvc.perform(post(endpoint)
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("POST %s with invalid token '%s' should return 401 or 403, but returned %d",
                                    endpoint, invalidToken, status)
                            .isIn(401, 403);
                });
    }

    /**
     * Property 22.6: PUT requests without auth are rejected
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any PUT request to a protected resource endpoint without authentication,
     * the request SHALL be rejected with 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 50)
    void putRequestsWithoutAuth_shouldBeRejected() throws Exception {
        Indicator indicator = getTestIndicator();
        assumeThat(indicator).isNotNull();

        // Act & Assert: PUT request without auth should return 403
        mockMvc.perform(put("/api/indicators/{id}", indicator.getIndicatorId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("PUT /api/indicators/{id} without auth should return 403, but returned %d", status)
                            .isEqualTo(403);
                });
    }

    /**
     * Property 22.7: DELETE requests without auth are rejected
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any DELETE request to a protected resource endpoint without authentication,
     * the request SHALL be rejected with 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 50)
    void deleteRequestsWithoutAuth_shouldBeRejected() throws Exception {
        Indicator indicator = getTestIndicator();
        assumeThat(indicator).isNotNull();

        // Act & Assert: DELETE request without auth should return 403
        mockMvc.perform(delete("/api/indicators/{id}", indicator.getIndicatorId()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("DELETE /api/indicators/{id} without auth should return 403, but returned %d", status)
                            .isEqualTo(403);
                });
    }

    /**
     * Property 22.8: PUT requests with invalid token are rejected
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any PUT request to a protected resource endpoint with an invalid token,
     * the request SHALL be rejected with 401 Unauthorized or 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 50)
    void putRequestsWithInvalidToken_shouldBeRejected(
            @ForAll("invalidTokenPatterns") String invalidToken) throws Exception {
        Indicator indicator = getTestIndicator();
        assumeThat(indicator).isNotNull();

        // Act & Assert: PUT request with invalid token should return 401 or 403
        mockMvc.perform(put("/api/indicators/{id}", indicator.getIndicatorId())
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("PUT with invalid token should return 401 or 403, but returned %d", status)
                            .isIn(401, 403);
                });
    }

    /**
     * Property 22.9: DELETE requests with invalid token are rejected
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any DELETE request to a protected resource endpoint with an invalid token,
     * the request SHALL be rejected with 401 Unauthorized or 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 50)
    void deleteRequestsWithInvalidToken_shouldBeRejected(
            @ForAll("invalidTokenPatterns") String invalidToken) throws Exception {
        Indicator indicator = getTestIndicator();
        assumeThat(indicator).isNotNull();

        // Act & Assert: DELETE request with invalid token should return 401 or 403
        mockMvc.perform(delete("/api/indicators/{id}", indicator.getIndicatorId())
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("DELETE with invalid token should return 401 or 403, but returned %d", status)
                            .isIn(401, 403);
                });
    }

    /**
     * Property 22.10: Malformed Authorization header is rejected
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any request with a malformed Authorization header (not starting with "Bearer "),
     * the request SHALL be rejected with 401 Unauthorized or 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 50)
    void malformedAuthHeader_shouldBeRejected(
            @ForAll("protectedGetEndpoints") String endpoint) throws Exception {

        // Test various malformed Authorization header formats
        List<String> malformedHeaders = List.of(
                "Basic dXNlcjpwYXNz",           // Basic auth instead of Bearer
                "Token abc123",                  // Wrong prefix
                "bearer abc123",                 // Lowercase bearer
                "BEARER abc123",                 // Uppercase BEARER
                "abc123",                        // No prefix at all
                "Bearer",                        // Bearer without token
                "Bearer "                        // Bearer with only space
        );

        for (String malformedHeader : malformedHeaders) {
            mockMvc.perform(get(endpoint)
                            .header("Authorization", malformedHeader))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertThat(status)
                                .as("GET %s with malformed auth header '%s' should return 401 or 403, but returned %d",
                                        endpoint, malformedHeader, status)
                                .isIn(401, 403);
                    });
        }
    }

    /**
     * Property 22.11: Unauthorized access to specific resource IDs is rejected
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any request to access a specific resource by ID without authentication,
     * the request SHALL be rejected with 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 30)
    void unauthorizedAccessToResourceById_shouldBeRejected() throws Exception {
        Indicator indicator = getTestIndicator();
        Milestone milestone = getTestMilestone();
        StrategicTask task = getTestTask();
        Org org = getTestOrg();

        // Test unauthorized access to various resource endpoints
        if (indicator != null) {
            mockMvc.perform(get("/api/indicators/{id}", indicator.getIndicatorId()))
                    .andExpect(status().isForbidden());
        }

        if (milestone != null) {
            mockMvc.perform(get("/api/milestones/{id}", milestone.getMilestoneId()))
                    .andExpect(status().isForbidden());
        }

        if (task != null) {
            mockMvc.perform(get("/api/tasks/{id}", task.getTaskId()))
                    .andExpect(status().isForbidden());
        }

        if (org != null) {
            mockMvc.perform(get("/api/orgs/{id}", org.getOrgId()))
                    .andExpect(status().isForbidden());
        }
    }

    /**
     * Property 22.12: Unauthorized modification attempts are rejected
     * 
     * **Feature: production-deployment-integration, Property 22: 越权访问拒绝**
     * 
     * For any attempt to modify a resource (POST/PUT/DELETE) without authentication,
     * the request SHALL be rejected with 403 Forbidden status.
     * 
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 30)
    void unauthorizedModificationAttempts_shouldBeRejected() throws Exception {
        Indicator indicator = getTestIndicator();
        Org org = getTestOrg();
        StrategicTask task = getTestTask();

        assumeThat(indicator).isNotNull();
        assumeThat(org).isNotNull();
        assumeThat(task).isNotNull();

        // Test unauthorized POST (create)
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setIndicatorDesc("Unauthorized Create Attempt");
        createRequest.setTaskId(task.getTaskId());
        createRequest.setOwnerOrgId(org.getOrgId());
        createRequest.setTargetOrgId(org.getOrgId());
        createRequest.setLevel(IndicatorLevel.STRAT_TO_FUNC);
        createRequest.setWeightPercent(BigDecimal.valueOf(10));
        createRequest.setYear(2025);

        mockMvc.perform(post("/api/indicators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        // Test unauthorized PUT (update)
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        updateRequest.setIndicatorDesc("Unauthorized Update Attempt");

        mockMvc.perform(put("/api/indicators/{id}", indicator.getIndicatorId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // Test unauthorized DELETE
        mockMvc.perform(delete("/api/indicators/{id}", indicator.getIndicatorId()))
                .andExpect(status().isForbidden());
    }
}
