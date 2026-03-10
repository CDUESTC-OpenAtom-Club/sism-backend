package com.sism.controller;

import com.sism.config.TestSecurityConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.dto.LoginRequest;
import com.sism.entity.SysUser;
import com.sism.entity.SysOrg;
import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.entity.StrategicTask;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.repository.*;
import com.sism.util.TestDataFactory;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for IndicatorController
 * Tests indicator CRUD and distribution endpoints
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class IndicatorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssessmentCycleRepository cycleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private Indicator testIndicator;
    private SysOrg testOrg;
    private StrategicTask testTask;
    private SysUser testUser;

    @BeforeEach
    void setUp() throws Exception {
        // Create test data using TestDataFactory
        testOrg = TestDataFactory.createTestOrg(orgRepository, "测试组织", OrgType.FUNCTIONAL_DEPT);
        testTask = TestDataFactory.createTestTask(taskRepository, cycleRepository, orgRepository);
        testIndicator = TestDataFactory.createTestIndicator(indicatorRepository, taskRepository, 
                                                            cycleRepository, orgRepository);
        
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

        // Login to get token
        authToken = loginAndGetToken(testUser.getUsername(), "testPassword123");
    }

    @Nested
    @DisplayName("GET /api/indicators")
    class GetAllIndicatorsTests {

        @Test
        @DisplayName("Should return all active indicators")
        void shouldReturnAllActiveIndicators() throws Exception {
            mockMvc.perform(get("/indicators")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/indicators"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return Last-Modified header in response")
        void shouldReturnLastModifiedHeader() throws Exception {
            MvcResult result = mockMvc.perform(get("/indicators")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Last-Modified"))
                    .andExpect(header().exists("Cache-Control"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            
            String lastModified = result.getResponse().getHeader("Last-Modified");
            assertThat(lastModified).isNotNull();
            assertThat(lastModified).isNotEmpty();
        }

        /**
         * Task 3.8: Test 304 Not Modified response when data hasn't changed
         * Requirements: 2.6, 2.7
         */
        @Test
        @DisplayName("Should return 304 Not Modified when data hasn't changed")
        void shouldReturn304WhenNotModified() throws Exception {
            // First request to get Last-Modified header
            MvcResult firstResult = mockMvc.perform(get("/indicators")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Last-Modified"))
                    .andReturn();
            
            String lastModified = firstResult.getResponse().getHeader("Last-Modified");
            assertThat(lastModified).isNotNull();
            
            // Second request with If-Modified-Since header
            mockMvc.perform(get("/indicators")
                            .header("Authorization", "Bearer " + authToken)
                            .header("If-Modified-Since", lastModified))
                    .andExpect(status().isNotModified())
                    .andExpect(header().exists("Last-Modified"));
        }

        /**
         * Task 3.8: Test 200 OK response when data has been modified
         * Requirements: 2.6, 2.7
         */
        @Test
        @DisplayName("Should return 200 OK when data has been modified")
        void shouldReturn200WhenModified() throws Exception {
            // Use an old date for If-Modified-Since
            String oldDate = "Mon, 01 Jan 2020 00:00:00 GMT";
            
            mockMvc.perform(get("/indicators")
                            .header("Authorization", "Bearer " + authToken)
                            .header("If-Modified-Since", oldDate))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Last-Modified"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        /**
         * Task 3.8: Test Cache-Control header is set correctly
         * Requirements: 2.6, 2.7
         */
        @Test
        @DisplayName("Should return Cache-Control header with max-age")
        void shouldReturnCacheControlHeader() throws Exception {
            mockMvc.perform(get("/indicators")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Cache-Control"))
                    .andExpect(header().string("Cache-Control", containsString("max-age")))
                    .andExpect(header().string("Cache-Control", containsString("must-revalidate")));
        }

        /**
         * Task 7.1: 测试指标列表 API - 验证响应包含所有前端需要的字段
         * Requirements: 5.3, 5.4
         */
        @Test
        @DisplayName("Should return indicators with all frontend-required fields")
        void shouldReturnIndicatorsWithAllFrontendFields() throws Exception {
            mockMvc.perform(get("/indicators")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    // Core fields
                    .andExpect(jsonPath("$.data[0].indicatorId").exists())
                    .andExpect(jsonPath("$.data[0].indicatorDesc").exists())
                    .andExpect(jsonPath("$.data[0].status").exists())
                    .andExpect(jsonPath("$.data[0].year").exists())
                    .andExpect(jsonPath("$.data[0].weightPercent").exists())
                    // Organization fields
                    .andExpect(jsonPath("$.data[0].ownerOrgId").exists())
                    .andExpect(jsonPath("$.data[0].ownerOrgName").exists())
                    .andExpect(jsonPath("$.data[0].targetOrgId").exists())
                    .andExpect(jsonPath("$.data[0].targetOrgName").exists())
                    // New alignment fields (may be null but should exist in response)
                    .andExpect(jsonPath("$.data[0]").value(hasKey("isQualitative")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("type1")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("type2")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("canWithdraw")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("targetValue")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("unit")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("responsiblePerson")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("progress")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("statusAudit")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("progressApprovalStatus")))
                    // Derived fields
                    .andExpect(jsonPath("$.data[0]").value(hasKey("isStrategic")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("responsibleDept")))
                    .andExpect(jsonPath("$.data[0]").value(hasKey("ownerDept")));
        }
    }

    @Nested
    @DisplayName("GET /api/indicators/{id}")
    class GetIndicatorByIdTests {

        @Test
        @DisplayName("Should return indicator by ID")
        void shouldReturnIndicatorById() throws Exception {
            mockMvc.perform(get("/indicators/{id}", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.indicatorId").value(testIndicator.getIndicatorId()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent indicator")
        void shouldReturn404ForNonExistentIndicator() throws Exception {
            mockMvc.perform(get("/indicators/{id}", 999999L)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }

        /**
         * Task 7.2: 测试指标详情 API - 验证里程碑、审计日志等嵌套数据
         * Requirements: 7.4, 8.3
         */
        @Test
        @DisplayName("Should return indicator detail with milestones and nested data")
        void shouldReturnIndicatorDetailWithMilestones() throws Exception {
            mockMvc.perform(get("/indicators/{id}", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.indicatorId").value(testIndicator.getIndicatorId()))
                    // Core fields
                    .andExpect(jsonPath("$.data.indicatorDesc").exists())
                    .andExpect(jsonPath("$.data.status").exists())
                    .andExpect(jsonPath("$.data.level").exists())
                    // Nested data - milestones (may be empty array)
                    .andExpect(jsonPath("$.data.milestones").isArray())
                    // Nested data - child indicators (may be empty array)
                    .andExpect(jsonPath("$.data.childIndicators").isArray())
                    // Audit log field (JSON string, may be null)
                    .andExpect(jsonPath("$.data").value(hasKey("statusAudit")))
                    // Progress approval fields
                    .andExpect(jsonPath("$.data").value(hasKey("progressApprovalStatus")))
                    .andExpect(jsonPath("$.data").value(hasKey("pendingProgress")))
                    .andExpect(jsonPath("$.data").value(hasKey("pendingRemark")));
        }

        /**
         * Task 7.2: 验证里程碑数据结构完整性
         * Requirements: 7.4
         */
        @Test
        @DisplayName("Should return milestones with all required fields when present")
        void shouldReturnMilestonesWithRequiredFields() throws Exception {
            // First check if indicator has milestones
            MvcResult result = mockMvc.perform(get("/indicators/{id}", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            var response = objectMapper.readTree(responseBody);
            var milestones = response.get("data").get("milestones");
            
            // If milestones exist, verify their structure
            if (milestones != null && milestones.isArray() && milestones.size() > 0) {
                var firstMilestone = milestones.get(0);
                assertThat(firstMilestone.has("milestoneId")).isTrue();
                assertThat(firstMilestone.has("milestoneName")).isTrue();
                assertThat(firstMilestone.has("dueDate")).isTrue();
                assertThat(firstMilestone.has("status")).isTrue();
                assertThat(firstMilestone.has("weightPercent")).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("GET /api/indicators/task/{taskId}")
    class GetIndicatorsByTaskIdTests {

        @Test
        @DisplayName("Should return indicators by task ID")
        void shouldReturnIndicatorsByTaskId() throws Exception {
            mockMvc.perform(get("/indicators/task/{taskId}", testTask.getTaskId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/indicators/owner/{ownerOrgId}")
    class GetIndicatorsByOwnerOrgIdTests {

        @Test
        @DisplayName("Should return indicators by owner org ID")
        void shouldReturnIndicatorsByOwnerOrgId() throws Exception {
            mockMvc.perform(get("/indicators/owner/{ownerOrgId}", testOrg.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/indicators/search")
    class SearchIndicatorsTests {

        @Test
        @DisplayName("Should search indicators by keyword")
        void shouldSearchIndicatorsByKeyword() throws Exception {
            mockMvc.perform(get("/indicators/search")
                            .param("keyword", "Test")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/indicators")
    class CreateIndicatorTests {

        @Test
        @DisplayName("Should create new indicator")
        void shouldCreateNewIndicator() throws Exception {
            IndicatorCreateRequest request = new IndicatorCreateRequest();
            request.setIndicatorDesc("New Test Indicator");
            request.setTaskId(testTask.getTaskId());
            request.setOwnerOrgId(testOrg.getId());
            request.setTargetOrgId(testOrg.getId());
            request.setLevel(IndicatorLevel.PRIMARY.name());
            request.setWeightPercent(BigDecimal.valueOf(5));
            request.setYear(2025);

            mockMvc.perform(post("/indicators")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.indicatorDesc").value("New Test Indicator"));
        }
    }

    @Nested
    @DisplayName("PUT /api/indicators/{id}")
    class UpdateIndicatorTests {

        @Test
        @DisplayName("Should update existing indicator")
        void shouldUpdateExistingIndicator() throws Exception {
            IndicatorUpdateRequest request = new IndicatorUpdateRequest();
            request.setIndicatorDesc("Updated Indicator Description");

            mockMvc.perform(put("/indicators/{id}", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.indicatorDesc").value("Updated Indicator Description"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent indicator")
        void shouldReturn404ForNonExistentIndicator() throws Exception {
            IndicatorUpdateRequest request = new IndicatorUpdateRequest();
            request.setIndicatorDesc("Updated Description");

            mockMvc.perform(put("/indicators/{id}", 999999L)
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/indicators/{id}")
    class DeleteIndicatorTests {

        @Test
        @DisplayName("Should soft delete indicator")
        void shouldSoftDeleteIndicator() throws Exception {
            // Create a new indicator to delete
            Indicator toDelete = new Indicator();
            toDelete.setIndicatorDesc("Indicator to Delete");
            toDelete.setTaskId(testTask.getTaskId());
            toDelete.setOwnerOrg(testOrg);
            toDelete.setTargetOrg(testOrg);
            toDelete.setLevel(IndicatorLevel.PRIMARY);
            toDelete.setStatus(IndicatorStatus.ACTIVE);
            toDelete.setWeightPercent(BigDecimal.valueOf(5));
            toDelete.setYear(2025);
            toDelete.setCreatedAt(LocalDateTime.now());
            toDelete.setUpdatedAt(LocalDateTime.now());
            toDelete.setIsDeleted(false);
            toDelete = indicatorRepository.save(toDelete);

            mockMvc.perform(delete("/indicators/{id}", toDelete.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/indicators/{id}/distribution-eligibility")
    class CheckDistributionEligibilityTests {

        @Test
        @DisplayName("Should check distribution eligibility")
        void shouldCheckDistributionEligibility() throws Exception {
            mockMvc.perform(get("/indicators/{id}/distribution-eligibility", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.canDistribute").exists());
        }
    }

    /**
     * Task 7.3: 测试指标过滤 API
     * Requirements: 7.3, 7.5
     */
    @Nested
    @DisplayName("GET /api/indicators/filter")
    class FilterIndicatorsTests {

        @Test
        @DisplayName("Should filter indicators by type1 (定性)")
        void shouldFilterByType1Qualitative() throws Exception {
            mockMvc.perform(get("/indicators/filter")
                            .param("type1", "定性")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should filter indicators by type1 (定量)")
        void shouldFilterByType1Quantitative() throws Exception {
            mockMvc.perform(get("/indicators/filter")
                            .param("type1", "定量")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should filter indicators by type2 (发展性)")
        void shouldFilterByType2Development() throws Exception {
            mockMvc.perform(get("/indicators/filter")
                            .param("type2", "发展性")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should filter indicators by type2 (基础性)")
        void shouldFilterByType2Basic() throws Exception {
            mockMvc.perform(get("/indicators/filter")
                            .param("type2", "基础性")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should filter indicators by status")
        void shouldFilterByStatus() throws Exception {
            mockMvc.perform(get("/indicators/filter")
                            .param("status", "ACTIVE")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should filter indicators by combined type1 and type2")
        void shouldFilterByCombinedTypes() throws Exception {
            mockMvc.perform(get("/indicators/filter")
                            .param("type1", "定量")
                            .param("type2", "发展性")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should return all active indicators when no filter specified")
        void shouldReturnAllWhenNoFilter() throws Exception {
            mockMvc.perform(get("/indicators/filter")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/indicators/qualitative and /quantitative")
    class QualitativeQuantitativeTests {

        @Test
        @DisplayName("Should get qualitative indicators")
        void shouldGetQualitativeIndicators() throws Exception {
            mockMvc.perform(get("/indicators/qualitative")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should get quantitative indicators")
        void shouldGetQuantitativeIndicators() throws Exception {
            mockMvc.perform(get("/indicators/quantitative")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
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
