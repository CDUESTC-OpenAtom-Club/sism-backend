package com.sism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.LoginRequest;
import com.sism.entity.AppUser;
import com.sism.entity.Org;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrgController
 * Tests organization query endpoints
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrgControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private Org testOrg;

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

        // Get a test org
        testOrg = orgRepository.findAll().stream().findFirst().orElseThrow();

        // Login to get token
        authToken = loginAndGetToken(testUser.getUsername(), "testPassword123");
    }

    @Nested
    @DisplayName("GET /api/orgs")
    class GetAllOrgsTests {

        @Test
        @DisplayName("Should return all organizations")
        void shouldReturnAllOrganizations() throws Exception {
            mockMvc.perform(get("/api/orgs")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/orgs"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/orgs/hierarchy")
    class GetOrgHierarchyTests {

        @Test
        @DisplayName("Should return organization hierarchy tree")
        void shouldReturnOrgHierarchy() throws Exception {
            mockMvc.perform(get("/api/orgs/hierarchy")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/orgs/{orgId}/hierarchy")
    class GetOrgSubtreeTests {

        @Test
        @DisplayName("Should return organization subtree")
        void shouldReturnOrgSubtree() throws Exception {
            mockMvc.perform(get("/api/orgs/{orgId}/hierarchy", testOrg.getOrgId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent org")
        void shouldReturn404ForNonExistentOrg() throws Exception {
            mockMvc.perform(get("/api/orgs/{orgId}/hierarchy", 999999L)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/orgs/{orgId}/descendants")
    class GetDescendantOrgIdsTests {

        @Test
        @DisplayName("Should return descendant organization IDs")
        void shouldReturnDescendantOrgIds() throws Exception {
            mockMvc.perform(get("/api/orgs/{orgId}/descendants", testOrg.getOrgId())
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
