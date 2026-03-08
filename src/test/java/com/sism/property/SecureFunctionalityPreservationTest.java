package com.sism.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.AbstractIntegrationTest;
import com.sism.dto.LoginRequest;
import com.sism.dto.UserCreateRequest;
import com.sism.entity.AssessmentCycle;
import com.sism.entity.SysOrg;
import com.sism.entity.SysUser;
import com.sism.enums.OrgType;
import com.sism.repository.AssessmentCycleRepository;
import com.sism.repository.OrgRepository;
import com.sism.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Preservation Property Tests for Backend Security Refactor
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 * 
 * **CRITICAL**: These tests MUST PASS on unfixed code - they establish the baseline
 * behavior that must be preserved after the security fix.
 * 
 * **Property 2: Preservation** - Secure Functionality Continuity
 * 
 * This test verifies that secure, well-designed endpoints continue to work exactly
 * as before. These tests observe the current behavior on UNFIXED code and encode
 * it as properties that must be preserved.
 * 
 * **GOAL**: Establish baseline behavior for secure endpoints that should NOT change:
 * - UserManagementController operations continue working identically
 * - AuthController authentication flows remain unchanged
 * - HealthController system status reporting continues functioning
 * - AssessmentCycleController workflow management remains functionally identical
 * - All existing secure business operations process without functional changes
 * 
 * **Expected Outcome**: All tests PASS on unfixed code (baseline established)
 */
@JqwikSpringSupport
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecureFunctionalityPreservationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private AssessmentCycleRepository assessmentCycleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SysUser testUser;
    private SysOrg testOrg;
    private AssessmentCycle testCycle;

    @BeforeEach
    void setUp() {
        // Clean up test data
        assessmentCycleRepository.deleteAll();
        userRepository.deleteAll();
        orgRepository.deleteAll();

        // Create test organization
        testOrg = new SysOrg();
        testOrg.setName("测试部门");
        testOrg.setType(OrgType.FUNCTIONAL_DEPT);
        testOrg.setIsActive(true);
        testOrg.setSortOrder(1);
        testOrg.setCreatedAt(LocalDateTime.now());
        testOrg.setUpdatedAt(LocalDateTime.now());
        testOrg = orgRepository.save(testOrg);

        // Create test user
        testUser = new SysUser();
        testUser.setUsername("testuser");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setRealName("Test User");
        testUser.setOrg(testOrg);
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Create test assessment cycle
        testCycle = new AssessmentCycle();
        testCycle.setCycleName("2024年度考核周期");
        testCycle.setYear(2024);
        testCycle.setStartDate(LocalDate.of(2024, 1, 1));
        testCycle.setEndDate(LocalDate.of(2024, 12, 31));
        testCycle.setDescription("Test assessment cycle");
        testCycle.setCreatedAt(LocalDateTime.now());
        testCycle.setUpdatedAt(LocalDateTime.now());
        testCycle = assessmentCycleRepository.save(testCycle);
    }

    /**
     * Property 2.1: UserManagementController - User Search Functionality Preserved
     * 
     * EXPECTED BEHAVIOR: User search with pagination and filters continues to work
     * exactly as before, returning properly formatted results.
     * 
     * This test observes the current behavior and ensures it remains unchanged.
     */
    @Property(tries = 10)
    @Label("Bugfix: backend-security-refactor, Property 2.1: UserManagement Search Preserved")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void userManagementController_searchUsers_shouldContinueWorking(
            @ForAll("pageNumbers") int page,
            @ForAll("pageSizes") int size) throws Exception {

        // EXPECTED BEHAVIOR: Search endpoint returns 200 OK with paginated results
        // This behavior must be preserved after the security fix
        mockMvc.perform(get("/api/admin/users")
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("sortBy", "id")
                .param("sortOrder", "asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageable").exists());
    }

    /**
     * Property 2.2: UserManagementController - Get User By ID Preserved
     * 
     * EXPECTED BEHAVIOR: Getting user by ID continues to work correctly,
     * returning user details or 404 for non-existent users.
     */
    @Property(tries = 5)
    @Label("Bugfix: backend-security-refactor, Property 2.2: UserManagement GetById Preserved")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void userManagementController_getUserById_shouldContinueWorking() throws Exception {

        // EXPECTED BEHAVIOR: Valid user ID returns 200 OK with user details
        mockMvc.perform(get("/api/admin/users/" + testUser.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testUser.getId()))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.realName").value("Test User"));
    }

    /**
     * Property 2.3: UserManagementController - Create User Preserved
     * 
     * EXPECTED BEHAVIOR: User creation with valid DTOs continues to work,
     * properly validating input and creating users.
     */
    @Property(tries = 5)
    @Label("Bugfix: backend-security-refactor, Property 2.3: UserManagement Create Preserved")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void userManagementController_createUser_shouldContinueWorking(
            @ForAll("usernames") String username) throws Exception {

        // Create valid user request
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(username);
        request.setPassword("Password123!");
        request.setRealName("New User");
        request.setOrgId(testOrg.getId());

        // EXPECTED BEHAVIOR: Valid request returns 200 OK with created user
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(username));
    }

    /**
     * Property 2.4: AuthController - Login Flow Preserved
     * 
     * EXPECTED BEHAVIOR: Authentication with valid credentials continues to work,
     * returning JWT tokens and user information.
     */
    @Property(tries = 5)
    @Label("Bugfix: backend-security-refactor, Property 2.4: Auth Login Preserved")
    void authController_login_shouldContinueWorking() throws Exception {

        // Create login request
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // EXPECTED BEHAVIOR: Valid credentials return 200 OK with token
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user").exists())
                .andExpect(jsonPath("$.data.user.username").value("testuser"));
    }

    /**
     * Property 2.5: AuthController - Invalid Login Rejected
     * 
     * EXPECTED BEHAVIOR: Authentication with invalid credentials continues to
     * be properly rejected with appropriate error responses.
     */
    @Property(tries = 5)
    @Label("Bugfix: backend-security-refactor, Property 2.5: Auth Invalid Login Preserved")
    void authController_invalidLogin_shouldContinueBeingRejected(
            @ForAll("invalidPasswords") String invalidPassword) throws Exception {

        // Create login request with invalid password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword(invalidPassword);

        // EXPECTED BEHAVIOR: Invalid credentials return 401 Unauthorized
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Property 2.6: HealthController - Health Check Preserved
     * 
     * EXPECTED BEHAVIOR: Health check endpoint continues to return system status
     * without requiring authentication.
     */
    @Property(tries = 5)
    @Label("Bugfix: backend-security-refactor, Property 2.6: Health Check Preserved")
    void healthController_healthCheck_shouldContinueWorking() throws Exception {

        // EXPECTED BEHAVIOR: Health check returns 200 OK with status information
        mockMvc.perform(get("/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("sism-backend"))
                .andExpect(jsonPath("$.data.timestamp").exists());
    }

    /**
     * Property 2.7: AssessmentCycleController - Get All Cycles Preserved
     * 
     * EXPECTED BEHAVIOR: Retrieving all assessment cycles continues to work,
     * returning properly formatted cycle data.
     */
    @Property(tries = 5)
    @Label("Bugfix: backend-security-refactor, Property 2.7: AssessmentCycle GetAll Preserved")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void assessmentCycleController_getAllCycles_shouldContinueWorking() throws Exception {

        // EXPECTED BEHAVIOR: Get all cycles returns 200 OK with cycle list
        mockMvc.perform(get("/cycles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].year").exists());
    }

    /**
     * Property 2.8: AssessmentCycleController - Get Cycle By ID Preserved
     * 
     * EXPECTED BEHAVIOR: Retrieving assessment cycle by ID continues to work,
     * returning cycle details or 404 for non-existent cycles.
     */
    @Property(tries = 5)
    @Label("Bugfix: backend-security-refactor, Property 2.8: AssessmentCycle GetById Preserved")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void assessmentCycleController_getCycleById_shouldContinueWorking() throws Exception {

        // EXPECTED BEHAVIOR: Valid cycle ID returns 200 OK with cycle details
        mockMvc.perform(get("/cycles/" + testCycle.getCycleId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cycleId").value(testCycle.getCycleId()))
                .andExpect(jsonPath("$.data.year").value(2024));
    }

    /**
     * Property 2.9: AssessmentCycleController - Get Cycle By Year Preserved
     * 
     * EXPECTED BEHAVIOR: Retrieving assessment cycle by year continues to work,
     * returning the correct cycle for the specified year.
     */
    @Property(tries = 5)
    @Label("Bugfix: backend-security-refactor, Property 2.9: AssessmentCycle GetByYear Preserved")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void assessmentCycleController_getCycleByYear_shouldContinueWorking() throws Exception {

        // EXPECTED BEHAVIOR: Valid year returns 200 OK with cycle for that year
        mockMvc.perform(get("/cycles/year/2024")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.year").value(2024))
                .andExpect(jsonPath("$.data.startDate").exists())
                .andExpect(jsonPath("$.data.endDate").exists());
    }

    /**
     * Property 2.10: AssessmentCycleController - Get Active Cycles Preserved
     * 
     * EXPECTED BEHAVIOR: Retrieving active or future cycles continues to work,
     * returning only cycles that are currently active or will be active.
     */
    @Property(tries = 5)
    @Label("Bugfix: backend-security-refactor, Property 2.10: AssessmentCycle GetActive Preserved")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void assessmentCycleController_getActiveCycles_shouldContinueWorking() throws Exception {

        // EXPECTED BEHAVIOR: Get active cycles returns 200 OK with filtered list
        mockMvc.perform(get("/cycles/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== Arbitraries (Test Data Generators) ====================

    /**
     * Generate valid page numbers for pagination testing
     */
    @Provide
    Arbitrary<Integer> pageNumbers() {
        return Arbitraries.integers().between(0, 5);
    }

    /**
     * Generate valid page sizes for pagination testing
     */
    @Provide
    Arbitrary<Integer> pageSizes() {
        return Arbitraries.of(10, 20, 50);
    }

    /**
     * Generate valid usernames for user creation testing
     */
    @Provide
    Arbitrary<String> usernames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> "user_" + s);
    }

    /**
     * Generate invalid passwords for authentication testing
     */
    @Provide
    Arbitrary<String> invalidPasswords() {
        return Arbitraries.of(
                "wrongpassword",
                "incorrect123",
                "badpass",
                "notthepassword",
                "invalid"
        );
    }
}
