package com.sism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.LoginRequest;
import com.sism.entity.SysUser;
import com.sism.repository.UserRepository;
import com.sism.vo.LoginResponse;
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
 * Integration tests for AuthController
 * Tests authentication endpoints: login, logout, and current user
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SysUser testUser;
    private final String testPassword = "testPassword123";

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            SysUser user = new SysUser();
            user.setUsername("testuser");
            user.setPasswordHash(passwordEncoder.encode(testPassword));
            user.setRealName("Test User");
            user.setIsActive(true);
            user.setOrg(userRepository.findAll().stream()
                    .filter(u -> u.getOrg() != null)
                    .map(SysUser::getOrg)
                    .findFirst()
                    .orElseThrow());
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
        @DisplayName("Should return 200 for logout with valid token")
        void shouldReturn200ForLogoutWithValidToken() throws Exception {
            // First login to get token
            String token = loginAndGetToken();

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + token))
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
        @DisplayName("Should return current user for valid token")
        void shouldReturnCurrentUserForValidToken() throws Exception {
            String token = loginAndGetToken();

            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + token))
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

    private String loginAndGetToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(testUser.getUsername());
        request.setPassword(testPassword);

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
