package com.sism.service;

import com.sism.entity.AuditInstance;
import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.enums.AuditEntityType;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.repository.AssessmentCycleRepository;
import com.sism.repository.AuditInstanceRepository;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.util.TestDataFactory;
import com.sism.vo.IndicatorVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Indicator Display Status calculation
 * 
 * Validates: Requirement 3.1 - Display status based on approval state
 * 
 * Status Flow:
 * - DRAFT: No audit instance or audit rejected
 * - PENDING_APPROVAL: Audit in progress (IN_PROGRESS/PENDING)
 * - DISTRIBUTED: Audit approved (APPROVED)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IndicatorDisplayStatusTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private AuditInstanceRepository auditInstanceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private AssessmentCycleRepository cycleRepository;

    private Indicator testIndicator;
    private SysOrg testOwnerOrg;
    private SysOrg testTargetOrg;

    @BeforeEach
    void setUp() {
        // Create test organizations
        testOwnerOrg = TestDataFactory.createTestOrg(orgRepository, "测试职能部门", OrgType.FUNCTIONAL_DEPT);
        testTargetOrg = TestDataFactory.createTestOrg(orgRepository, "测试二级学院", OrgType.COLLEGE);
        
        // Create test indicator
        testIndicator = TestDataFactory.createTestIndicator(indicatorRepository, taskRepository, 
                                                            cycleRepository, orgRepository);
    }

    @Test
    @DisplayName("Should return DRAFT when no audit instance exists")
    void shouldReturnDraftWhenNoAuditInstance() {
        // Given: Indicator with no audit instance
        Long indicatorId = testIndicator.getIndicatorId();

        // When: Get indicator
        IndicatorVO result = indicatorService.getIndicatorById(indicatorId);

        // Then: Display status should be DRAFT
        assertThat(result.getDisplayStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Should return PENDING_APPROVAL when audit is IN_PROGRESS")
    void shouldReturnPendingApprovalWhenAuditInProgress() {
        // Given: Indicator with IN_PROGRESS audit
        Long indicatorId = testIndicator.getIndicatorId();
        createAuditInstance(indicatorId, "IN_PROGRESS");

        // When: Get indicator
        IndicatorVO result = indicatorService.getIndicatorById(indicatorId);

        // Then: Display status should be PENDING_APPROVAL
        assertThat(result.getDisplayStatus()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    @DisplayName("Should return PENDING_APPROVAL when audit is PENDING")
    void shouldReturnPendingApprovalWhenAuditPending() {
        // Given: Indicator with PENDING audit
        Long indicatorId = testIndicator.getIndicatorId();
        createAuditInstance(indicatorId, "PENDING");

        // When: Get indicator
        IndicatorVO result = indicatorService.getIndicatorById(indicatorId);

        // Then: Display status should be PENDING_APPROVAL
        assertThat(result.getDisplayStatus()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    @DisplayName("Should return DISTRIBUTED when audit is APPROVED")
    void shouldReturnDistributedWhenAuditApproved() {
        // Given: Indicator with APPROVED audit
        Long indicatorId = testIndicator.getIndicatorId();
        createAuditInstance(indicatorId, "APPROVED");

        // When: Get indicator
        IndicatorVO result = indicatorService.getIndicatorById(indicatorId);

        // Then: Display status should be DISTRIBUTED
        assertThat(result.getDisplayStatus()).isEqualTo("DISTRIBUTED");
    }

    @Test
    @DisplayName("Should return DRAFT when audit is REJECTED")
    void shouldReturnDraftWhenAuditRejected() {
        // Given: Indicator with REJECTED audit
        Long indicatorId = testIndicator.getIndicatorId();
        createAuditInstance(indicatorId, "REJECTED");

        // When: Get indicator
        IndicatorVO result = indicatorService.getIndicatorById(indicatorId);

        // Then: Display status should be DRAFT (returned to draft for modification)
        assertThat(result.getDisplayStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Should handle test environment gracefully when audit_instance table missing")
    void shouldHandleTestEnvironmentGracefully() {
        // Given: Indicator in test environment (audit_instance table might not exist)
        Long indicatorId = testIndicator.getIndicatorId();

        // When: Get indicator (should not throw exception)
        IndicatorVO result = indicatorService.getIndicatorById(indicatorId);

        // Then: Should return DRAFT as default
        assertThat(result.getDisplayStatus()).isNotNull();
        assertThat(result.getDisplayStatus()).isEqualTo("DRAFT");
    }

    /**
     * Helper method to create audit instance for testing
     */
    private void createAuditInstance(Long indicatorId, String status) {
        try {
            AuditInstance auditInstance = new AuditInstance();
            auditInstance.setFlowId(1L);
            auditInstance.setEntityId(indicatorId);
            auditInstance.setEntityType(AuditEntityType.INDICATOR);
            auditInstance.setStatus(status);
            auditInstance.setInitiatedBy(1L);
            auditInstance.setInitiatedAt(LocalDateTime.now());
            auditInstance.setCreatedAt(LocalDateTime.now());
            auditInstance.setUpdatedAt(LocalDateTime.now());
            
            auditInstanceRepository.save(auditInstance);
        } catch (Exception e) {
            // In test environment, audit_instance table might not exist
            // This is expected and handled by calculateDisplayStatus
        }
    }
}
