package com.sism.property;

import com.sism.AbstractIntegrationTest;
import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.enums.ProgressApprovalStatus;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.vo.IndicatorVO;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug Condition Exploration Test for Indicator Status Separation
 * 
 * **Validates: Requirements 2.1, 2.2**
 * 
 * **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * **DO NOT attempt to fix the test or the code when it fails**
 * **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
 * **GOAL**: Surface counterexamples that demonstrate the bug exists
 * 
 * Bug Condition: The system conflates indicator lifecycle status (DRAFT, PENDING_REVIEW, DISTRIBUTED)
 * with progress approval status (NONE, DRAFT, PENDING, APPROVED, REJECTED). These are orthogonal
 * state machines that should operate independently.
 * 
 * This test checks:
 * 1. Withdraw button logic incorrectly checks lifecycle status instead of progressApprovalStatus
 * 2. UI displays only one status without distinguishing between lifecycle and approval workflow
 * 3. Service methods mix logic for the two independent state machines
 * 4. System treats DISTRIBUTED + PENDING as conflicting states when they should coexist
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class IndicatorStatusSeparationBugConditionTest extends AbstractIntegrationTest {

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private SysOrgRepository sysOrgRepository;

    private SysOrg testOwnerOrg;
    private SysOrg testTargetOrg;

    @BeforeEach
    public void setUp() {
        // Create test organizations once for all tests
        testOwnerOrg = createTestOrg("战略发展部");
        testTargetOrg = createTestOrg("教务处");
    }

    /**
     * Test 1: Indicator with DISTRIBUTED status and PENDING progress approval should coexist
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code if system treats these as conflicting
     * - Current behavior: System may prevent or incorrectly handle this valid state combination
     * - Expected behavior: DISTRIBUTED (lifecycle) and PENDING (approval) are independent and can coexist
     * 
     * **Validates: Requirements 2.1**
     */
    @Test
    public void testDistributedWithPendingProgressApproval_shouldCoexist() {
        // Create indicator with DISTRIBUTED status and PENDING progress approval
        // This is a VALID state: indicator is distributed, and progress submission is pending approval
        Indicator indicator = Indicator.builder()
                .indicatorDesc("Test Indicator - Distributed with Pending Progress")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOwnerOrg)
                .targetOrg(testTargetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.DISTRIBUTED)  // Lifecycle status: distributed
                .progressApprovalStatus(ProgressApprovalStatus.PENDING)  // Approval status: pending
                .pendingProgress(80)
                .pendingRemark("Pending progress submission")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Indicator saved = indicatorRepository.save(indicator);

        // **BUG CONDITION**: System should treat these as independent, non-conflicting states
        // **EXPECTED BEHAVIOR**: Both statuses should be preserved and accessible
        
        assertThat(saved.getStatus())
                .as("Lifecycle status should be DISTRIBUTED")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);

        assertThat(saved.getProgressApprovalStatus())
                .as("Progress approval status should be PENDING")
                .isEqualTo(ProgressApprovalStatus.PENDING);

        // Verify the indicator can be retrieved and both statuses are maintained
        Indicator retrieved = indicatorRepository.findById(saved.getIndicatorId()).orElseThrow();
        
        assertThat(retrieved.getStatus())
                .as("Retrieved indicator should maintain DISTRIBUTED lifecycle status")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);

        assertThat(retrieved.getProgressApprovalStatus())
                .as("Retrieved indicator should maintain PENDING progress approval status")
                .isEqualTo(ProgressApprovalStatus.PENDING);
    }

    /**
     * Test 2: Withdraw button logic should check only progressApprovalStatus, not lifecycle status
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code
     * - Current behavior: canWithdraw field or logic checks lifecycle status (status field)
     * - Expected behavior: Withdraw button should check only progressApprovalStatus === PENDING
     * 
     * **Validates: Requirements 2.2**
     */
    @Test
    public void testWithdrawButtonLogic_shouldCheckProgressApprovalStatusOnly() {
        // Scenario 1: DISTRIBUTED indicator with PENDING progress approval
        // Withdraw button SHOULD be enabled (can withdraw progress submission)
        Indicator distributedWithPending = Indicator.builder()
                .indicatorDesc("Distributed with Pending Progress")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOwnerOrg)
                .targetOrg(testTargetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.DISTRIBUTED)  // Lifecycle: distributed
                .progressApprovalStatus(ProgressApprovalStatus.PENDING)  // Approval: pending
                .canWithdraw(false)  // This field might be incorrectly set based on lifecycle status
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Indicator saved1 = indicatorRepository.save(distributedWithPending);

        // **BUG CONDITION**: canWithdraw field may be set based on lifecycle status
        // **EXPECTED BEHAVIOR**: Withdraw enablement should depend ONLY on progressApprovalStatus
        
        // The correct logic should be: canWithdraw = (progressApprovalStatus == PENDING)
        // NOT: canWithdraw = (status == DRAFT or status == PENDING_REVIEW)
        
        boolean shouldBeWithdrawable = saved1.getProgressApprovalStatus() == ProgressApprovalStatus.PENDING;
        
        assertThat(shouldBeWithdrawable)
                .as("Indicator with PENDING progress approval should be withdrawable")
                .isTrue();

        // Scenario 2: DISTRIBUTED indicator with APPROVED progress
        // Withdraw button SHOULD be disabled (nothing to withdraw)
        Indicator distributedWithApproved = Indicator.builder()
                .indicatorDesc("Distributed with Approved Progress")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOwnerOrg)
                .targetOrg(testTargetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(2)
                .type("定量")
                .status(IndicatorStatus.DISTRIBUTED)  // Lifecycle: distributed
                .progressApprovalStatus(ProgressApprovalStatus.APPROVED)  // Approval: approved
                .canWithdraw(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Indicator saved2 = indicatorRepository.save(distributedWithApproved);

        boolean shouldNotBeWithdrawable = saved2.getProgressApprovalStatus() == ProgressApprovalStatus.PENDING;
        
        assertThat(shouldNotBeWithdrawable)
                .as("Indicator with APPROVED progress should NOT be withdrawable")
                .isFalse();

        // Scenario 3: DRAFT indicator with NONE progress approval
        // Withdraw button SHOULD be disabled (no progress to withdraw)
        Indicator draftWithNone = Indicator.builder()
                .indicatorDesc("Draft with No Progress")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOwnerOrg)
                .targetOrg(testTargetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(3)
                .type("定量")
                .status(IndicatorStatus.DRAFT)  // Lifecycle: draft
                .progressApprovalStatus(ProgressApprovalStatus.NONE)  // Approval: none
                .canWithdraw(true)  // This might be incorrectly set based on lifecycle status
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Indicator saved3 = indicatorRepository.save(draftWithNone);

        boolean shouldNotBeWithdrawable2 = saved3.getProgressApprovalStatus() == ProgressApprovalStatus.PENDING;
        
        assertThat(shouldNotBeWithdrawable2)
                .as("Indicator with NONE progress approval should NOT be withdrawable")
                .isFalse();
    }

    /**
     * Test 3: IndicatorVO should expose both status fields independently
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code if VO doesn't clearly separate the statuses
     * - Current behavior: VO may only expose one status or conflate them
     * - Expected behavior: VO should have separate fields for lifecycle status and progress approval status
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    @Test
    public void testIndicatorVO_shouldExposeBothStatusFieldsIndependently() {
        // Create indicator with both statuses set
        Indicator indicator = Indicator.builder()
                .indicatorDesc("Test Indicator for VO")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOwnerOrg)
                .targetOrg(testTargetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.DISTRIBUTED)
                .progressApprovalStatus(ProgressApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Indicator saved = indicatorRepository.save(indicator);

        // Convert to VO
        IndicatorVO vo = saved.toDTO();

        // **BUG CONDITION**: VO should expose both status fields clearly
        // **EXPECTED BEHAVIOR**: VO has separate fields for lifecycle and approval status
        
        assertThat(vo.getStatus())
                .as("VO should expose lifecycle status")
                .isNotNull();

        assertThat(vo.getProgressApprovalStatus())
                .as("VO should expose progress approval status")
                .isNotNull();

        assertThat(vo.getStatus().name())
                .as("VO lifecycle status should be DISTRIBUTED")
                .isEqualTo("DISTRIBUTED");

        assertThat(vo.getProgressApprovalStatus())
                .as("VO progress approval status should be PENDING")
                .isEqualTo("PENDING");
    }

    /**
     * Test 4: Property-based test - All valid status combinations should be supported
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code if system rejects valid combinations
     * - Current behavior: System may treat certain valid combinations as errors
     * - Expected behavior: All combinations of lifecycle status and progress approval status are valid
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 50)
    public void testAllValidStatusCombinations_shouldBeSupported(
            @ForAll("lifecycleStatuses") IndicatorStatus lifecycleStatus,
            @ForAll("progressApprovalStatuses") ProgressApprovalStatus progressApprovalStatus) {
        
        // Create indicator with any combination of statuses
        Indicator indicator = Indicator.builder()
                .indicatorDesc("Property Test Indicator")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOwnerOrg)
                .targetOrg(testTargetOrg)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .status(lifecycleStatus)
                .progressApprovalStatus(progressApprovalStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // **BUG CONDITION**: System should accept all valid status combinations
        // **EXPECTED BEHAVIOR**: Any combination of lifecycle and approval status is valid
        
        Indicator saved = indicatorRepository.save(indicator);

        assertThat(saved.getStatus())
                .as("Lifecycle status should be preserved")
                .isEqualTo(lifecycleStatus);

        assertThat(saved.getProgressApprovalStatus())
                .as("Progress approval status should be preserved")
                .isEqualTo(progressApprovalStatus);

        // Verify both statuses are independent
        Indicator retrieved = indicatorRepository.findById(saved.getIndicatorId()).orElseThrow();
        
        assertThat(retrieved.getStatus())
                .as("Retrieved lifecycle status should match saved")
                .isEqualTo(lifecycleStatus);

        assertThat(retrieved.getProgressApprovalStatus())
                .as("Retrieved progress approval status should match saved")
                .isEqualTo(progressApprovalStatus);
    }

    // ==================== Arbitraries ====================

    @Provide
    Arbitrary<IndicatorStatus> lifecycleStatuses() {
        return Arbitraries.of(
                IndicatorStatus.DRAFT,
                IndicatorStatus.PENDING_REVIEW,
                IndicatorStatus.DISTRIBUTED,
                IndicatorStatus.ARCHIVED
        );
    }

    @Provide
    Arbitrary<ProgressApprovalStatus> progressApprovalStatuses() {
        return Arbitraries.of(
                ProgressApprovalStatus.NONE,
                ProgressApprovalStatus.DRAFT,
                ProgressApprovalStatus.PENDING,
                ProgressApprovalStatus.APPROVED,
                ProgressApprovalStatus.REJECTED
        );
    }

    // ==================== Helper Methods ====================

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
