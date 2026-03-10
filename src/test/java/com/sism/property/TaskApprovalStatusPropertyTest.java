package com.sism.property;

import com.sism.config.TestSecurityConfig;
import com.sism.entity.Indicator;
import com.sism.entity.StrategicTask;
import com.sism.entity.SysOrg;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.ProgressApprovalStatus;
import com.sism.enums.TaskType;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.service.TaskService;
import com.sism.vo.TaskVO;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Task Approval Status Bug Condition Exploration
 * 
 * **Feature: task-approval-workflow-display, Property 1: Fault Condition**
 * 
 * This test is EXPECTED TO FAIL on unfixed code - failure confirms the bug exists.
 * 
 * For any task query request (GET /api/tasks or GET /api/tasks/{id}), 
 * the TaskService SHALL compute and return an `approvalStatus` field in the 
 * TaskVO response, derived from the task's associated indicators using the 
 * status aggregation algorithm.
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class TaskApprovalStatusPropertyTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private SysOrgRepository sysOrgRepository;

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> progressApprovalStatuses() {
        return Arbitraries.of("NONE", "DRAFT", "PENDING", "APPROVED", "REJECTED");
    }

    @Provide
    Arbitrary<List<String>> indicatorStatusCombinations() {
        return Arbitraries.of(
            // Single status scenarios
            List.of("DRAFT"),
            List.of("PENDING"),
            List.of("APPROVED"),
            List.of("REJECTED"),
            List.of("NONE"),
            // Multiple indicators with same status
            List.of("DRAFT", "DRAFT"),
            List.of("PENDING", "PENDING"),
            List.of("APPROVED", "APPROVED"),
            List.of("REJECTED", "REJECTED"),
            // Mixed status scenarios
            List.of("APPROVED", "PENDING"),
            List.of("APPROVED", "DRAFT"),
            List.of("PENDING", "DRAFT"),
            List.of("REJECTED", "APPROVED"),
            List.of("REJECTED", "PENDING"),
            List.of("REJECTED", "DRAFT"),
            // Three indicators with mixed statuses
            List.of("APPROVED", "APPROVED", "PENDING"),
            List.of("APPROVED", "PENDING", "DRAFT"),
            List.of("REJECTED", "APPROVED", "PENDING")
        );
    }

    // ==================== Property Tests ====================

    /**
     * Property 1.1: Task list response includes approvalStatus field
     * 
     * **Feature: task-approval-workflow-display, Property 1: Fault Condition**
     * 
     * WHEN a user queries the task list via getAllTasks()
     * THEN the system SHALL return each task with a computed `approvalStatus` field
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code (approvalStatus is NULL)
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 20)
    @Transactional
    void taskListResponse_shouldIncludeApprovalStatusField(
            @ForAll("indicatorStatusCombinations") List<String> indicatorStatuses) {
        
        // Setup: Create test data
        SysOrg org = createTestOrg();
        StrategicTask task = createTestTask(org);
        List<Indicator> indicators = createTestIndicators(task, org, indicatorStatuses);
        
        // Execute: Get all tasks
        List<TaskVO> tasks = taskService.getAllTasks();
        
        // Find our test task
        TaskVO taskVO = tasks.stream()
                .filter(t -> t.getTaskId().equals(task.getTaskId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Test task not found in response"));
        
        // Verify: approvalStatus field should NOT be NULL
        assertThat(taskVO.getApprovalStatus())
                .as("TaskVO should include approvalStatus field (currently NULL - bug exists)")
                .isNotNull();
        
        // Verify: approvalStatus should be a valid status value
        assertThat(taskVO.getApprovalStatus())
                .as("approvalStatus should be one of: DRAFT, PENDING, APPROVED, REJECTED, MIXED")
                .isIn("DRAFT", "PENDING", "APPROVED", "REJECTED", "MIXED");
        
        // Cleanup
        cleanupTestData(task, indicators);
    }

    /**
     * Property 1.2: Task detail response includes approvalStatus field
     * 
     * **Feature: task-approval-workflow-display, Property 1: Fault Condition**
     * 
     * WHEN a user views task details via getTaskById()
     * THEN the system SHALL return the task with `approvalStatus` field
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code (approvalStatus is NULL)
     * 
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 20)
    @Transactional
    void taskDetailResponse_shouldIncludeApprovalStatusField(
            @ForAll("indicatorStatusCombinations") List<String> indicatorStatuses) {
        
        // Setup: Create test data
        SysOrg org = createTestOrg();
        StrategicTask task = createTestTask(org);
        List<Indicator> indicators = createTestIndicators(task, org, indicatorStatuses);
        
        // Execute: Get task by ID
        TaskVO taskVO = taskService.getTaskById(task.getTaskId());
        
        // Verify: approvalStatus field should NOT be NULL
        assertThat(taskVO.getApprovalStatus())
                .as("TaskVO should include approvalStatus field (currently NULL - bug exists)")
                .isNotNull();
        
        // Verify: approvalStatus should be a valid status value
        assertThat(taskVO.getApprovalStatus())
                .as("approvalStatus should be one of: DRAFT, PENDING, APPROVED, REJECTED, MIXED")
                .isIn("DRAFT", "PENDING", "APPROVED", "REJECTED", "MIXED");
        
        // Cleanup
        cleanupTestData(task, indicators);
    }

    /**
     * Property 1.3: Task with no indicators should have DRAFT status
     * 
     * **Feature: task-approval-workflow-display, Property 1: Fault Condition**
     * 
     * WHEN a task has no associated indicators
     * THEN the system SHALL return approval status as DRAFT
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code (approvalStatus is NULL)
     * 
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 10)
    @Transactional
    void taskWithNoIndicators_shouldHaveDraftStatus() {
        
        // Setup: Create task without indicators
        SysOrg org = createTestOrg();
        StrategicTask task = createTestTask(org);
        
        // Execute: Get task by ID
        TaskVO taskVO = taskService.getTaskById(task.getTaskId());
        
        // Verify: approvalStatus should be DRAFT
        assertThat(taskVO.getApprovalStatus())
                .as("Task with no indicators should have DRAFT status (currently NULL - bug exists)")
                .isEqualTo("DRAFT");
        
        // Cleanup
        taskRepository.delete(task);
        sysOrgRepository.delete(org);
    }

    /**
     * Property 1.4: Task with all APPROVED indicators should have APPROVED status
     * 
     * **Feature: task-approval-workflow-display, Property 1: Fault Condition**
     * 
     * WHEN a task has multiple indicators with identical APPROVED statuses
     * THEN the system SHALL set the task's approval status to APPROVED
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code (approvalStatus is NULL)
     * 
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 10)
    @Transactional
    void taskWithAllApprovedIndicators_shouldHaveApprovedStatus() {
        
        // Setup: Create task with all APPROVED indicators
        SysOrg org = createTestOrg();
        StrategicTask task = createTestTask(org);
        List<Indicator> indicators = createTestIndicators(task, org, List.of("APPROVED", "APPROVED"));
        
        // Execute: Get task by ID
        TaskVO taskVO = taskService.getTaskById(task.getTaskId());
        
        // Verify: approvalStatus should be APPROVED
        assertThat(taskVO.getApprovalStatus())
                .as("Task with all APPROVED indicators should have APPROVED status (currently NULL - bug exists)")
                .isEqualTo("APPROVED");
        
        // Cleanup
        cleanupTestData(task, indicators);
    }

    /**
     * Property 1.5: Task with any REJECTED indicator should have REJECTED status
     * 
     * **Feature: task-approval-workflow-display, Property 1: Fault Condition**
     * 
     * WHEN a task has at least one REJECTED indicator
     * THEN the system SHALL set the task's approval status to REJECTED (highest priority)
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code (approvalStatus is NULL)
     * 
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 10)
    @Transactional
    void taskWithAnyRejectedIndicator_shouldHaveRejectedStatus() {
        
        // Setup: Create task with mixed statuses including REJECTED
        SysOrg org = createTestOrg();
        StrategicTask task = createTestTask(org);
        List<Indicator> indicators = createTestIndicators(task, org, List.of("REJECTED", "APPROVED", "PENDING"));
        
        // Execute: Get task by ID
        TaskVO taskVO = taskService.getTaskById(task.getTaskId());
        
        // Verify: approvalStatus should be REJECTED (highest priority)
        assertThat(taskVO.getApprovalStatus())
                .as("Task with any REJECTED indicator should have REJECTED status (currently NULL - bug exists)")
                .isEqualTo("REJECTED");
        
        // Cleanup
        cleanupTestData(task, indicators);
    }

    /**
     * Property 1.6: Task with any PENDING indicator should have PENDING status
     * 
     * **Feature: task-approval-workflow-display, Property 1: Fault Condition**
     * 
     * WHEN a task has at least one PENDING indicator (and no REJECTED)
     * THEN the system SHALL set the task's approval status to PENDING
     * 
     * **EXPECTED OUTCOME**: Test FAILS on unfixed code (approvalStatus is NULL)
     * 
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 10)
    @Transactional
    void taskWithAnyPendingIndicator_shouldHavePendingStatus() {
        
        // Setup: Create task with PENDING and APPROVED indicators
        SysOrg org = createTestOrg();
        StrategicTask task = createTestTask(org);
        List<Indicator> indicators = createTestIndicators(task, org, List.of("PENDING", "APPROVED"));
        
        // Execute: Get task by ID
        TaskVO taskVO = taskService.getTaskById(task.getTaskId());
        
        // Verify: approvalStatus should be PENDING
        assertThat(taskVO.getApprovalStatus())
                .as("Task with any PENDING indicator should have PENDING status (currently NULL - bug exists)")
                .isEqualTo("PENDING");
        
        // Cleanup
        cleanupTestData(task, indicators);
    }

    // ==================== Helper Methods ====================

    private SysOrg createTestOrg() {
        SysOrg org = new SysOrg();
        org.setName("Test Organization " + System.currentTimeMillis());
        org.setType(com.sism.enums.OrgType.FUNCTIONAL_DEPT);
        org.setIsActive(true);
        org.setSortOrder(0);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        return sysOrgRepository.save(org);
    }

    private StrategicTask createTestTask(SysOrg org) {
        StrategicTask task = StrategicTask.builder()
                .planId(1L)
                .cycleId(1L)
                .taskName("Test Task " + System.currentTimeMillis())
                .taskDesc("Test task for approval status testing")
                .taskType(TaskType.BASIC)
                .org(org)
                .createdByOrg(org)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        return taskRepository.save(task);
    }

    private List<Indicator> createTestIndicators(StrategicTask task, SysOrg org, List<String> statuses) {
        List<Indicator> indicators = new ArrayList<>();
        
        for (int i = 0; i < statuses.size(); i++) {
            Indicator indicator = Indicator.builder()
                    .taskId(task.getTaskId())
                    .level(IndicatorLevel.PRIMARY)
                    .ownerOrg(org)
                    .targetOrg(org)
                    .indicatorDesc("Test Indicator " + i)
                    .weightPercent(BigDecimal.valueOf(100.0 / statuses.size()))
                    .sortOrder(i)
                    .type("quantitative")
                    .status(IndicatorStatus.ACTIVE)
                    .progressApprovalStatus(ProgressApprovalStatus.valueOf(statuses.get(i)))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();
            indicators.add(indicatorRepository.save(indicator));
        }
        
        return indicators;
    }

    private void cleanupTestData(StrategicTask task, List<Indicator> indicators) {
        // Delete indicators
        for (Indicator indicator : indicators) {
            indicatorRepository.delete(indicator);
        }
        
        // Delete task
        SysOrg org = task.getOrg();
        taskRepository.delete(task);
        sysOrgRepository.delete(org);
    }
}
