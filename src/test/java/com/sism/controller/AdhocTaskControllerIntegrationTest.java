package com.sism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.AdhocTaskCreateRequest;
import com.sism.dto.LoginRequest;
import com.sism.entity.AdhocTask;
import com.sism.entity.SysUser;
import com.sism.entity.AssessmentCycle;
import com.sism.entity.SysOrg;
import com.sism.enums.AdhocScopeType;
import com.sism.enums.AdhocTaskStatus;
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

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdhocTaskController
 * Tests adhoc task CRUD and status transition endpoints
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdhocTaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private AdhocTaskRepository adhocTaskRepository;

    @Autowired
    private AssessmentCycleRepository cycleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private SysUser testUser;
    private SysOrg testOrg;
    private AssessmentCycle testCycle;
    private AdhocTask testAdhocTask;

    @BeforeEach
    void setUp() throws Exception {
        // Create test data using TestDataFactory
        testCycle = TestDataFactory.createTestCycle(cycleRepository);
        testOrg = TestDataFactory.createTestOrg(orgRepository, "测试组织", com.sism.enums.OrgType.FUNCTIONAL_DEPT);
        
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

        // Create test adhoc task
        testAdhocTask = adhocTaskRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    AdhocTask task = new AdhocTask();
                    task.setTaskTitle("Test Adhoc Task");
                    task.setTaskDesc("Test adhoc task description");
                    task.setCycle(testCycle);
                    task.setCreatorOrg(testOrg);
                    task.setScopeType(AdhocScopeType.ALL_ORGS);
                    task.setStatus(AdhocTaskStatus.DRAFT);
                    task.setDueAt(LocalDate.now().plusMonths(1));
                    task.setIncludeInAlert(false);
                    return adhocTaskRepository.save(task);
                });

        // Login to get token
        authToken = loginAndGetToken(testUser.getUsername(), "testPassword123");
    }

    @Nested
    @DisplayName("GET /api/adhoc-tasks/{id}")
    class GetAdhocTaskByIdTests {

        @Test
        @DisplayName("Should return adhoc task by ID")
        void shouldReturnAdhocTaskById() throws Exception {
            mockMvc.perform(get("/api/adhoc-tasks/{id}", testAdhocTask.getAdhocTaskId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.adhocTaskId").value(testAdhocTask.getAdhocTaskId()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent adhoc task")
        void shouldReturn404ForNonExistentAdhocTask() throws Exception {
            mockMvc.perform(get("/api/adhoc-tasks/{id}", 999999L)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/adhoc-tasks/cycle/{cycleId}")
    class GetAdhocTasksByCycleIdTests {

        @Test
        @DisplayName("Should return adhoc tasks by cycle ID")
        void shouldReturnAdhocTasksByCycleId() throws Exception {
            mockMvc.perform(get("/api/adhoc-tasks/cycle/{cycleId}", testCycle.getCycleId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/adhoc-tasks/creator/{creatorOrgId}")
    class GetAdhocTasksByCreatorOrgIdTests {

        @Test
        @DisplayName("Should return adhoc tasks by creator org ID")
        void shouldReturnAdhocTasksByCreatorOrgId() throws Exception {
            mockMvc.perform(get("/api/adhoc-tasks/creator/{creatorOrgId}", testOrg.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/adhoc-tasks/status/{status}")
    class GetAdhocTasksByStatusTests {

        @Test
        @DisplayName("Should return adhoc tasks by status")
        void shouldReturnAdhocTasksByStatus() throws Exception {
            mockMvc.perform(get("/api/adhoc-tasks/status/{status}", AdhocTaskStatus.DRAFT)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/adhoc-tasks/search")
    class SearchAdhocTasksTests {

        @Test
        @DisplayName("Should search adhoc tasks by keyword")
        void shouldSearchAdhocTasksByKeyword() throws Exception {
            mockMvc.perform(get("/api/adhoc-tasks/search")
                            .param("keyword", "Test")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/adhoc-tasks/overdue")
    class GetOverdueAdhocTasksTests {

        @Test
        @DisplayName("Should return overdue adhoc tasks")
        void shouldReturnOverdueAdhocTasks() throws Exception {
            mockMvc.perform(get("/api/adhoc-tasks/overdue")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/adhoc-tasks")
    class CreateAdhocTaskTests {

        @Test
        @DisplayName("Should create new adhoc task")
        void shouldCreateNewAdhocTask() throws Exception {
            AdhocTaskCreateRequest request = new AdhocTaskCreateRequest();
            request.setTaskTitle("New Adhoc Task");
            request.setTaskDesc("New adhoc task description");
            request.setCycleId(testCycle.getCycleId());
            request.setCreatorOrgId(testOrg.getId());
            request.setScopeType(AdhocScopeType.CUSTOM);
            request.setDueAt(LocalDate.now().plusMonths(2));
            request.setTargetOrgIds(List.of(testOrg.getId()));

            mockMvc.perform(post("/api/adhoc-tasks")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.taskTitle").value("New Adhoc Task"));
        }
    }

    @Nested
    @DisplayName("POST /api/adhoc-tasks/{id}/open")
    class OpenAdhocTaskTests {

        @Test
        @DisplayName("Should open draft adhoc task")
        void shouldOpenDraftAdhocTask() throws Exception {
            // Create a fresh draft task to ensure it's in DRAFT status
            AdhocTask draftTask = new AdhocTask();
            draftTask.setTaskTitle("Draft Task to Open");
            draftTask.setTaskDesc("Draft task description");
            draftTask.setCycle(testCycle);
            draftTask.setCreatorOrg(testOrg);
            draftTask.setScopeType(AdhocScopeType.ALL_ORGS);
            draftTask.setStatus(AdhocTaskStatus.DRAFT);
            draftTask.setDueAt(LocalDate.now().plusMonths(1));
            draftTask.setIncludeInAlert(false);
            draftTask = adhocTaskRepository.save(draftTask);

            mockMvc.perform(post("/api/adhoc-tasks/{id}/open", draftTask.getAdhocTaskId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value(AdhocTaskStatus.OPEN.name()));
        }
    }

    @Nested
    @DisplayName("POST /api/adhoc-tasks/{id}/close")
    class CloseAdhocTaskTests {

        @Test
        @DisplayName("Should close open adhoc task")
        void shouldCloseOpenAdhocTask() throws Exception {
            // First open the task
            testAdhocTask.setStatus(AdhocTaskStatus.OPEN);
            adhocTaskRepository.save(testAdhocTask);

            mockMvc.perform(post("/api/adhoc-tasks/{id}/close", testAdhocTask.getAdhocTaskId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value(AdhocTaskStatus.CLOSED.name()));
        }
    }

    @Nested
    @DisplayName("POST /api/adhoc-tasks/{id}/archive")
    class ArchiveAdhocTaskTests {

        @Test
        @DisplayName("Should archive adhoc task")
        void shouldArchiveAdhocTask() throws Exception {
            mockMvc.perform(post("/api/adhoc-tasks/{id}/archive", testAdhocTask.getAdhocTaskId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value(AdhocTaskStatus.ARCHIVED.name()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/adhoc-tasks/{id}")
    class DeleteAdhocTaskTests {

        @Test
        @DisplayName("Should delete draft adhoc task")
        void shouldDeleteDraftAdhocTask() throws Exception {
            // Create a new draft task to delete
            AdhocTask taskToDelete = new AdhocTask();
            taskToDelete.setTaskTitle("Task to Delete");
            taskToDelete.setTaskDesc("Task to delete description");
            taskToDelete.setCycle(testCycle);
            taskToDelete.setCreatorOrg(testOrg);
            taskToDelete.setScopeType(AdhocScopeType.ALL_ORGS);
            taskToDelete.setStatus(AdhocTaskStatus.DRAFT);
            taskToDelete.setDueAt(LocalDate.now().plusMonths(1));
            taskToDelete.setIncludeInAlert(false);
            taskToDelete = adhocTaskRepository.save(taskToDelete);

            mockMvc.perform(delete("/api/adhoc-tasks/{id}", taskToDelete.getAdhocTaskId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/adhoc-tasks/{id}/targets")
    class GetTargetOrganizationsTests {

        @Test
        @DisplayName("Should return target organizations for adhoc task")
        void shouldReturnTargetOrganizations() throws Exception {
            mockMvc.perform(get("/api/adhoc-tasks/{id}/targets", testAdhocTask.getAdhocTaskId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/adhoc-tasks/{id}/indicators")
    class GetMappedIndicatorsTests {

        @Test
        @DisplayName("Should return mapped indicators for adhoc task")
        void shouldReturnMappedIndicators() throws Exception {
            mockMvc.perform(get("/api/adhoc-tasks/{id}/indicators", testAdhocTask.getAdhocTaskId())
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
