package com.sism.controller;

import com.sism.config.TestSecurityConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.LoginRequest;
import com.sism.entity.SysUser;
import com.sism.entity.SysOrg;
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
import org.springframework.security.test.context.support.WithMockUser;
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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class OrgControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private SysOrg testOrg;
    private SysUser testUser;

    @BeforeEach
    void setUp() throws Exception {
        // Create test data using TestDataFactory
        testOrg = TestDataFactory.createTestOrg(orgRepository, "测试组织", OrgType.FUNCTIONAL_DEPT);
        
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
    @DisplayName("GET /api/orgs")
    class GetAllOrgsTests {

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should return all organizations")
        void shouldReturnAllOrganizations() throws Exception {
            mockMvc.perform(get("/orgs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should return 403 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/orgs"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/orgs/hierarchy")
    class GetOrgHierarchyTests {

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should return organization hierarchy tree")
        void shouldReturnOrgHierarchy() throws Exception {
            mockMvc.perform(get("/orgs/hierarchy"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/orgs/{orgId}/hierarchy")
    class GetOrgSubtreeTests {

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should return organization subtree")
        void shouldReturnOrgSubtree() throws Exception {
            mockMvc.perform(get("/orgs/{orgId}/hierarchy", testOrg.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should return 404 for non-existent org")
        void shouldReturn404ForNonExistentOrg() throws Exception {
            mockMvc.perform(get("/orgs/{orgId}/hierarchy", 999999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/orgs/{orgId}/descendants")
    class GetDescendantOrgIdsTests {

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should return descendant organization IDs")
        void shouldReturnDescendantOrgIds() throws Exception {
            mockMvc.perform(get("/orgs/{orgId}/descendants", testOrg.getId()))
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
