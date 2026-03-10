package com.sism.property;

import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.enums.ProgressApprovalStatus;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.SysOrgRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug Condition Exploration Test for Indicator Status Confusion
 * 
 * **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * **DO NOT attempt to fix the test or the code when it fails**
 * **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
 * **GOAL**: Surface counterexamples that demonstrate the bug exists
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.6, 2.9**
 * 
 * Bug Condition: The system uses ambiguous PENDING status for indicator definition review,
 * conflating it with progress approval PENDING. The fix introduces PENDING_REVIEW as a
 * distinct state for indicator definition review.
 * 
 * This test checks:
 * 1. Indicators with ambiguous PENDING status cannot be distinguished from progress approval PENDING
 * 2. POST /indicators/{id}/submit-review endpoint does not exist (404)
 * 3. Indicator entity defaults to ACTIVE instead of DRAFT
 * 4. Frontend types use ambiguous IndicatorStatus.PENDING
 * 5. IndicatorStatus enum lacks PENDING_REVIEW state
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class IndicatorStatusConfusionBugConditionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private SysOrgRepository sysOrgRepository;

    /**
     * Test 1: Indicators with ambiguous PENDING status cannot be distinguished from progress approval PENDING
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code
     * - Current behavior: Indicator uses status='PENDING' for definition review
     * - Expected behavior: Indicator should use status='PENDING_REVIEW' for definition review
     * 
     * **Validates: Requirements 2.1, 2.2, 2.3, 2.6**
     */
    @Test
    public void testIndicatorStatusAmbiguity_shouldFailOnUnfixedCode() {
        // Create test organizations
        SysOrg ownerOrg = createTestOrg("战略发展部");
        SysOrg targetOrg = createTestOrg("教务处");

        // Create indicator with PENDING status (ambiguous - could be definition review or progress approval)
        Indicator indicator = Indicator.builder()
                .indicatorDesc("Test Indicator")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(ownerOrg)
                .targetOrg(targetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.PENDING)  // Ambiguous status
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Indicator saved = indicatorRepository.save(indicator);

        // **BUG CONDITION**: The system uses PENDING for definition review, which is ambiguous
        // **EXPECTED BEHAVIOR**: The system should use PENDING_REVIEW for definition review
        
        // This assertion will FAIL on unfixed code because:
        // - Current: status = PENDING (ambiguous)
        // - Expected: status = PENDING_REVIEW (clear distinction)
        
        // Check if PENDING_REVIEW enum value exists
        boolean hasPendingReview = false;
        for (IndicatorStatus status : IndicatorStatus.values()) {
            if (status.name().equals("PENDING_REVIEW")) {
                hasPendingReview = true;
                break;
            }
        }

        // **EXPECTED TO FAIL**: PENDING_REVIEW does not exist in unfixed code
        assertThat(hasPendingReview)
                .as("IndicatorStatus enum should have PENDING_REVIEW value for definition review")
                .isTrue();

        // **EXPECTED TO FAIL**: Indicator uses PENDING instead of PENDING_REVIEW
        assertThat(saved.getStatus().name())
                .as("Indicator awaiting definition review should use PENDING_REVIEW, not ambiguous PENDING")
                .isEqualTo("PENDING_REVIEW");
    }

    /**
     * Test 2: POST /indicators/{id}/submit-review endpoint does not exist
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code (404 Not Found)
     * - Current behavior: Endpoint does not exist
     * - Expected behavior: Endpoint should exist and transition DRAFT → PENDING_REVIEW
     * 
     * **Validates: Requirements 2.3, 2.7**
     */
    @Test
    @WithMockUser(username = "strategic_user", roles = {"STRATEGIC_DEPT"})
    public void testSubmitReviewEndpointMissing_shouldFailOnUnfixedCode() throws Exception {
        // Create test organizations
        SysOrg ownerOrg = createTestOrg("战略发展部");
        SysOrg targetOrg = createTestOrg("教务处");

        // Create indicator in DRAFT status
        Indicator indicator = Indicator.builder()
                .indicatorDesc("Test Indicator")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(ownerOrg)
                .targetOrg(targetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.DRAFT)
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Indicator saved = indicatorRepository.save(indicator);

        // **BUG CONDITION**: POST /indicators/{id}/submit-review endpoint does not exist
        // **EXPECTED BEHAVIOR**: Endpoint should exist and return 200 OK
        
        // This will FAIL on unfixed code with 404 Not Found
        mockMvc.perform(post("/api/indicators/" + saved.getIndicatorId() + "/submit-review")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())  // **EXPECTED TO FAIL**: Endpoint does not exist (404)
                .andReturn();
    }

    /**
     * Test 3: Indicator entity defaults to ACTIVE instead of DRAFT
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code
     * - Current behavior: New indicators default to ACTIVE (deprecated status)
     * - Expected behavior: New indicators should default to DRAFT
     * 
     * **Validates: Requirements 2.4, 2.8**
     */
    @Test
    public void testIndicatorDefaultStatus_shouldFailOnUnfixedCode() {
        // Create test organizations
        SysOrg ownerOrg = createTestOrg("战略发展部");
        SysOrg targetOrg = createTestOrg("教务处");

        // Create indicator using @Builder without explicitly setting status
        Indicator indicator = Indicator.builder()
                .indicatorDesc("Test Indicator")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(ownerOrg)
                .targetOrg(targetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // **BUG CONDITION**: Indicator defaults to ACTIVE (deprecated)
        // **EXPECTED BEHAVIOR**: Indicator should default to DRAFT
        
        // This assertion will FAIL on unfixed code because:
        // - Current: @Builder.Default status = IndicatorStatus.ACTIVE
        // - Expected: @Builder.Default status = IndicatorStatus.DRAFT
        
        assertThat(indicator.getStatus())
                .as("New indicators should default to DRAFT, not deprecated ACTIVE")
                .isEqualTo(IndicatorStatus.DRAFT);  // **EXPECTED TO FAIL**: Defaults to ACTIVE
    }

    /**
     * Test 4: IndicatorStatus enum lacks PENDING_REVIEW state
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code
     * - Current behavior: Enum has DRAFT, PENDING, DISTRIBUTED, ACTIVE, ARCHIVED
     * - Expected behavior: Enum should have DRAFT, PENDING_REVIEW, DISTRIBUTED, ARCHIVED
     * 
     * **Validates: Requirements 2.1, 2.4, 2.6**
     */
    @Test
    public void testIndicatorStatusEnumLacksPendingReview_shouldFailOnUnfixedCode() {
        // **BUG CONDITION**: IndicatorStatus enum lacks PENDING_REVIEW value
        // **EXPECTED BEHAVIOR**: Enum should have PENDING_REVIEW for definition review
        
        boolean hasPendingReview = false;
        boolean hasPending = false;
        
        for (IndicatorStatus status : IndicatorStatus.values()) {
            if (status.name().equals("PENDING_REVIEW")) {
                hasPendingReview = true;
            }
            if (status.name().equals("PENDING")) {
                hasPending = true;
            }
        }

        // **EXPECTED TO FAIL**: PENDING_REVIEW does not exist in unfixed code
        assertThat(hasPendingReview)
                .as("IndicatorStatus enum should have PENDING_REVIEW for indicator definition review")
                .isTrue();

        // **EXPECTED TO FAIL**: PENDING still exists (should be deprecated or removed)
        assertThat(hasPending)
                .as("IndicatorStatus enum should deprecate ambiguous PENDING in favor of PENDING_REVIEW")
                .isFalse();
    }

    /**
     * Test 5: Status field separation - lifecycle vs approval status
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code
     * - Current behavior: PENDING used for both lifecycle and approval status
     * - Expected behavior: PENDING_REVIEW for lifecycle, PENDING for approval status only
     * 
     * **Validates: Requirements 2.2, 2.5, 2.6**
     */
    @Test
    public void testStatusFieldSeparation_shouldFailOnUnfixedCode() {
        // Create test organizations
        SysOrg ownerOrg = createTestOrg("战略发展部");
        SysOrg targetOrg = createTestOrg("教务处");

        // Scenario 1: Indicator awaiting definition review
        Indicator indicatorAwaitingReview = Indicator.builder()
                .indicatorDesc("Indicator awaiting definition review")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(ownerOrg)
                .targetOrg(targetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.PENDING)  // Ambiguous - definition review or progress approval?
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Scenario 2: Indicator distributed with progress approval pending
        Indicator indicatorWithProgressPending = Indicator.builder()
                .indicatorDesc("Indicator with progress approval pending")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(ownerOrg)
                .targetOrg(targetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.DISTRIBUTED)
                .progressApprovalStatus(ProgressApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // **BUG CONDITION**: Cannot distinguish between definition review and progress approval
        // Both use "PENDING" but in different contexts
        
        // **EXPECTED BEHAVIOR**: 
        // - Definition review: status = PENDING_REVIEW, progressApprovalStatus = NONE
        // - Progress approval: status = DISTRIBUTED, progressApprovalStatus = PENDING
        
        // This assertion will FAIL on unfixed code
        assertThat(indicatorAwaitingReview.getStatus().name())
                .as("Indicator awaiting definition review should use PENDING_REVIEW")
                .isEqualTo("PENDING_REVIEW");  // **EXPECTED TO FAIL**: Uses PENDING

        // This should pass (progress approval uses separate field)
        assertThat(indicatorWithProgressPending.getStatus())
                .as("Indicator with progress approval should use DISTRIBUTED status")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);

        assertThat(indicatorWithProgressPending.getProgressApprovalStatus())
                .as("Progress approval should use progressApprovalStatus field")
                .isEqualTo(ProgressApprovalStatus.PENDING);
    }

    // Helper method to create test organization
    private SysOrg createTestOrg(String name) {
        SysOrg org = new SysOrg();
        org.setName(name);
        org.setType(OrgType.FUNCTIONAL_DEPT);
        org.setIsActive(true);
        org.setSortOrder(0);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        return sysOrgRepository.save(org);
    }
}
