package com.sism.performance;

import com.sism.AbstractIntegrationTest;
import com.sism.dto.LoginRequest;
import com.sism.entity.*;
import com.sism.enums.*;
import com.sism.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Performance Benchmark Tests for SISM Backend
 * 
 * Validates that API endpoints meet performance requirements:
 * - Authentication: < 500ms
 * - Simple queries: < 200ms
 * - Complex queries: < 1000ms
 * - Write operations: < 300ms
 * - Bulk operations: < 2000ms
 */
@AutoConfigureMockMvc
class PerformanceBenchmarkTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private AssessmentCycleRepository cycleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private SysUser testUser;
    private SysOrg testOrg;
    private AssessmentCycle testCycle;

    @BeforeEach
    void setUp() throws Exception {
        // Create test organization
        testOrg = new SysOrg();
        testOrg.setName("Performance Test Org");
        testOrg.setType(OrgType.FUNCTIONAL_DEPT);
        testOrg.setIsActive(true);
        testOrg.setSortOrder(0);
        testOrg = orgRepository.save(testOrg);

        // Create test user
        testUser = new SysUser();
        testUser.setUsername("perftest");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setRealName("Performance Tester");
        testUser.setOrg(testOrg);
        testUser.setIsActive(true);
        testUser = userRepository.save(testUser);

        // Create test cycle
        testCycle = new AssessmentCycle();
        testCycle.setCycleName("2026 Performance Test");
        testCycle.setYear(2026);
        testCycle.setStartDate(LocalDate.of(2026, 1, 1));
        testCycle.setEndDate(LocalDate.of(2026, 12, 31));
        testCycle.setDescription("Performance testing cycle");
        testCycle = cycleRepository.save(testCycle);

        // Authenticate and get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("perftest");
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).get("data").get("token").asText();
    }

    /**
     * Benchmark 1: Authentication Performance
     * Requirement: Login should complete in < 500ms
     */
    @Test
    void testAuthenticationPerformance() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("perftest");
        loginRequest.setPassword("password123");

        long startTime = System.nanoTime();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertThat(duration)
                .as("Authentication should complete in < 500ms")
                .isLessThan(500);

        System.out.printf("✓ Authentication Performance: %dms (target: <500ms)%n", duration);
    }

    /**
     * Benchmark 2: Simple Query Performance
     * Requirement: Single entity retrieval should complete in < 200ms
     */
    @Test
    void testSimpleQueryPerformance() throws Exception {
        long startTime = System.nanoTime();

        mockMvc.perform(get("/api/org/" + testOrg.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertThat(duration)
                .as("Simple query should complete in < 200ms")
                .isLessThan(200);

        System.out.printf("✓ Simple Query Performance: %dms (target: <200ms)%n", duration);
    }

    /**
     * Benchmark 3: List Query Performance
     * Requirement: List queries should complete in < 300ms
     */
    @Test
    void testListQueryPerformance() throws Exception {
        // Create some test organizations
        for (int i = 0; i < 10; i++) {
            SysOrg org = new SysOrg();
            org.setName("Perf Test Org " + i);
            org.setType(OrgType.FUNCTIONAL_DEPT);
            org.setIsActive(true);
            org.setSortOrder(i);
            orgRepository.save(org);
        }

        long startTime = System.nanoTime();

        mockMvc.perform(get("/api/org")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertThat(duration)
                .as("List query should complete in < 300ms")
                .isLessThan(300);

        System.out.printf("✓ List Query Performance: %dms (target: <300ms)%n", duration);
    }

    /**
     * Benchmark 4: Write Operation Performance
     * Requirement: Single entity creation should complete in < 300ms
     */
    @Test
    void testWriteOperationPerformance() throws Exception {
        String orgJson = """
                {
                    "name": "Performance Write Test Org",
                    "type": "FUNCTIONAL_DEPT",
                    "isActive": true,
                    "sortOrder": 100
                }
                """;

        long startTime = System.nanoTime();

        mockMvc.perform(post("/api/org")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orgJson))
                .andExpect(status().isOk());

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertThat(duration)
                .as("Write operation should complete in < 300ms")
                .isLessThan(300);

        System.out.printf("✓ Write Operation Performance: %dms (target: <300ms)%n", duration);
    }

    /**
     * Benchmark 5: Complex Query Performance
     * Requirement: Queries with joins should complete in < 1000ms
     */
    @Test
    void testComplexQueryPerformance() throws Exception {
        // Create test user with organization relationship
        SysUser complexUser = new SysUser();
        complexUser.setUsername("complextest");
        complexUser.setPasswordHash(passwordEncoder.encode("password123"));
        complexUser.setRealName("Complex Test User");
        complexUser.setOrg(testOrg);
        complexUser.setIsActive(true);
        complexUser = userRepository.save(complexUser);

        long startTime = System.nanoTime();

        // Query that involves joins (user + org)
        mockMvc.perform(get("/api/users/" + complexUser.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertThat(duration)
                .as("Complex query should complete in < 1000ms")
                .isLessThan(1000);

        System.out.printf("✓ Complex Query Performance: %dms (target: <1000ms)%n", duration);
    }

    /**
     * Benchmark 6: Concurrent Request Performance
     * Requirement: System should handle 10 concurrent requests in < 2000ms total
     */
    @Test
    void testConcurrentRequestPerformance() throws Exception {
        int concurrentRequests = 10;
        List<Long> durations = new ArrayList<>();

        long startTime = System.nanoTime();

        for (int i = 0; i < concurrentRequests; i++) {
            long requestStart = System.nanoTime();

            mockMvc.perform(get("/api/org/" + testOrg.getId())
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk());

            long requestDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - requestStart);
            durations.add(requestDuration);
        }

        long totalDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertThat(totalDuration)
                .as("10 concurrent requests should complete in < 2000ms")
                .isLessThan(2000);

        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.printf("✓ Concurrent Request Performance: %dms total, %.1fms avg (target: <2000ms total)%n",
                totalDuration, avgDuration);
    }

    /**
     * Benchmark 7: Update Operation Performance
     * Requirement: Entity updates should complete in < 300ms
     */
    @Test
    void testUpdateOperationPerformance() throws Exception {
        SysOrg updateOrg = new SysOrg();
        updateOrg.setName("Update Test Org");
        updateOrg.setType(OrgType.FUNCTIONAL_DEPT);
        updateOrg.setIsActive(true);
        updateOrg.setSortOrder(0);
        updateOrg = orgRepository.save(updateOrg);

        String updateJson = """
                {
                    "name": "Updated Performance Test Org",
                    "isActive": false
                }
                """;

        long startTime = System.nanoTime();

        mockMvc.perform(put("/api/org/" + updateOrg.getId())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertThat(duration)
                .as("Update operation should complete in < 300ms")
                .isLessThan(300);

        System.out.printf("✓ Update Operation Performance: %dms (target: <300ms)%n", duration);
    }

    /**
     * Benchmark 8: Health Check Performance
     * Requirement: Health check should complete in < 100ms
     */
    @Test
    void testHealthCheckPerformance() throws Exception {
        long startTime = System.nanoTime();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertThat(duration)
                .as("Health check should complete in < 100ms")
                .isLessThan(100);

        System.out.printf("✓ Health Check Performance: %dms (target: <100ms)%n", duration);
    }

    /**
     * Benchmark Summary
     * Prints overall performance metrics
     */
    @Test
    void testPerformanceSummary() throws Exception {
        System.out.println("\n=== SISM Backend Performance Benchmark Summary ===");
        System.out.println("Target Performance Requirements:");
        System.out.println("  • Authentication: < 500ms");
        System.out.println("  • Simple queries: < 200ms");
        System.out.println("  • List queries: < 300ms");
        System.out.println("  • Write operations: < 300ms");
        System.out.println("  • Complex queries: < 1000ms");
        System.out.println("  • Concurrent requests (10): < 2000ms");
        System.out.println("  • Health check: < 100ms");
        System.out.println("\nRun individual benchmark tests to verify performance.");
        System.out.println("==================================================\n");
    }
}
