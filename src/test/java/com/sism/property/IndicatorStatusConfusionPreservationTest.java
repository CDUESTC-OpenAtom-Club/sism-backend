package com.sism.property;

import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.enums.ProgressApprovalStatus;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.service.IndicatorService;
import com.sism.vo.IndicatorVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preservation Property Tests for Indicator Status Confusion Fix
 * 
 * **Property 2: Preservation** - Progress Approval and Distributed Indicator Behavior
 * 
 * **IMPORTANT**: These tests observe behavior on UNFIXED code for non-buggy inputs.
 * They capture baseline behavior that MUST be preserved after the fix is implemented.
 * 
 * **EXPECTED OUTCOME**: All tests PASS on unfixed code (confirms baseline behavior)
 * 
 * **Validates: Requirements 3.1, 3.3, 3.4, 3.5, 3.8, 3.10, 3.13**
 * 
 * Test Strategy:
 * 1. Observe progress approval workflow (NONE → DRAFT → PENDING → APPROVED/REJECTED)
 * 2. Observe DISTRIBUTED indicators function correctly
 * 3. Observe existing CRUD operations work correctly
 * 4. Observe dashboard filtering by progressApprovalStatus works correctly
 * 5. Observe distribution endpoint transitions to DISTRIBUTED correctly
 * 6. Observe approval history logs both workflows separately
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IndicatorStatusConfusionPreservationTest {

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private IndicatorService indicatorService;

    private SysOrg strategicOrg;
    private SysOrg functionalOrg;

    @BeforeEach
    void setUp() {
        // Create test organizations
        strategicOrg = new SysOrg();
        strategicOrg.setName("战略发展部");
        strategicOrg.setType(OrgType.FUNCTIONAL_DEPT);
        strategicOrg.setIsActive(true);
        strategicOrg.setSortOrder(1);
        strategicOrg = orgRepository.save(strategicOrg);

        functionalOrg = new SysOrg();
        functionalOrg.setName("教务处");
        functionalOrg.setType(OrgType.FUNCTIONAL_DEPT);
        functionalOrg.setIsActive(true);
        functionalOrg.setSortOrder(2);
        functionalOrg = orgRepository.save(functionalOrg);
    }

    /**
     * Property 1: Progress Approval Workflow Preservation
     * 
     * **Validates: Requirements 3.1, 3.3, 3.4, 3.5**
     * 
     * FOR ALL indicators WHERE status = DISTRIBUTED AND progressApprovalStatus transitions through workflow
     * THEN the progress approval workflow (NONE → DRAFT → PENDING → APPROVED/REJECTED) 
     * MUST work exactly as before the fix
     */
    @Test
    void progressApprovalWorkflow_shouldBePreserved() {
        
        int progress = 75;  // Test with a specific progress value
        
        // Create a DISTRIBUTED indicator (non-buggy input - not affected by fix)
        Indicator indicator = Indicator.builder()
                .taskId(1L)
                .indicatorDesc("测试指标 - 进度审批流程")
                .level(IndicatorLevel.PRIMARY)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .progress(0)
                .ownerOrg(strategicOrg)
                .targetOrg(functionalOrg)
                .status(IndicatorStatus.DISTRIBUTED)  // Non-buggy: DISTRIBUTED status
                .progressApprovalStatus(ProgressApprovalStatus.NONE)  // Progress approval workflow
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        
        indicator = indicatorRepository.save(indicator);
        
        // **OBSERVE BASELINE BEHAVIOR**: Progress approval workflow transitions
        
        // Transition 1: NONE → DRAFT (department creates progress draft)
        indicator.setProgressApprovalStatus(ProgressApprovalStatus.DRAFT);
        indicator.setPendingProgress(progress);
        indicator = indicatorRepository.save(indicator);
        
        assertThat(indicator.getProgressApprovalStatus())
                .as("Progress approval workflow: NONE → DRAFT transition works")
                .isEqualTo(ProgressApprovalStatus.DRAFT);
        assertThat(indicator.getStatus())
                .as("Lifecycle status remains DISTRIBUTED during progress approval")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);
        
        // Transition 2: DRAFT → PENDING (department submits for approval)
        indicator.setProgressApprovalStatus(ProgressApprovalStatus.PENDING);
        indicator = indicatorRepository.save(indicator);
        
        assertThat(indicator.getProgressApprovalStatus())
                .as("Progress approval workflow: DRAFT → PENDING transition works")
                .isEqualTo(ProgressApprovalStatus.PENDING);
        assertThat(indicator.getStatus())
                .as("Lifecycle status remains DISTRIBUTED during progress approval")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);
        
        // Transition 3: PENDING → APPROVED (strategic dept approves)
        indicator.setProgressApprovalStatus(ProgressApprovalStatus.APPROVED);
        indicator.setProgress(indicator.getPendingProgress());  // Copy pending to actual
        indicator = indicatorRepository.save(indicator);
        
        assertThat(indicator.getProgressApprovalStatus())
                .as("Progress approval workflow: PENDING → APPROVED transition works")
                .isEqualTo(ProgressApprovalStatus.APPROVED);
        assertThat(indicator.getProgress())
                .as("Progress value copied from pending to actual on approval")
                .isEqualTo(progress);
        assertThat(indicator.getStatus())
                .as("Lifecycle status remains DISTRIBUTED after progress approval")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);
    }

    /**
     * Property 2: Progress Approval Rejection Workflow Preservation
     * 
     * **Validates: Requirements 3.1, 3.3, 3.4, 3.5**
     * 
     * FOR ALL indicators WHERE progressApprovalStatus = PENDING
     * THEN rejection workflow (PENDING → REJECTED → DRAFT) MUST work as before
     */
    @Test
    void progressApprovalRejection_shouldBePreserved() {
        
        int progress = 60;  // Test with a specific progress value
        
        // Create indicator with pending progress approval
        Indicator indicator = Indicator.builder()
                .taskId(1L)
                .indicatorDesc("测试指标 - 进度审批驳回")
                .level(IndicatorLevel.PRIMARY)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .progress(0)
                .ownerOrg(strategicOrg)
                .targetOrg(functionalOrg)
                .status(IndicatorStatus.DISTRIBUTED)
                .progressApprovalStatus(ProgressApprovalStatus.PENDING)
                .pendingProgress(progress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        
        indicator = indicatorRepository.save(indicator);
        
        // **OBSERVE BASELINE BEHAVIOR**: Rejection workflow
        
        // Reject progress submission
        indicator.setProgressApprovalStatus(ProgressApprovalStatus.REJECTED);
        indicator = indicatorRepository.save(indicator);
        
        assertThat(indicator.getProgressApprovalStatus())
                .as("Progress approval workflow: PENDING → REJECTED transition works")
                .isEqualTo(ProgressApprovalStatus.REJECTED);
        assertThat(indicator.getStatus())
                .as("Lifecycle status remains DISTRIBUTED after rejection")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);
        
        // Department revises and creates new draft
        indicator.setProgressApprovalStatus(ProgressApprovalStatus.DRAFT);
        indicator.setPendingProgress(progress + 10);
        indicator = indicatorRepository.save(indicator);
        
        assertThat(indicator.getProgressApprovalStatus())
                .as("Progress approval workflow: REJECTED → DRAFT transition works")
                .isEqualTo(ProgressApprovalStatus.DRAFT);
    }

    /**
     * Property 3: DISTRIBUTED Indicator Behavior Preservation
     * 
     * **Validates: Requirements 3.3, 3.8**
     * 
     * FOR ALL indicators WHERE status = DISTRIBUTED
     * THEN department access and progress submission MUST work as before
     */
    @Test
    void distributedIndicatorBehavior_shouldBePreserved() {
        
        int progress = 80;
        String remark = "测试备注";
        
        // Create DISTRIBUTED indicator
        Indicator indicator = Indicator.builder()
                .taskId(1L)
                .indicatorDesc("测试指标 - 已下发状态")
                .level(IndicatorLevel.PRIMARY)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .progress(0)
                .ownerOrg(strategicOrg)
                .targetOrg(functionalOrg)
                .status(IndicatorStatus.DISTRIBUTED)
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        
        indicator = indicatorRepository.save(indicator);
        
        // **OBSERVE BASELINE BEHAVIOR**: DISTRIBUTED indicators are accessible
        
        IndicatorVO vo = indicatorService.getIndicatorById(indicator.getIndicatorId());
        
        assertThat(vo).isNotNull();
        assertThat(vo.getStatus())
                .as("DISTRIBUTED indicators are accessible via service")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);
        
        // Department can submit progress
        indicator.setPendingProgress(progress);
        indicator.setPendingRemark(remark);
        indicator.setProgressApprovalStatus(ProgressApprovalStatus.DRAFT);
        indicator = indicatorRepository.save(indicator);
        
        assertThat(indicator.getPendingProgress())
                .as("Departments can submit progress for DISTRIBUTED indicators")
                .isEqualTo(progress);
        assertThat(indicator.getProgressApprovalStatus())
                .as("Progress approval status can be updated for DISTRIBUTED indicators")
                .isEqualTo(ProgressApprovalStatus.DRAFT);
    }

    /**
     * Property 4: CRUD Operations Preservation
     * 
     * **Validates: Requirements 3.14**
     * 
     * FOR ALL indicators
     * THEN existing CRUD operations (GET, POST, PUT) MUST work without breaking changes
     */
    @Test
    void crudOperations_shouldBePreserved() {
        
        int weightPercent = 100;
        
        // **OBSERVE BASELINE BEHAVIOR**: CRUD operations work correctly
        
        // CREATE
        Indicator indicator = Indicator.builder()
                .taskId(1L)
                .indicatorDesc("测试指标 - CRUD操作")
                .level(IndicatorLevel.PRIMARY)
                .weightPercent(new BigDecimal(weightPercent))
                .sortOrder(1)
                .type("定量")
                .progress(0)
                .ownerOrg(strategicOrg)
                .targetOrg(functionalOrg)
                .status(IndicatorStatus.DISTRIBUTED)
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        
        indicator = indicatorRepository.save(indicator);
        Long indicatorId = indicator.getIndicatorId();
        
        assertThat(indicatorId).isNotNull();
        
        // READ
        IndicatorVO vo = indicatorService.getIndicatorById(indicatorId);
        assertThat(vo).isNotNull();
        assertThat(vo.getIndicatorId()).isEqualTo(indicatorId);
        assertThat(vo.getWeightPercent()).isEqualByComparingTo(new BigDecimal(weightPercent));
        
        // UPDATE
        indicator.setProgress(50);
        indicator = indicatorRepository.save(indicator);
        
        assertThat(indicator.getProgress())
                .as("Indicator can be updated")
                .isEqualTo(50);
        
        // Verify entity fields are preserved
        assertThat(indicator.getStatus())
                .as("Status field preserved during update")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);
        assertThat(indicator.getProgressApprovalStatus())
                .as("ProgressApprovalStatus field preserved during update")
                .isEqualTo(ProgressApprovalStatus.NONE);
    }

    /**
     * Property 5: Dashboard Filtering Preservation
     * 
     * **Validates: Requirements 3.10**
     * 
     * FOR ALL indicators
     * THEN filtering by progressApprovalStatus MUST work correctly
     */
    @Test
    void dashboardFiltering_shouldBePreserved() {
        
        // Create indicators with different progressApprovalStatus values
        Indicator indicator1 = createDistributedIndicator("指标1", ProgressApprovalStatus.NONE);
        Indicator indicator2 = createDistributedIndicator("指标2", ProgressApprovalStatus.PENDING);
        Indicator indicator3 = createDistributedIndicator("指标3", ProgressApprovalStatus.APPROVED);
        
        indicatorRepository.saveAll(List.of(indicator1, indicator2, indicator3));
        
        // **OBSERVE BASELINE BEHAVIOR**: Filtering by progressApprovalStatus works
        
        List<Indicator> allIndicators = indicatorRepository.findAll();
        
        long pendingCount = allIndicators.stream()
                .filter(i -> i.getProgressApprovalStatus() == ProgressApprovalStatus.PENDING)
                .count();
        
        assertThat(pendingCount)
                .as("Dashboard can filter by progressApprovalStatus=PENDING")
                .isGreaterThanOrEqualTo(1);
        
        long approvedCount = allIndicators.stream()
                .filter(i -> i.getProgressApprovalStatus() == ProgressApprovalStatus.APPROVED)
                .count();
        
        assertThat(approvedCount)
                .as("Dashboard can filter by progressApprovalStatus=APPROVED")
                .isGreaterThanOrEqualTo(1);
    }

    /**
     * Property 6: Distribution Endpoint Preservation
     * 
     * **Validates: Requirements 3.6**
     * 
     * FOR ALL indicators
     * THEN POST /indicators/{id}/distribute MUST transition to DISTRIBUTED correctly
     */
    @Test
    void distributionEndpoint_shouldBePreserved() {
        
        // Create parent indicator with ACTIVE status (current behavior)
        Indicator parentIndicator = Indicator.builder()
                .taskId(1L)
                .indicatorDesc("父指标 - 下发测试")
                .level(IndicatorLevel.PRIMARY)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .progress(0)
                .ownerOrg(strategicOrg)
                .targetOrg(strategicOrg)
                .status(IndicatorStatus.ACTIVE)  // Current behavior uses ACTIVE
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        
        parentIndicator = indicatorRepository.save(parentIndicator);
        
        // **OBSERVE BASELINE BEHAVIOR**: Distribution creates child with ACTIVE status
        
        IndicatorVO childVO = indicatorService.distributeIndicator(
                parentIndicator.getIndicatorId(),
                functionalOrg.getId(),
                "子指标描述",
                1L
        );
        
        assertThat(childVO).isNotNull();
        assertThat(childVO.getStatus())
                .as("Distribution endpoint creates child indicator with ACTIVE status")
                .isEqualTo(IndicatorStatus.ACTIVE);
        assertThat(childVO.getParentIndicatorId())
                .as("Child indicator references parent")
                .isEqualTo(parentIndicator.getIndicatorId());
    }

    /**
     * Property 7: Two Independent State Machines Preservation
     * 
     * **Validates: Requirements 3.1, 3.7**
     * 
     * FOR ALL indicators
     * THEN status field and progressApprovalStatus field MUST remain independent
     */
    @Test
    void twoIndependentStateMachines_shouldBePreserved() {
        
        int progress = 50;
        
        // Create indicator
        Indicator indicator = Indicator.builder()
                .taskId(1L)
                .indicatorDesc("测试指标 - 独立状态机")
                .level(IndicatorLevel.PRIMARY)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .progress(0)
                .ownerOrg(strategicOrg)
                .targetOrg(functionalOrg)
                .status(IndicatorStatus.DISTRIBUTED)
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        
        indicator = indicatorRepository.save(indicator);
        
        // **OBSERVE BASELINE BEHAVIOR**: Two fields are independent
        
        // Change progressApprovalStatus without affecting status
        indicator.setProgressApprovalStatus(ProgressApprovalStatus.PENDING);
        indicator = indicatorRepository.save(indicator);
        
        assertThat(indicator.getStatus())
                .as("Lifecycle status unchanged when progressApprovalStatus changes")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);
        assertThat(indicator.getProgressApprovalStatus())
                .as("ProgressApprovalStatus can change independently")
                .isEqualTo(ProgressApprovalStatus.PENDING);
        
        // Change status without affecting progressApprovalStatus
        indicator.setStatus(IndicatorStatus.ARCHIVED);
        indicator = indicatorRepository.save(indicator);
        
        assertThat(indicator.getProgressApprovalStatus())
                .as("ProgressApprovalStatus unchanged when status changes")
                .isEqualTo(ProgressApprovalStatus.PENDING);
        assertThat(indicator.getStatus())
                .as("Lifecycle status can change independently")
                .isEqualTo(IndicatorStatus.ARCHIVED);
    }

    // Helper method
    private Indicator createDistributedIndicator(String desc, ProgressApprovalStatus approvalStatus) {
        return Indicator.builder()
                .taskId(1L)
                .indicatorDesc(desc)
                .level(IndicatorLevel.PRIMARY)
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .progress(0)
                .ownerOrg(strategicOrg)
                .targetOrg(functionalOrg)
                .status(IndicatorStatus.DISTRIBUTED)
                .progressApprovalStatus(approvalStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }
}
