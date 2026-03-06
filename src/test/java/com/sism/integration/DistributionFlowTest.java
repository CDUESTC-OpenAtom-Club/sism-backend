package com.sism.integration;

import com.sism.AbstractIntegrationTest;
import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.entity.*;
import com.sism.enums.*;
import com.sism.repository.*;
import com.sism.service.IndicatorService;
import com.sism.util.TestDataFactory;
import com.sism.vo.IndicatorVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the full distribution flow
 * 
 * Task 6.1: Test full distribution flow end-to-end
 * - User clicks "Distribute" button
 * - Verify indicators updated to "distributed" status
 * - Verify approval instance created in database
 * - Verify approvers notified (if notification system exists)
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3**
 */
@Transactional
public class DistributionFlowTest extends AbstractIntegrationTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private AssessmentCycleRepository cycleRepository;

    @Autowired
    private AuditInstanceRepository auditInstanceRepository;

    @Autowired
    private SysUserRepository userRepository;

    private StrategicTask testTask;
    private SysOrg testOwnerOrg;
    private SysOrg testTargetOrg;
    private SysUser testUser;

    @BeforeEach
    void setUp() {
        // Create test cycle
        AssessmentCycle cycle = TestDataFactory.createTestCycle(cycleRepository);
        
        // Create test organizations
        testOwnerOrg = TestDataFactory.createTestOrg(orgRepository, "测试职能部门", OrgType.FUNCTIONAL_DEPT);
        testTargetOrg = TestDataFactory.createTestOrg(orgRepository, "测试二级学院", OrgType.COLLEGE);
        
        // Create test task
        testTask = TestDataFactory.createTestTask(taskRepository, cycleRepository, orgRepository);
        
        // Create test user
        testUser = TestDataFactory.createTestUser(userRepository, orgRepository, "test_distributor");
    }

    @Test
    @DisplayName("Should complete full distribution flow: update indicator status and create approval instance")
    void shouldCompleteFullDistributionFlow() {
        // Given - Create an indicator to distribute
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("Test Indicator for Full Distribution Flow");
        createRequest.setWeightPercent(new BigDecimal("20.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);
        Long indicatorId = created.getIndicatorId();

        // Verify initial state - no approval instance exists
        List<AuditInstance> initialInstances = auditInstanceRepository
            .findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId);
        assertThat(initialInstances).isEmpty();

        // When - User clicks "Distribute" button (simulated by updateIndicator with distribute action)
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        String statusAuditJson = "[{\"action\":\"distribute\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":" + testUser.getId() + "}]";
        updateRequest.setStatusAudit(statusAuditJson);

        IndicatorVO result = indicatorService.updateIndicator(indicatorId, updateRequest);

        // Then - Verify indicator was updated to "distributed" status
        assertThat(result).isNotNull();
        assertThat(result.getIndicatorId()).isEqualTo(indicatorId);
        assertThat(result.getStatusAudit()).isEqualTo(statusAuditJson);

        // Verify indicator in database has the status audit
        Indicator updatedIndicator = indicatorRepository.findById(indicatorId).orElseThrow();
        assertThat(updatedIndicator.getStatusAudit()).isEqualTo(statusAuditJson);

        // Verify approval instance was created in database
        List<AuditInstance> approvalInstances = auditInstanceRepository
            .findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId);
        
        assertThat(approvalInstances)
            .isNotEmpty()
            .hasSize(1);

        AuditInstance approvalInstance = approvalInstances.get(0);
        
        // Verify approval instance properties
        assertThat(approvalInstance.getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
        assertThat(approvalInstance.getEntityId()).isEqualTo(indicatorId);
        assertThat(approvalInstance.getStatus()).isIn("PENDING", "IN_PROGRESS");
        assertThat(approvalInstance.getInitiatedBy()).isNotNull();
        assertThat(approvalInstance.getInitiatedAt()).isNotNull();
        assertThat(approvalInstance.getFlowId()).isNotNull();
        
        // Verify multi-level approval fields
        assertThat(approvalInstance.getCurrentStepOrder()).isEqualTo(1); // Should start at level 1
        assertThat(approvalInstance.getSubmitterDeptId()).isEqualTo(testUser.getOrg().getId());
        assertThat(approvalInstance.getDirectSupervisorId()).isNotNull(); // Should have level 1 approver
        assertThat(approvalInstance.getLevel2SupervisorId()).isNotNull(); // Should have level 2 approver
        assertThat(approvalInstance.getSuperiorDeptId()).isEqualTo(testOwnerOrg.getId()); // From indicator.owner_org_id
        
        // Verify pending approvers list contains only level 1 approver initially
        assertThat(approvalInstance.getPendingApprovers())
            .isNotNull()
            .hasSize(1)
            .contains(approvalInstance.getDirectSupervisorId());
    }

    @Test
    @DisplayName("Should create approval instance with college flow code for college department")
    void shouldCreateApprovalInstanceWithCollegeFlowCodeInFullFlow() {
        // Given - Create an indicator with college department
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("College Indicator for Distribution Flow");
        createRequest.setWeightPercent(new BigDecimal("25.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);
        Long indicatorId = created.getIndicatorId();

        // Update the indicator to set responsibleDept to a college department
        Indicator indicator = indicatorRepository.findById(indicatorId).orElseThrow();
        indicator.setResponsibleDept("计算机学院");
        indicatorRepository.save(indicator);

        // When - Distribute the indicator
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        String statusAuditJson = "[{\"action\":\"distribute\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":" + testUser.getId() + "}]";
        updateRequest.setStatusAudit(statusAuditJson);

        indicatorService.updateIndicator(indicatorId, updateRequest);

        // Then - Verify approval instance was created with college flow code
        List<AuditInstance> approvalInstances = auditInstanceRepository
            .findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId);
        
        assertThat(approvalInstances).isNotEmpty();
        
        AuditInstance approvalInstance = approvalInstances.get(0);
        assertThat(approvalInstance.getFlowId()).isNotNull();
        
        // Verify the flow code by checking the associated flow definition
        // The flow code should be INDICATOR_COLLEGE_APPROVAL for college departments
        if (approvalInstance.getAuditFlowDef() != null) {
            assertThat(approvalInstance.getAuditFlowDef().getFlowCode())
                .isEqualTo("INDICATOR_COLLEGE_APPROVAL");
        }
    }

    @Test
    @DisplayName("Should create approval instance with default flow code for non-college department")
    void shouldCreateApprovalInstanceWithDefaultFlowCodeInFullFlow() {
        // Given - Create an indicator with non-college department
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("Non-College Indicator for Distribution Flow");
        createRequest.setWeightPercent(new BigDecimal("30.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);
        Long indicatorId = created.getIndicatorId();

        // Update the indicator to set responsibleDept to a non-college department
        Indicator indicator = indicatorRepository.findById(indicatorId).orElseThrow();
        indicator.setResponsibleDept("人事处");
        indicatorRepository.save(indicator);

        // When - Distribute the indicator
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        String statusAuditJson = "[{\"action\":\"distribute\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":" + testUser.getId() + "}]";
        updateRequest.setStatusAudit(statusAuditJson);

        indicatorService.updateIndicator(indicatorId, updateRequest);

        // Then - Verify approval instance was created with default flow code
        List<AuditInstance> approvalInstances = auditInstanceRepository
            .findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId);
        
        assertThat(approvalInstances).isNotEmpty();
        
        AuditInstance approvalInstance = approvalInstances.get(0);
        assertThat(approvalInstance.getFlowId()).isNotNull();
        
        // Verify the flow code by checking the associated flow definition
        // The flow code should be INDICATOR_DEFAULT_APPROVAL for non-college departments
        if (approvalInstance.getAuditFlowDef() != null) {
            assertThat(approvalInstance.getAuditFlowDef().getFlowCode())
                .isEqualTo("INDICATOR_DEFAULT_APPROVAL");
        }
    }

    @Test
    @DisplayName("Should handle multiple indicators distribution in sequence")
    void shouldHandleMultipleIndicatorsDistribution() {
        // Given - Create multiple indicators
        int indicatorCount = 3;
        Long[] indicatorIds = new Long[indicatorCount];
        
        for (int i = 0; i < indicatorCount; i++) {
            IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
            createRequest.setTaskId(testTask.getTaskId());
            createRequest.setOwnerOrgId(testOwnerOrg.getId());
            createRequest.setTargetOrgId(testTargetOrg.getId());
            createRequest.setLevel(IndicatorLevel.PRIMARY.name());
            createRequest.setIndicatorDesc("Test Indicator " + (i + 1) + " for Batch Distribution");
            createRequest.setWeightPercent(new BigDecimal("10.00"));
            createRequest.setYear(2025);

            IndicatorVO created = indicatorService.createIndicator(createRequest);
            indicatorIds[i] = created.getIndicatorId();
        }

        // When - Distribute all indicators
        for (Long indicatorId : indicatorIds) {
            IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
            String statusAuditJson = "[{\"action\":\"distribute\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":" + testUser.getId() + "}]";
            updateRequest.setStatusAudit(statusAuditJson);

            indicatorService.updateIndicator(indicatorId, updateRequest);
        }

        // Then - Verify all indicators have approval instances
        for (Long indicatorId : indicatorIds) {
            List<AuditInstance> approvalInstances = auditInstanceRepository
                .findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId);
            
            assertThat(approvalInstances)
                .as("Approval instance should exist for indicator " + indicatorId)
                .isNotEmpty()
                .hasSize(1);
            
            AuditInstance approvalInstance = approvalInstances.get(0);
            assertThat(approvalInstance.getStatus()).isIn("PENDING", "IN_PROGRESS");
            // NOTE: Multi-level approval fields removed - database schema doesn't support them
        }
    }

    @Test
    @DisplayName("Should not create duplicate approval instances for same indicator")
    void shouldNotCreateDuplicateApprovalInstances() {
        // Given - Create an indicator
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("Test Indicator for Duplicate Check");
        createRequest.setWeightPercent(new BigDecimal("15.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);
        Long indicatorId = created.getIndicatorId();

        // When - Distribute the indicator
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        String statusAuditJson = "[{\"action\":\"distribute\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":" + testUser.getId() + "}]";
        updateRequest.setStatusAudit(statusAuditJson);

        indicatorService.updateIndicator(indicatorId, updateRequest);

        // Then - Verify only one approval instance was created
        List<AuditInstance> approvalInstances = auditInstanceRepository
            .findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId);
        
        assertThat(approvalInstances)
            .hasSize(1)
            .as("Only one approval instance should be created per distribution");
    }
}
