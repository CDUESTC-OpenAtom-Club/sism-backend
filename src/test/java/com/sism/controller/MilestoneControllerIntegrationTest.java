package com.sism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.LoginRequest;
import com.sism.dto.MilestoneCreateRequest;
import com.sism.dto.MilestoneUpdateRequest;
import com.sism.entity.SysUser;
import com.sism.entity.Indicator;
import com.sism.entity.Milestone;
import com.sism.enums.MilestoneStatus;
import com.sism.repository.*;
import com.sism.util.TestDataFactory;
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
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MilestoneController
 * Tests milestone CRUD and pairing mechanism endpoints
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MilestoneControllerIntegrationTest {

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
    private MilestoneRepository milestoneRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssessmentCycleRepository cycleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private Indicator testIndicator;
    private Milestone testMilestone;
    private SysUser testUser;

    @BeforeEach
    void setUp() throws Exception {
        // Create test data using TestDataFactory
        testIndicator = TestDataFactory.createTestIndicator(indicatorRepository, taskRepository, 
                                                            cycleRepository, orgRepository);
        testMilestone = TestDataFactory.createTestMilestone(milestoneRepository, indicatorRepository, 
                                                            taskRepository, cycleRepository, orgRepository);
        
        // Create test user with encoded password
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            SysUser user = new SysUser();
            user.setUsername("testuser");
            user.setPasswordHash(passwordEncoder.encode("testPassword123"));
            user.setRealName("Test User");
            user.setIsActive(true);
            user.setOrg(testIndicator.getOwnerOrg());
            return userRepository.save(user);
        });

        // Login to get token
        authToken = loginAndGetToken(testUser.getUsername(), "testPassword123");
    }

    @Nested
    @DisplayName("GET /api/milestones/{id}")
    class GetMilestoneByIdTests {

        @Test
        @DisplayName("Should return milestone by ID")
        void shouldReturnMilestoneById() throws Exception {
            mockMvc.perform(get("/api/milestones/{id}", testMilestone.getMilestoneId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.milestoneId").value(testMilestone.getMilestoneId()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent milestone")
        void shouldReturn404ForNonExistentMilestone() throws Exception {
            mockMvc.perform(get("/api/milestones/{id}", 999999L)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/milestones/indicator/{indicatorId}")
    class GetMilestonesByIndicatorIdTests {

        @Test
        @DisplayName("Should return milestones by indicator ID")
        void shouldReturnMilestonesByIndicatorId() throws Exception {
            mockMvc.perform(get("/api/milestones/indicator/{indicatorId}", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/milestones/status/{status}")
    class GetMilestonesByStatusTests {

        @Test
        @DisplayName("Should return milestones by status")
        void shouldReturnMilestonesByStatus() throws Exception {
            mockMvc.perform(get("/api/milestones/status/{status}", MilestoneStatus.NOT_STARTED)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/milestones/overdue")
    class GetOverdueMilestonesTests {

        @Test
        @DisplayName("Should return overdue milestones")
        void shouldReturnOverdueMilestones() throws Exception {
            mockMvc.perform(get("/api/milestones/overdue")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/milestones/upcoming")
    class GetUpcomingMilestonesTests {

        @Test
        @DisplayName("Should return upcoming milestones")
        void shouldReturnUpcomingMilestones() throws Exception {
            mockMvc.perform(get("/api/milestones/upcoming")
                            .param("days", "30")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/milestones/indicator/{indicatorId}/weight-validation")
    class ValidateWeightsTests {

        @Test
        @DisplayName("Should validate milestone weights")
        void shouldValidateMilestoneWeights() throws Exception {
            mockMvc.perform(get("/api/milestones/indicator/{indicatorId}/weight-validation", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.isValid").exists())
                    .andExpect(jsonPath("$.data.actualSum").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/milestones")
    class CreateMilestoneTests {

        @Test
        @DisplayName("Should create new milestone")
        void shouldCreateNewMilestone() throws Exception {
            MilestoneCreateRequest request = new MilestoneCreateRequest();
            request.setMilestoneName("New Test Milestone");
            request.setIndicatorId(testIndicator.getIndicatorId());
            request.setDueDate(LocalDate.now().plusMonths(2));
            request.setTargetProgress(10);

            mockMvc.perform(post("/api/milestones")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.milestoneName").value("New Test Milestone"));
        }
    }

    @Nested
    @DisplayName("PUT /api/milestones/{id}")
    class UpdateMilestoneTests {

        @Test
        @DisplayName("Should update existing milestone")
        void shouldUpdateExistingMilestone() throws Exception {
            MilestoneUpdateRequest request = new MilestoneUpdateRequest();
            request.setMilestoneName("Updated Milestone Name");

            mockMvc.perform(put("/api/milestones/{id}", testMilestone.getMilestoneId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.milestoneName").value("Updated Milestone Name"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/milestones/{id}/status")
    class UpdateMilestoneStatusTests {

        @Test
        @DisplayName("Should update milestone status")
        void shouldUpdateMilestoneStatus() throws Exception {
            mockMvc.perform(patch("/api/milestones/{id}/status", testMilestone.getMilestoneId())
                            .param("status", MilestoneStatus.IN_PROGRESS.name())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value(MilestoneStatus.IN_PROGRESS.name()));
        }
    }

    @Nested
    @DisplayName("GET /api/milestones/indicator/{indicatorId}/pairing-status")
    class GetPairingStatusTests {

        @Test
        @DisplayName("Should return pairing status summary")
        void shouldReturnPairingStatusSummary() throws Exception {
            mockMvc.perform(get("/api/milestones/indicator/{indicatorId}/pairing-status", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.totalMilestones").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/milestones/{id}/is-paired")
    class IsMilestonePairedTests {

        @Test
        @DisplayName("Should check if milestone is paired")
        void shouldCheckIfMilestoneIsPaired() throws Exception {
            mockMvc.perform(get("/api/milestones/{id}/is-paired", testMilestone.getMilestoneId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.isPaired").exists());
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
