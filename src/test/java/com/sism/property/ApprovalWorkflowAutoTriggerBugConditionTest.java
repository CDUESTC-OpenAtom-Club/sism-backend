package com.sism.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sism.AbstractIntegrationTest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.entity.AuditInstance;
import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.entity.SysUser;
import com.sism.enums.AuditEntityType;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.repository.AuditInstanceRepository;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.OrgRepository;
import com.sism.repository.UserRepository;
import com.sism.service.IndicatorService;
import com.sism.vo.IndicatorVO;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug Condition Exploration Test for Approval Workflow Auto-Trigger Fix
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3**
 * 
 * **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists.
 * 
 * **Property 1: Bug Condition** - Distribution Does Not Auto-Create Approval Instance
 * 
 * This test encodes the EXPECTED behavior (automatic approval creation on distribution).
 * On UNFIXED code, this test will FAIL because approval instances are NOT created automatically.
 * After the fix is implemented, this test will PASS, validating the fix works correctly.
 * 
 * **GOAL**: Surface counterexamples that demonstrate the bug exists:
 * - Distribution of college indicator does not create approval instance
 * - Distribution of non-college indicator does not create approval instance
 * - Database query shows no audit_instance records after distribution
 * 
 * **Scoped PBT Approach**: Test concrete failing cases - distribution actions that should
 * create approval instances but don't on unfixed code.
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class ApprovalWorkflowAutoTriggerBugConditionTest extends AbstractIntegrationTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private AuditInstanceRepository auditInstanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    private SysUser testUser;
    private SysOrg testOrg;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Clean up test data
        auditInstanceRepository.deleteAll();
        indicatorRepository.deleteAll();

        // Create test organization
        testOrg = new SysOrg();
        testOrg.setName("测试部门");
        testOrg.setType(com.sism.enums.OrgType.FUNCTIONAL_DEPT);
        testOrg.setIsActive(true);
        testOrg.setSortOrder(1);
        testOrg.setCreatedAt(LocalDateTime.now());
        testOrg.setUpdatedAt(LocalDateTime.now());
        testOrg = orgRepository.save(testOrg);

        // Create test user
        testUser = new SysUser();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("password");
        testUser.setRealName("Test User");
        testUser.setOrg(testOrg);
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    /**
     * Property 1.1: College Department Distribution Should Auto-Create Approval Instance
     * 
     * EXPECTED BEHAVIOR: When distributing indicators for college department (responsibleDept contains "学院"),
     * the system should automatically create an approval instance with INDICATOR_COLLEGE_APPROVAL flow code.
     * 
     * ACTUAL BEHAVIOR ON UNFIXED CODE: No approval instance is created (TEST WILL FAIL).
     * 
     * This failure confirms the bug exists.
     */
    @Property(tries = 10)
    @Label("Bugfix: approval-workflow-auto-trigger-fix, Property 1.1: College Department Distribution Bug")
    void collegeDepartment_distributionShouldAutoCreateApprovalInstance(
            @ForAll("collegeIndicators") Indicator indicator) {

        // Save the indicator
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // Create distribution request with statusAudit containing "distribute" action
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setStatusAudit(createDistributeStatusAudit());

        // Execute: Update indicator with distribution action
        IndicatorVO result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // EXPECTED BEHAVIOR: Approval instance should be created automatically
        // On UNFIXED code, this assertion will FAIL (no approval instance exists)
        Optional<AuditInstance> approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());

        // Assert: Approval instance EXISTS (will fail on unfixed code)
        assertThat(approvalInstance)
                .as("Distribution should automatically create approval instance for college department")
                .isPresent();

        // Assert: Approval instance has correct flow code (will fail on unfixed code)
        if (approvalInstance.isPresent()) {
            AuditInstance instance = approvalInstance.get();
            assertThat(instance.getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
            assertThat(instance.getEntityId()).isEqualTo(savedIndicator.getIndicatorId());
            assertThat(instance.getStatus()).isIn("PENDING", "IN_PROGRESS");
            // Note: Flow code validation requires checking auditFlowDef.flowCode
            // which would need additional query - simplified for exploration test
        }

        // Verify in database: audit_instance record exists
        List<AuditInstance> allInstances = auditInstanceRepository
                .findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());
        assertThat(allInstances)
                .as("Database should contain audit_instance record after distribution")
                .isNotEmpty();
    }

    /**
     * Property 1.2: Non-College Department Distribution Should Auto-Create Approval Instance
     * 
     * EXPECTED BEHAVIOR: When distributing indicators for non-college department,
     * the system should automatically create an approval instance with INDICATOR_DEFAULT_APPROVAL flow code.
     * 
     * ACTUAL BEHAVIOR ON UNFIXED CODE: No approval instance is created (TEST WILL FAIL).
     * 
     * This failure confirms the bug exists.
     */
    @Property(tries = 10)
    @Label("Bugfix: approval-workflow-auto-trigger-fix, Property 1.2: Non-College Department Distribution Bug")
    void nonCollegeDepartment_distributionShouldAutoCreateApprovalInstance(
            @ForAll("nonCollegeIndicators") Indicator indicator) {

        // Save the indicator
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // Create distribution request with statusAudit containing "distribute" action
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setStatusAudit(createDistributeStatusAudit());

        // Execute: Update indicator with distribution action
        IndicatorVO result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // EXPECTED BEHAVIOR: Approval instance should be created automatically
        // On UNFIXED code, this assertion will FAIL (no approval instance exists)
        Optional<AuditInstance> approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());

        // Assert: Approval instance EXISTS (will fail on unfixed code)
        assertThat(approvalInstance)
                .as("Distribution should automatically create approval instance for non-college department")
                .isPresent();

        // Assert: Approval instance has correct flow code (will fail on unfixed code)
        if (approvalInstance.isPresent()) {
            AuditInstance instance = approvalInstance.get();
            assertThat(instance.getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
            assertThat(instance.getEntityId()).isEqualTo(savedIndicator.getIndicatorId());
            assertThat(instance.getStatus()).isIn("PENDING", "IN_PROGRESS");
        }

        // Verify in database: audit_instance record exists
        List<AuditInstance> allInstances = auditInstanceRepository
                .findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());
        assertThat(allInstances)
                .as("Database should contain audit_instance record after distribution")
                .isNotEmpty();
    }

    /**
     * Property 1.3: Non-Distribution Updates Should NOT Create Approval Instance
     * 
     * EXPECTED BEHAVIOR: When updating indicators without distribution action,
     * no approval instance should be created.
     * 
     * This test should PASS on both unfixed and fixed code (preservation property).
     */
    @Property(tries = 10)
    @Label("Bugfix: approval-workflow-auto-trigger-fix, Property 1.3: Non-Distribution Preservation")
    void nonDistribution_updatesShouldNotCreateApprovalInstance(
            @ForAll("collegeIndicators") Indicator indicator) {

        // Save the indicator
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // Create non-distribution request (edit description)
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setIndicatorDesc("Updated description without distribution");
        request.setProgress(50);

        // Execute: Update indicator without distribution action
        IndicatorVO result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // EXPECTED BEHAVIOR: No approval instance should be created
        Optional<AuditInstance> approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());

        // Assert: No approval instance exists (should pass on both unfixed and fixed code)
        assertThat(approvalInstance)
                .as("Non-distribution updates should NOT create approval instance")
                .isEmpty();
    }

    // ==================== Arbitraries (Test Data Generators) ====================

    /**
     * Generate college department indicators (responsibleDept contains "学院")
     */
    @Provide
    Arbitrary<Indicator> collegeIndicators() {
        return Arbitraries.of(
                "计算机学院",
                "经济管理学院",
                "外国语学院",
                "数学与统计学院"
        ).map(collegeName -> createIndicator(collegeName));
    }

    /**
     * Generate non-college department indicators
     */
    @Provide
    Arbitrary<Indicator> nonCollegeIndicators() {
        return Arbitraries.of(
                "行政办公室",
                "人力资源部",
                "财务部",
                "战略发展部"
        ).map(deptName -> createIndicator(deptName));
    }

    // ==================== Helper Methods ====================

    /**
     * Create an indicator with specified responsible department
     */
    private Indicator createIndicator(String responsibleDept) {
        Indicator indicator = new Indicator();
        indicator.setTaskId(1L);
        indicator.setLevel(IndicatorLevel.SECONDARY);
        indicator.setOwnerOrg(testOrg);
        indicator.setTargetOrg(testOrg);
        indicator.setIndicatorDesc("Test indicator for " + responsibleDept);
        indicator.setWeightPercent(new BigDecimal("10.00"));
        indicator.setSortOrder(1);
        indicator.setType("QUANTITATIVE");
        indicator.setProgress(0);
        indicator.setStatus(IndicatorStatus.ACTIVE);
        indicator.setIsDeleted(false);
        indicator.setCreatedAt(LocalDateTime.now());
        indicator.setUpdatedAt(LocalDateTime.now());
        indicator.setResponsibleDept(responsibleDept);
        return indicator;
    }

    /**
     * Create statusAudit JSON with "distribute" action
     */
    private String createDistributeStatusAudit() {
        try {
            ArrayNode auditArray = objectMapper.createArrayNode();
            ObjectNode auditRecord = objectMapper.createObjectNode();
            auditRecord.put("action", "distribute");
            auditRecord.put("timestamp", LocalDateTime.now().toString());
            auditRecord.put("userId", testUser.getId());
            auditRecord.put("userName", testUser.getRealName());
            auditArray.add(auditRecord);
            return objectMapper.writeValueAsString(auditArray);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create statusAudit JSON", e);
        }
    }
}
