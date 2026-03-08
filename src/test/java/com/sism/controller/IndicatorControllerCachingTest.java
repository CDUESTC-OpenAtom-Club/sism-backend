package com.sism.controller;

import com.sism.config.TestSecurityConfig;
import com.sism.dto.LoginRequest;
import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.entity.SysUser;
import com.sism.entity.StrategicTask;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.repository.*;
import com.sism.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for IndicatorController HTTP caching functionality
 * Tests Last-Modified header implementation and 304 Not Modified responses
 * 
 * Task 3.8: Fix caching implementation in IndicatorController
 * Requirements: 2.6, 2.7
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("IndicatorController HTTP Caching Tests")
class IndicatorControllerCachingTest {

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
        // Create test data
        testOrg = TestDataFactory.createTestOrg(orgRepository, "测试组织", OrgType.FUNCTIONAL_DEPT);
        testTask = TestDataFactory.createTestTask(taskRepository, cycleRepository, orgRepository);
        testIndicator = TestDataFactory.createTestIndicator(indicatorRepository, taskRepository, 
                                                            cycleRepository, orgRepository);
        
        // Create test user
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
        
        System.out.println("✓ Last-Modified header: " + lastModified);
    }

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
        
        System.out.println("✓ First request Last-Modified: " + lastModified);
        
        // Second request with If-Modified-Since header
        MvcResult secondResult = mockMvc.perform(get("/indicators")
                        .header("Authorization", "Bearer " + authToken)
                        .header("If-Modified-Since", lastModified))
                .andExpect(status().isNotModified())
                .andExpect(header().exists("Last-Modified"))
                .andReturn();
        
        // Verify no body is returned for 304
        String responseBody = secondResult.getResponse().getContentAsString();
        assertThat(responseBody).isEmpty();
        
        System.out.println("✓ Second request returned 304 Not Modified");
    }

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
        
        System.out.println("✓ Request with old If-Modified-Since returned 200 OK with data");
    }

    @Test
    @DisplayName("Should return Cache-Control header with max-age and must-revalidate")
    void shouldReturnCacheControlHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/indicators")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("Cache-Control", containsString("max-age")))
                .andExpect(header().string("Cache-Control", containsString("must-revalidate")))
                .andReturn();
        
        String cacheControl = result.getResponse().getHeader("Cache-Control");
        System.out.println("✓ Cache-Control header: " + cacheControl);
    }

    @Test
    @DisplayName("Should handle missing If-Modified-Since header gracefully")
    void shouldHandleMissingIfModifiedSinceHeader() throws Exception {
        // Request without If-Modified-Since should return 200 OK
        mockMvc.perform(get("/indicators")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("Last-Modified"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
        
        System.out.println("✓ Request without If-Modified-Since returned 200 OK");
    }

    @Test
    @DisplayName("Should handle invalid If-Modified-Since header gracefully")
    void shouldHandleInvalidIfModifiedSinceHeader() throws Exception {
        // Request with invalid If-Modified-Since should return 200 OK
        mockMvc.perform(get("/indicators")
                        .header("Authorization", "Bearer " + authToken)
                        .header("If-Modified-Since", "invalid-date-format"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Last-Modified"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
        
        System.out.println("✓ Request with invalid If-Modified-Since returned 200 OK");
    }

    @Test
    @DisplayName("Should update Last-Modified when indicator is modified")
    void shouldUpdateLastModifiedWhenIndicatorModified() throws Exception {
        // First request to get initial Last-Modified
        MvcResult firstResult = mockMvc.perform(get("/indicators")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("Last-Modified"))
                .andReturn();
        
        String firstLastModified = firstResult.getResponse().getHeader("Last-Modified");
        System.out.println("✓ Initial Last-Modified: " + firstLastModified);
        
        // Wait a moment to ensure timestamp difference
        Thread.sleep(1000);
        
        // Modify an indicator
        testIndicator.setUpdatedAt(LocalDateTime.now());
        indicatorRepository.save(testIndicator);
        
        // Second request should return 200 OK with new Last-Modified
        MvcResult secondResult = mockMvc.perform(get("/indicators")
                        .header("Authorization", "Bearer " + authToken)
                        .header("If-Modified-Since", firstLastModified))
                .andExpect(status().isOk())
                .andExpect(header().exists("Last-Modified"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        
        String secondLastModified = secondResult.getResponse().getHeader("Last-Modified");
        System.out.println("✓ Updated Last-Modified: " + secondLastModified);
        
        // Verify Last-Modified has changed
        assertThat(secondLastModified).isNotEqualTo(firstLastModified);
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
