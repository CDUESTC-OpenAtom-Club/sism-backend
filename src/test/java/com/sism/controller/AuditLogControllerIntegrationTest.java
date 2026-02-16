package com.sism.controller;

import com.sism.config.TestSecurityConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.LoginRequest;
import com.sism.entity.SysUser;
import com.sism.entity.SysOrg;
import com.sism.entity.AuditLog;
import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;
import com.sism.repository.AuditLogRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.repository.SysUserRepository;
import com.sism.util.TestDataFactory;
import com.sism.enums.OrgType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class AuditLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private SysUser testUser;
    private AuditLog testAuditLog;

    @BeforeEach
    void setUp() throws Exception {
        // Create test data using TestDataFactory
        SysOrg testOrg = TestDataFactory.createTestOrg(orgRepository, "测试组织", OrgType.FUNCTIONAL_DEPT);
        
        // Create test user with encoded password
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            SysUser user = new SysUser();
            user.setUsername("testuser");
            user.setPasswordHash(passwordEncoder.encode("testPassword123"));
            user.setRealName("Test User");
            user.setIsActive(true);
            user.setOrg(testOrg);
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
            mockMvc.perform(get("/audit-logs")
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
            mockMvc.perform(get("/audit-logs")
                            .param("entityType", AuditEntityType.INDICATOR.name())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should filter audit logs by action")
        void shouldFilterByAction() throws Exception {
            mockMvc.perform(get("/audit-logs")
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
            mockMvc.perform(get("/audit-logs/entity-type/{entityType}", AuditEntityType.INDICATOR)
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
            mockMvc.perform(get("/audit-logs/action/{action}", AuditAction.CREATE)
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

            mockMvc.perform(get("/audit-logs/time-range")
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
            mockMvc.perform(get("/audit-logs/trail/{entityType}/{entityId}", 
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
            mockMvc.perform(get("/audit-logs/user/{userId}/recent", testUser.getId())
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
            mockMvc.perform(get("/audit-logs/search")
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
            mockMvc.perform(get("/audit-logs/{logId}/differences", testAuditLog.getLogId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var response = objectMapper.readTree(responseBody);
        return response.get("data").get("token").asText();
    }
}
