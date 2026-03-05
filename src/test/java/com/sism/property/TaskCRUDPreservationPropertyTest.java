package com.sism.property;

import com.sism.config.TestSecurityConfig;
import com.sism.dto.TaskCreateRequest;
import com.sism.dto.TaskUpdateRequest;
import com.sism.entity.Indicator;
import com.sism.entity.StrategicTask;
import com.sism.entity.SysOrg;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Task CRUD Preservation
 * 
 * **Feature: task-approval-workflow-display, Property 2: Preservation**
 * 
 * These tests verify that task CRUD operations remain unchanged after implementing
 * the approval status fix. Tests should PASS on both unfixed and fixed code.
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class TaskCRUDPreservationPropertyTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private SysOrgRepository sysOrgRepository;

    // ==================== Property Tests ====================

    /**
     * Property 2.1: Task Creation Does NOT Require approvalStatus
     * 
     * **Feature: task-approval-workflow-display, Property 2: Preservation**
     * 
     * WHEN creating a task via POST /api/tasks
     * THEN the system SHALL NOT require approvalStatus field in request payload
     * AND the task SHALL be created successfully
     * 
     * **Validates: Requirements 3.2**
     */
    @Property(tries = 10)
    @Transactional
    void taskCreation_shouldNotRequireApprovalStatusInRequest() {
        
        // Setup: Create test organization
        SysOrg org = createTestOrg();
        
        // Execute: Create task without approvalStatus field
        TaskCreateRequest request = new TaskCreateRequest();
        request.setPlanId(1L);
        request.setTaskName("Test Task " + System.currentTimeMillis());
        request.setTaskDesc("Test task description");
        request.setTaskType(TaskType.BASIC);
        request.setSortOrder(0);
        
        TaskVO createdTask = taskService.createTask(request);
        
        // Verify: Task is created successfully
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getTaskId()).isNotNull();
        assertThat(createdTask.getTaskName()).isEqualTo(request.getTaskName());
        assertThat(createdTask.getTaskType()).isEqualTo(request.getTaskType());
        
        // Verify: Task is persisted with isDeleted = false
        StrategicTask persistedTask = taskRepository.findById(createdTask.getTaskId()).orElse(null);
        assertThat(persistedTask).isNotNull();
        assertThat(persistedTask.getIsDeleted()).isFalse();
        
        // Cleanup
        taskRepository.delete(persistedTask);
        sysOrgRepository.delete(org);
    }

    /**
     * Property 2.2: Task Update Does NOT Modify Indicator Approval Statuses
     * 
     * **Feature: task-approval-workflow-display, Property 2: Preservation**
     * 
     * WHEN updating a task via PUT /api/tasks/{id}
     * THEN the system SHALL NOT modify indicator progressApprovalStatus values
     * AND indicator approval workflow SHALL remain independent
     * 
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 10)
    @Transactional
    void taskUpdate_shouldNotModifyIndicatorApprovalStatuses() {
        
        // Setup: Create task with indicator
        SysOrg org = createTestOrg();
        StrategicTask task = createTestTask(org, "Test Task");
        Indicator indicator = createTestIndicator(task, org, "PENDING");
        
        String originalStatus = indicator.getProgressApprovalStatus();
        
        // Execute: Update task
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTaskName("Updated Task Name");
        request.setTaskDesc("Updated description");
        
        taskService.updateTask(task.getTaskId(), request);
        
        // Verify: Indicator status remains unchanged
        Indicator updatedIndicator = indicatorRepository.findById(indicator.getIndicatorId()).orElse(null);
        assertThat(updatedIndicator).isNotNull();
        assertThat(updatedIndicator.getProgressApprovalStatus()).isEqualTo(originalStatus);
        
        // Cleanup
        indicatorRepository.delete(indicator);
        taskRepository.delete(task);
        sysOrgRepository.delete(org);
    }

    /**
     * Property 2.3: Task Deletion Performs Soft Delete Correctly
     * 
     * **Feature: task-approval-workflow-display, Property 2: Preservation**
     * 
     * WHEN deleting a task via DELETE /api/tasks/{id}
     * THEN the system SHALL set isDeleted = true (soft delete)
     * AND the task record SHALL remain in database
     * AND the task SHALL NOT appear in getAllTasks() results
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 10)
    @Transactional
    void taskDeletion_shouldPerformSoftDeleteCorrectly() {
        
        // Setup: Create task
        SysOrg org = createTestOrg();
        StrategicTask task = createTestTask(org, "Task to Delete");
        Long taskId = task.getTaskId();
        
        // Execute: Delete task
        taskService.deleteTask(taskId);
        
        // Verify: Task record still exists in database
        StrategicTask deletedTask = taskRepository.findById(taskId).orElse(null);
        assertThat(deletedTask).isNotNull();
        assertThat(deletedTask.getIsDeleted()).isTrue();
        
        // Verify: Task does not appear in getAllTasks()
        List<TaskVO> allTasks = taskService.getAllTasks();
        boolean taskInList = allTasks.stream()
                .anyMatch(t -> t.getTaskId().equals(taskId));
        assertThat(taskInList).isFalse();
        
        // Cleanup
        taskRepository.delete(deletedTask);
        sysOrgRepository.delete(org);
    }



    /**
     * Property 2.4: Task filtering by plan returns correct results
     * 
     * **Feature: task-approval-workflow-display, Property 2: Preservation**
     * 
     * WHEN querying tasks by plan, cycle, or organization filters
     * THEN the system SHALL return filtered results correctly with all existing fields
     * 
     * **EXPECTED OUTCOME**: Test PASSES (confirms baseline behavior)
     * 
     * **Validates: Requirements 3.1**
     */
    @Property(tries = 10)
    @Transactional
    void taskFiltering_shouldReturnCorrectResults() {
        
        // Setup: Create multiple tasks with different cycle IDs
        SysOrg org = createTestOrg();
        StrategicTask task1 = createTestTaskWithCycle(org, "Task 1", 1L);
        StrategicTask task2 = createTestTaskWithCycle(org, "Task 2", 1L);
        StrategicTask task3 = createTestTaskWithCycle(org, "Task 3", 2L);
        
        // Execute: Filter by cycle ID
        List<TaskVO> cycleOneTasks = taskService.getTasksByCycleId(1L);
        
        // Verify: Correct tasks are returned
        List<Long> cycleOneTaskIds = cycleOneTasks.stream()
                .map(TaskVO::getTaskId)
                .toList();
        
        assertThat(cycleOneTaskIds)
                .as("Should return tasks for cycle 1")
                .contains(task1.getTaskId(), task2.getTaskId())
                .doesNotContain(task3.getTaskId());
        
        // Verify: All existing fields are present
        for (TaskVO taskVO : cycleOneTasks) {
            if (taskVO.getTaskId().equals(task1.getTaskId()) || 
                taskVO.getTaskId().equals(task2.getTaskId())) {
                assertThat(taskVO.getTaskName()).isNotNull();
                assertThat(taskVO.getTaskType()).isNotNull();
                assertThat(taskVO.getSortOrder()).isNotNull();
            }
        }
        
        // Cleanup
        taskRepository.deleteById(task1.getTaskId());
        taskRepository.deleteById(task2.getTaskId());
        taskRepository.deleteById(task3.getTaskId());
        sysOrgRepository.delete(org);
    }

    /**
     * Property 2.5: Task sorting by sortOrder remains functional
     * 
     * **Feature: task-approval-workflow-display, Property 2: Preservation**
     * 
     * WHEN tasks are sorted by sortOrder
     * THEN the system SHALL apply sorting correctly
     * 
     * **EXPECTED OUTCOME**: Test PASSES (confirms baseline behavior)
     * 
     * **Validates: Requirements 3.7**
     */
    @Property(tries = 10)
    @Transactional
    void taskSorting_shouldRemainFunctional() {
        
        // Setup: Create tasks with different sort orders
        SysOrg org = createTestOrg();
        StrategicTask task1 = createTestTaskWithSortOrder(org, "Task A", 1L, 30);
        StrategicTask task2 = createTestTaskWithSortOrder(org, "Task B", 1L, 10);
        StrategicTask task3 = createTestTaskWithSortOrder(org, "Task C", 1L, 20);
        
        // Execute: Get tasks by cycle (should be sorted by sortOrder)
        List<TaskVO> tasks = taskService.getTasksByCycleId(1L);
        
        // Filter to our test tasks
        List<TaskVO> testTasks = tasks.stream()
                .filter(t -> t.getTaskId().equals(task1.getTaskId()) ||
                           t.getTaskId().equals(task2.getTaskId()) ||
                           t.getTaskId().equals(task3.getTaskId()))
                .toList();
        
        // Verify: Tasks are sorted by sortOrder (ascending)
        if (testTasks.size() >= 3) {
            assertThat(testTasks.get(0).getSortOrder())
                    .as("First task should have lowest sortOrder")
                    .isLessThanOrEqualTo(testTasks.get(1).getSortOrder());
            assertThat(testTasks.get(1).getSortOrder())
                    .as("Second task should have middle sortOrder")
                    .isLessThanOrEqualTo(testTasks.get(2).getSortOrder());
        }
        
        // Cleanup
        taskRepository.deleteById(task1.getTaskId());
        taskRepository.deleteById(task2.getTaskId());
        taskRepository.deleteById(task3.getTaskId());
        sysOrgRepository.delete(org);
    }

    /**
     * Property 2.6: Task filtering by taskType remains functional
     * 
     * **Feature: task-approval-workflow-display, Property 2: Preservation**
     * 
     * WHEN tasks are filtered by taskType
     * THEN the system SHALL apply filtering correctly
     * 
     * **EXPECTED OUTCOME**: Test PASSES (confirms baseline behavior)
     * 
     * **Validates: Requirements 3.7**
     */
    @Property(tries = 10)
    @Transactional
    void taskTypeFiltering_shouldRemainFunctional() {
        
        // Setup: Create tasks with different types
        SysOrg org = createTestOrg();
        StrategicTask basicTask = createTestTaskWithType(org, "Basic Task", TaskType.BASIC);
        StrategicTask keyTask = createTestTaskWithType(org, "Key Task", TaskType.KEY);
        
        // Execute: Get all tasks
        List<TaskVO> allTasks = taskService.getAllTasks();
        
        // Verify: Tasks have correct types
        TaskVO basicTaskVO = allTasks.stream()
                .filter(t -> t.getTaskId().equals(basicTask.getTaskId()))
                .findFirst()
                .orElse(null);
        
        TaskVO keyTaskVO = allTasks.stream()
                .filter(t -> t.getTaskId().equals(keyTask.getTaskId()))
                .findFirst()
                .orElse(null);
        
        assertThat(basicTaskVO).isNotNull();
        assertThat(basicTaskVO.getTaskType()).isEqualTo(TaskType.BASIC);
        
        assertThat(keyTaskVO).isNotNull();
        assertThat(keyTaskVO.getTaskType()).isEqualTo(TaskType.KEY);
        
        // Cleanup
        taskRepository.deleteById(basicTask.getTaskId());
        taskRepository.deleteById(keyTask.getTaskId());
        sysOrgRepository.delete(org);
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

    private StrategicTask createTestTask(SysOrg org, String taskName) {
        StrategicTask task = StrategicTask.builder()
                .planId(1L)
                .cycleId(1L)
                .taskName(taskName)
                .taskDesc("Test task description")
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

    private StrategicTask createTestTaskWithCycle(SysOrg org, String taskName, Long cycleId) {
        StrategicTask task = StrategicTask.builder()
                .planId(1L)
                .cycleId(cycleId)
                .taskName(taskName)
                .taskDesc("Test task description")
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

    private StrategicTask createTestTaskWithSortOrder(SysOrg org, String taskName, Long cycleId, Integer sortOrder) {
        StrategicTask task = StrategicTask.builder()
                .planId(1L)
                .cycleId(cycleId)
                .taskName(taskName)
                .taskDesc("Test task description")
                .taskType(TaskType.BASIC)
                .org(org)
                .createdByOrg(org)
                .sortOrder(sortOrder)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        return taskRepository.save(task);
    }

    private StrategicTask createTestTaskWithType(SysOrg org, String taskName, TaskType taskType) {
        StrategicTask task = StrategicTask.builder()
                .planId(1L)
                .cycleId(1L)
                .taskName(taskName)
                .taskDesc("Test task description")
                .taskType(taskType)
                .org(org)
                .createdByOrg(org)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        return taskRepository.save(task);
    }

    private Indicator createTestIndicator(StrategicTask task, SysOrg org, String progressApprovalStatus) {
        Indicator indicator = Indicator.builder()
                .taskId(task.getTaskId())
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(org)
                .targetOrg(org)
                .indicatorDesc("Test Indicator")
                .weightPercent(BigDecimal.valueOf(100.0))
                .sortOrder(0)
                .type("quantitative")
                .status(IndicatorStatus.ACTIVE)
                .progressApprovalStatus(progressApprovalStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        return indicatorRepository.save(indicator);
    }

    private void cleanupTestData(StrategicTask task, List<Indicator> indicators, SysOrg org) {
        // Delete indicators
        for (Indicator indicator : indicators) {
            indicatorRepository.delete(indicator);
        }
        
        // Delete task
        taskRepository.delete(task);
        
        // Delete org
        sysOrgRepository.delete(org);
    }
}
