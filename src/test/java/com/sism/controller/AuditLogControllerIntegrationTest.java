package com.sism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.LoginRequest;
import com.sism.entity.AppUser;
import com.sism.entity.AuditLog;
import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;
import com.sism.repository.AuditLogRepository;
import com.sism.repository.OrgRepository;
import com.sism.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuditLogController
 * Tests audit log query and filtering endpoints
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private AppUser testUser;
    private AuditLog testAuditLog;

    @BeforeEach
    void setUp() throws Exception {
        // Get or create test user and login
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            AppUser user = new AppUser();
            user.setUsername("testuser");
            user.setPasswordHash(passwordEncoder.encode("testPassword123"));
            user.setRealName("Test User");
            user.setIsActive(true);
            user.setOrg(orgRepository.findAll().stream().findFirst().orElseThrow());
            return userRepository.save(user);
        });

        // Get or create test audit log
        testAuditLog = auditLogRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    AuditLog log = new AuditLog();
                    log.setEntityType(AuditEntityType.INDICATOR);
                    log.setEntityId(1L);
                    log.setAction(AuditAction.CREATE);
                    log.setActorUser(testUser);
                    log.setActorOrg(testUser.getOrg());
                    log.setReason("Test audit log");
                    log.setCreatedAt(LocalDateTime.now());
                    return auditLogRepository.save(log);
                });

        // Login to get token
        authToken = loginAndGetToken(testUser.getUsername(), "testPassword123");
    }

    @Nested
    @DisplayName("GET /api/audit-logs")
    class QueryAuditLogsTests {

        @Test
        @DisplayName("Should return audit logs with pagination")
        void shouldReturnAuditLogsWithPagination() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("page", "0")
                            .param("size", "10")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("Should filter audit logs by entity type")
        void shouldFilterByEntityType() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("entityType", AuditEntityType.INDICATOR.name())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should filter audit logs by action")
        void shouldFilterByAction() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("action", AuditAction.CREATE.name())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs/entity-type/{entityType}")
    class GetAuditLogsByEntityTypeTests {

        @Test
        @DisplayName("Should return audit logs by entity type")
        void shouldReturnAuditLogsByEntityType() throws Exception {
            mockMvc.perform(get("/api/audit-logs/entity-type/{entityType}", AuditEntityType.INDICATOR)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs/action/{action}")
    class GetAuditLogsByActionTests {

        @Test
        @DisplayName("Should return audit logs by action")
        void shouldReturnAuditLogsByAction() throws Exception {
            mockMvc.perform(get("/api/audit-logs/action/{action}", AuditAction.CREATE)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs/time-range")
    class GetAuditLogsByTimeRangeTests {

        @Test
        @DisplayName("Should return audit logs by time range")
        void shouldReturnAuditLogsByTimeRange() throws Exception {
            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            mockMvc.perform(get("/api/audit-logs/time-range")
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs/trail/{entityType}/{entityId}")
    class GetAuditTrailTests {

        @Test
        @DisplayName("Should return audit trail for entity")
        void shouldReturnAuditTrailForEntity() throws Exception {
            mockMvc.perform(get("/api/audit-logs/trail/{entityType}/{entityId}", 
                            AuditEntityType.INDICATOR, testAuditLog.getEntityId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs/user/{userId}/recent")
    class GetRecentAuditLogsByUserTests {

        @Test
        @DisplayName("Should return recent audit logs by user")
        void shouldReturnRecentAuditLogsByUser() throws Exception {
            mockMvc.perform(get("/api/audit-logs/user/{userId}/recent", testUser.getUserId())
                            .param("limit", "10")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs/search")
    class SearchAuditLogsTests {

        @Test
        @DisplayName("Should search audit logs by keyword")
        void shouldSearchAuditLogsByKeyword() throws Exception {
            mockMvc.perform(get("/api/audit-logs/search")
                            .param("keyword", "Test")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs/{logId}/differences")
    class GetFormattedDifferencesTests {

        @Test
        @DisplayName("Should return formatted differences for audit log")
        void shouldReturnFormattedDifferences() throws Exception {
            mockMvc.perform(get("/api/audit-logs/{logId}/differences", testAuditLog.getLogId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var response = objectMapper.readTree(responseBody);
        return response.get("data").get("token").asText();
    }
}
