package com.sism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.config.TestSecurityConfig;
import com.sism.dto.LoginRequest;
import com.sism.entity.SysUser;
import com.sism.entity.SysOrg;
import com.sism.repository.SysUserRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.util.TestDataFactory;
import com.sism.enums.OrgType;
import com.sism.vo.LoginResponse;
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
 * Integration tests for AuthController
 * Tests authentication endpoints: login, logout, and current user
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class AuthControllerIntegrationTest {

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

    private SysUser testUser;
    private final String testPassword = "testPassword123";

    @BeforeEach
    void setUp() {
        // Create test org first
        SysOrg testOrg = TestDataFactory.createTestOrg(orgRepository, "测试组织", OrgType.FUNCTIONAL_DEPT);
        
        // Create test user with encoded password
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            SysUser user = new SysUser();
            user.setUsername("testuser");
            user.setPasswordHash(passwordEncoder.encode(testPassword));
            user.setRealName("Test User");
            user.setIsActive(true);
            user.setOrg(testOrg);
            return userRepository.save(user);
        });
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return 200 and token for valid credentials")
        void shouldReturnTokenForValidCredentials() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername(testUser.getUsername());
            request.setPassword(testPassword);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.user.username").value(testUser.getUsername()));
        }

        @Test
        @DisplayName("Should return 401 for invalid username")
        void shouldReturn401ForInvalidUsername() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("anypassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 for invalid password")
        void shouldReturn401ForInvalidPassword() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername(testUser.getUsername());
            request.setPassword("wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutEndpointTests {

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should return 200 for logout with valid token")
        void shouldReturn200ForLogoutWithValidToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }

        @Test
        @DisplayName("Should return 200 for logout without token")
        void shouldReturn200ForLogoutWithoutToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUserEndpointTests {

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should return current user for valid token")
        void shouldReturnCurrentUserForValidToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.username").value(testUser.getUsername()));
        }

        @Test
        @DisplayName("Should return 500 for missing token")
        void shouldReturn500ForMissingToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should return 401 for invalid token")
        void shouldReturn401ForInvalidToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
