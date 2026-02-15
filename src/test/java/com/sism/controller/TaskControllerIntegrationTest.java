package com.sism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.LoginRequest;
import com.sism.entity.SysUser;
import com.sism.entity.AssessmentCycle;
import com.sism.entity.SysOrg;
import com.sism.entity.StrategicTask;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TaskController
 * Tests strategic task CRUD endpoints
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssessmentCycleRepository cycleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private StrategicTask testTask;
    private SysOrg testOrg;
    private AssessmentCycle testCycle;
    private SysUser testUser;

    @BeforeEach
    void setUp() throws Exception {
        // Create test data using TestDataFactory
        testTask = TestDataFactory.createTestTask(taskRepository, cycleRepository, orgRepository);
        testOrg = testTask.getOrg();
        testCycle = cycleRepository.findById(testTask.getCycleId()).orElseThrow();
        
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
    @DisplayName("GET /api/tasks")
    class GetAllTasksTests {

        @Test
        @DisplayName("Should return all tasks")
        void shouldReturnAllTasks() throws Exception {
            mockMvc.perform(get("/api/tasks")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Should return task by ID")
        void shouldReturnTaskById() throws Exception {
            mockMvc.perform(get("/api/tasks/{id}", testTask.getTaskId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.taskId").value(testTask.getTaskId()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent task")
        void shouldReturn404ForNonExistentTask() throws Exception {
            mockMvc.perform(get("/api/tasks/{id}", 999999L)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/cycle/{cycleId}")
    class GetTasksByCycleIdTests {

        @Test
        @DisplayName("Should return tasks by cycle ID")
        void shouldReturnTasksByCycleId() throws Exception {
            mockMvc.perform(get("/api/tasks/cycle/{cycleId}", testCycle.getCycleId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/org/{orgId}")
    class GetTasksByOrgIdTests {

        @Test
        @DisplayName("Should return tasks by organization ID")
        void shouldReturnTasksByOrgId() throws Exception {
            mockMvc.perform(get("/api/tasks/org/{orgId}", testOrg.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/search")
    class SearchTasksTests {

        @Test
        @DisplayName("Should search tasks by keyword")
        void shouldSearchTasksByKeyword() throws Exception {
            mockMvc.perform(get("/api/tasks/search")
                            .param("keyword", "战略")
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
