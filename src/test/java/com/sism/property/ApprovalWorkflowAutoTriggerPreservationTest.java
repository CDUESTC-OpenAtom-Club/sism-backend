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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preservation Property Tests for Approval Workflow Auto-Trigger Fix
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
 * 
 * **Property 2: Preservation** - Non-Distribution Updates Unchanged
 * 
 * These tests verify that non-distribution indicator updates continue to work exactly as before
 * and do NOT trigger approval workflow creation. These tests should PASS on both unfixed and fixed code.
 * 
 * **IMPORTANT**: Follow observation-first methodology - observe behavior on UNFIXED code first,
 * then write tests capturing that exact behavior.
 * 
 * **EXPECTED OUTCOME**: Tests PASS on unfixed code (confirms baseline behavior to preserve).
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class ApprovalWorkflowAutoTriggerPreservationTest extends AbstractIntegrationTest {

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
     * Property 2.1: Indicator Description Edit Should NOT Create Approval Instance
     * 
     * EXPECTED BEHAVIOR: When editing indicator description without distribution action,
     * no approval instance should be created. This behavior must be preserved after the fix.
     * 
     * This test should PASS on both unfixed and fixed code.
     */
    @Property(tries = 20)
    @Label("Bugfix: approval-workflow-auto-trigger-fix, Property 2.1: Description Edit Preservation")
    void indicatorDescriptionEdit_shouldNotCreateApprovalInstance(
            @ForAll("indicators") Indicator indicator,
            @ForAll("descriptions") String newDescription) {

        // Save the indicator
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // Create edit request (description change only, no statusAudit)
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setIndicatorDesc(newDescription);

        // Execute: Update indicator description
        IndicatorVO result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // EXPECTED BEHAVIOR: No approval instance should be created
        Optional<AuditInstance> approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());

        // Assert: No approval instance exists (should pass on both unfixed and fixed code)
        assertThat(approvalInstance)
                .as("Indicator description edit should NOT create approval instance")
                .isEmpty();

        // Verify indicator was updated successfully
        assertThat(result.getIndicatorDesc()).isEqualTo(newDescription);
    }

    /**
     * Property 2.2: Indicator Progress Update Should NOT Create Approval Instance
     * 
     * EXPECTED BEHAVIOR: When updating indicator progress/values without distribution action,
     * no approval instance should be created. This behavior must be preserved after the fix.
     * 
     * This test should PASS on both unfixed and fixed code.
     */
    @Property(tries = 20)
    @Label("Bugfix: approval-workflow-auto-trigger-fix, Property 2.2: Progress Update Preservation")
    void indicatorProgressUpdate_shouldNotCreateApprovalInstance(
            @ForAll("indicators") Indicator indicator,
            @ForAll("progressValues") Integer newProgress) {

        // Save the indicator
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // Create progress update request (no statusAudit)
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setProgress(newProgress);

        // Execute: Update indicator progress
        IndicatorVO result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // EXPECTED BEHAVIOR: No approval instance should be created
        Optional<AuditInstance> approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());

        // Assert: No approval instance exists (should pass on both unfixed and fixed code)
        assertThat(approvalInstance)
                .as("Indicator progress update should NOT create approval instance")
                .isEmpty();

        // Verify indicator was updated successfully
        assertThat(result.getProgress()).isEqualTo(newProgress);
    }

    /**
     * Property 2.3: Null StatusAudit Should NOT Create Approval Instance
     * 
     * EXPECTED BEHAVIOR: When updating indicators with null statusAudit,
     * no approval instance should be created. This behavior must be preserved after the fix.
     * 
     * This test should PASS on both unfixed and fixed code.
     */
    @Property(tries = 20)
    @Label("Bugfix: approval-workflow-auto-trigger-fix, Property 2.3: Null StatusAudit Preservation")
    void nullStatusAudit_shouldNotCreateApprovalInstance(
            @ForAll("indicators") Indicator indicator,
            @ForAll("descriptions") String newDescription,
            @ForAll("progressValues") Integer newProgress) {

        // Save the indicator
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // Create update request with null statusAudit
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setIndicatorDesc(newDescription);
        request.setProgress(newProgress);
        request.setStatusAudit(null); // Explicitly null

        // Execute: Update indicator with null statusAudit
        IndicatorVO result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // EXPECTED BEHAVIOR: No approval instance should be created
        Optional<AuditInstance> approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());

        // Assert: No approval instance exists (should pass on both unfixed and fixed code)
        assertThat(approvalInstance)
                .as("Updates with null statusAudit should NOT create approval instance")
                .isEmpty();

        // Verify indicator was updated successfully
        assertThat(result.getIndicatorDesc()).isEqualTo(newDescription);
        assertThat(result.getProgress()).isEqualTo(newProgress);
    }

    /**
     * Property 2.4: Non-Distribute StatusAudit Actions Should NOT Create Approval Instance
     * 
     * EXPECTED BEHAVIOR: When updating indicators with statusAudit containing actions other than "distribute",
     * no approval instance should be created. This behavior must be preserved after the fix.
     * 
     * This test should PASS on both unfixed and fixed code.
     */
    @Property(tries = 20)
    @Label("Bugfix: approval-workflow-auto-trigger-fix, Property 2.4: Non-Distribute Actions Preservation")
    void nonDistributeStatusAuditActions_shouldNotCreateApprovalInstance(
            @ForAll("indicators") Indicator indicator,
            @ForAll("nonDistributeActions") String action) {

        // Save the indicator
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // Create update request with non-distribute statusAudit action
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setStatusAudit(createStatusAuditWithAction(action));

        // Execute: Update indicator with non-distribute action
        IndicatorVO result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // EXPECTED BEHAVIOR: No approval instance should be created
        Optional<AuditInstance> approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());

        // Assert: No approval instance exists (should pass on both unfixed and fixed code)
        assertThat(approvalInstance)
                .as("StatusAudit with action '" + action + "' should NOT create approval instance")
                .isEmpty();
    }

    /**
     * Property 2.5: Empty StatusAudit Array Should NOT Create Approval Instance
     * 
     * EXPECTED BEHAVIOR: When updating indicators with empty statusAudit array,
     * no approval instance should be created. This behavior must be preserved after the fix.
     * 
     * This test should PASS on both unfixed and fixed code.
     */
    @Property(tries = 10)
    @Label("Bugfix: approval-workflow-auto-trigger-fix, Property 2.5: Empty StatusAudit Preservation")
    void emptyStatusAuditArray_shouldNotCreateApprovalInstance(
            @ForAll("indicators") Indicator indicator,
            @ForAll("descriptions") String newDescription) {

        // Save the indicator
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // Create update request with empty statusAudit array
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setIndicatorDesc(newDescription);
        request.setStatusAudit("[]"); // Empty JSON array

        // Execute: Update indicator with empty statusAudit
        IndicatorVO result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // EXPECTED BEHAVIOR: No approval instance should be created
        Optional<AuditInstance> approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());

        // Assert: No approval instance exists (should pass on both unfixed and fixed code)
        assertThat(approvalInstance)
                .as("Empty statusAudit array should NOT create approval instance")
                .isEmpty();

        // Verify indicator was updated successfully
        assertThat(result.getIndicatorDesc()).isEqualTo(newDescription);
    }

    /**
     * Property 2.6: Status Change to Non-Distributed States Should NOT Create Approval Instance
     * 
     * EXPECTED BEHAVIOR: When changing indicator status to non-distributed states,
     * no approval instance should be created. This behavior must be preserved after the fix.
     * 
     * This test should PASS on both unfixed and fixed code.
     */
    @Property(tries = 20)
    @Label("Bugfix: approval-workflow-auto-trigger-fix, Property 2.6: Non-Distributed Status Change Preservation")
    void nonDistributedStatusChange_shouldNotCreateApprovalInstance(
            @ForAll("indicators") Indicator indicator,
            @ForAll("nonDistributeActions") String action) {

        // Save the indicator
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // Create status change request with non-distribute action
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setStatusAudit(createStatusAuditWithAction(action));

        // Execute: Update indicator status to non-distributed state
        IndicatorVO result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // EXPECTED BEHAVIOR: No approval instance should be created
        Optional<AuditInstance> approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());

        // Assert: No approval instance exists (should pass on both unfixed and fixed code)
        assertThat(approvalInstance)
                .as("Status change to non-distributed state should NOT create approval instance")
                .isEmpty();
    }

    // ==================== Arbitraries (Test Data Generators) ====================

    /**
     * Generate random indicators with various department types
     */
    @Provide
    Arbitrary<Indicator> indicators() {
        return Arbitraries.of(
                "计算机学院",
                "经济管理学院",
                "行政办公室",
                "人力资源部",
                "财务部",
                "战略发展部"
        ).map(this::createIndicator);
    }

    /**
     * Generate random description strings
     */
    @Provide
    Arbitrary<String> descriptions() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(10)
                .ofMaxLength(100)
                .map(s -> "Updated description: " + s);
    }

    /**
     * Generate random progress values (0-100)
     */
    @Provide
    Arbitrary<Integer> progressValues() {
        return Arbitraries.integers().between(0, 100);
    }

    /**
     * Generate non-distribute action types
     */
    @Provide
    Arbitrary<String> nonDistributeActions() {
        return Arbitraries.of(
                "create",
                "update",
                "edit",
                "modify",
                "review",
                "approve",
                "reject",
                "cancel",
                "archive",
                "restore"
        );
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
     * Create statusAudit JSON with specified action
     */
    private String createStatusAuditWithAction(String action) {
        try {
            ArrayNode auditArray = objectMapper.createArrayNode();
            ObjectNode auditRecord = objectMapper.createObjectNode();
            auditRecord.put("action", action);
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
