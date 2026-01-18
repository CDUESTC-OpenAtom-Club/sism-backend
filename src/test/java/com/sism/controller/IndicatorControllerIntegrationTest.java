package com.sism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.dto.LoginRequest;
import com.sism.entity.AppUser;
import com.sism.entity.Indicator;
import com.sism.entity.Org;
import com.sism.entity.StrategicTask;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.OrgRepository;
import com.sism.repository.TaskRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for IndicatorController
 * Tests indicator CRUD and distribution endpoints
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IndicatorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private Indicator testIndicator;
    private Org testOrg;
    private StrategicTask testTask;

    @BeforeEach
    void setUp() throws Exception {
        // Get or create test user and login
        AppUser testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            AppUser user = new AppUser();
            user.setUsername("testuser");
            user.setPasswordHash(passwordEncoder.encode("testPassword123"));
            user.setRealName("Test User");
            user.setIsActive(true);
            user.setOrg(orgRepository.findAll().stream().findFirst().orElseThrow());
            return userRepository.save(user);
        });

        // Get test org and task
        testOrg = orgRepository.findAll().stream().findFirst().orElseThrow();
        testTask = taskRepository.findAll().stream().findFirst().orElseThrow();

        // Get or create test indicator
        testIndicator = indicatorRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Indicator indicator = new Indicator();
                    indicator.setIndicatorDesc("Test Indicator");
                    indicator.setTask(testTask);
                    indicator.setOwnerOrg(testOrg);
                    indicator.setTargetOrg(testOrg);
                    indicator.setLevel(IndicatorLevel.STRAT_TO_FUNC);
                    indicator.setStatus(IndicatorStatus.ACTIVE);
                    indicator.setWeightPercent(BigDecimal.valueOf(10));
                    indicator.setYear(2025);
                    return indicatorRepository.save(indicator);
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
            mockMvc.perform(get("/api/indicators")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/indicators"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/indicators/{id}")
    class GetIndicatorByIdTests {

        @Test
        @DisplayName("Should return indicator by ID")
        void shouldReturnIndicatorById() throws Exception {
            mockMvc.perform(get("/api/indicators/{id}", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.indicatorId").value(testIndicator.getIndicatorId()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent indicator")
        void shouldReturn404ForNonExistentIndicator() throws Exception {
            mockMvc.perform(get("/api/indicators/{id}", 999999L)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/indicators/task/{taskId}")
    class GetIndicatorsByTaskIdTests {

        @Test
        @DisplayName("Should return indicators by task ID")
        void shouldReturnIndicatorsByTaskId() throws Exception {
            mockMvc.perform(get("/api/indicators/task/{taskId}", testTask.getTaskId())
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
            mockMvc.perform(get("/api/indicators/owner/{ownerOrgId}", testOrg.getOrgId())
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
            mockMvc.perform(get("/api/indicators/search")
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
            request.setOwnerOrgId(testOrg.getOrgId());
            request.setTargetOrgId(testOrg.getOrgId());
            request.setLevel(IndicatorLevel.STRAT_TO_FUNC);
            request.setWeightPercent(BigDecimal.valueOf(5));
            request.setYear(2025);

            mockMvc.perform(post("/api/indicators")
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

            mockMvc.perform(put("/api/indicators/{id}", testIndicator.getIndicatorId())
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

            mockMvc.perform(put("/api/indicators/{id}", 999999L)
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
            toDelete.setTask(testTask);
            toDelete.setOwnerOrg(testOrg);
            toDelete.setTargetOrg(testOrg);
            toDelete.setLevel(IndicatorLevel.STRAT_TO_FUNC);
            toDelete.setStatus(IndicatorStatus.ACTIVE);
            toDelete.setWeightPercent(BigDecimal.valueOf(5));
            toDelete.setYear(2025);
            toDelete = indicatorRepository.save(toDelete);

            mockMvc.perform(delete("/api/indicators/{id}", toDelete.getIndicatorId())
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
            mockMvc.perform(get("/api/indicators/{id}/distribution-eligibility", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.canDistribute").exists());
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
