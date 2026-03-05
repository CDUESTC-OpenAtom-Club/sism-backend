package com.sism.service;

import com.sism.dto.TaskCreateRequest;
import com.sism.dto.TaskUpdateRequest;
import com.sism.entity.AssessmentCycle;
import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.entity.StrategicTask;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.AssessmentCycleRepository;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for strategic task management
 * Provides CRUD operations and query methods for strategic tasks
 * 
 * Requirements: 2.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final AssessmentCycleRepository cycleRepository;
    private final SysOrgRepository sysOrgRepository;
    private final IndicatorRepository indicatorRepository;

    /**
     * Get task by ID
     * 
     * @param taskId task ID
     * @return task VO
     * @throws ResourceNotFoundException if task not found
     */
    public TaskVO getTaskById(Long taskId) {
        StrategicTask task = findTaskById(taskId);
        return toTaskVO(task);
    }

    /**
     * Get all tasks
     * 
     * @return list of all tasks
     */
    public List<TaskVO> getAllTasks() {
        List<StrategicTask> allTasks = taskRepository.findAll();
        log.info("Found {} total tasks in database", allTasks.size());
        
        List<StrategicTask> activeTasks = allTasks.stream()
                .filter(task -> task.getIsDeleted() == null || !task.getIsDeleted())
                .collect(Collectors.toList());
        log.info("Filtered to {} active tasks (is_deleted = false or NULL)", activeTasks.size());
        
        // Optimize: batch load indicators for all tasks to avoid N+1 query problem
        return toTaskVOsWithBatchLoading(activeTasks);
    }

    /**
     * Get tasks by plan ID
     * 
     * @param planId plan ID
     * @return list of tasks for the plan
     */
    public List<TaskVO> getTasksByCycleId(Long cycleId) {
        List<StrategicTask> tasks = taskRepository.findByCycleIdOrderBySortOrderAsc(cycleId);
        return toTaskVOsWithBatchLoading(tasks);
    }

    /**
     * Get tasks by organization ID
     * 
     * @param orgId organization ID
     * @return list of tasks for the organization
     */
    public List<TaskVO> getTasksByOrgId(Long orgId) {
        List<StrategicTask> tasks = taskRepository.findByOrg_Id(orgId);
        return toTaskVOsWithBatchLoading(tasks);
    }

    /**
     * Create a new strategic task
     * Requirements: 2.1 - Persist task to database immediately
     * 
     * @param request task creation request
     * @return created task VO
     */
    @Transactional
    public TaskVO createTask(TaskCreateRequest request) {
        log.info("Creating strategic task: {}", request.getTaskName());

        StrategicTask task = StrategicTask.builder()
                .planId(request.getPlanId())
                .taskName(request.getTaskName())
                .taskDesc(request.getTaskDesc())
                .taskType(request.getTaskType())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .remark(request.getRemark())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        StrategicTask savedTask = taskRepository.save(task);
        log.info("Successfully created strategic task with ID: {}", savedTask.getTaskId());
        return toTaskVO(savedTask);
    }

    /**
     * Update an existing strategic task
     * 
     * @param taskId task ID
     * @param request task update request
     * @return updated task VO
     */
    @Transactional
    public TaskVO updateTask(Long taskId, TaskUpdateRequest request) {
        StrategicTask task = findTaskById(taskId);

        if (request.getTaskName() != null) {
            task.setTaskName(request.getTaskName());
        }
        if (request.getTaskDesc() != null) {
            task.setTaskDesc(request.getTaskDesc());
        }
        if (request.getTaskType() != null) {
            task.setTaskType(request.getTaskType());
        }
        if (request.getOrgId() != null) {
            SysOrg org = sysOrgRepository.findById(request.getOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrgId()));
            task.setOrg(org);
        }
        if (request.getSortOrder() != null) {
            task.setSortOrder(request.getSortOrder());
        }
        if (request.getRemark() != null) {
            task.setRemark(request.getRemark());
        }
        
        task.setUpdatedAt(LocalDateTime.now());
        StrategicTask updatedTask = taskRepository.save(task);
        return toTaskVO(updatedTask);
    }

    /**
     * Delete a strategic task (soft delete)
     * 
     * @param taskId task ID
     */
    @Transactional
    public void deleteTask(Long taskId) {
        StrategicTask task = findTaskById(taskId);
        task.setIsDeleted(true);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    /**
     * Search tasks by keyword
     * 
     * @param keyword search keyword
     * @return list of matching tasks
     */
    public List<TaskVO> searchTasks(String keyword) {
        List<StrategicTask> tasks = taskRepository.searchByKeyword(keyword).stream()
                .filter(task -> !task.getIsDeleted())
                .collect(Collectors.toList());
        return toTaskVOsWithBatchLoading(tasks);
    }

    /**
     * Find task entity by ID
     * 
     * @param taskId task ID
     * @return task entity
     * @throws ResourceNotFoundException if task not found
     */
    private StrategicTask findTaskById(Long taskId) {
        StrategicTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Strategic Task", taskId));
        if (task.getIsDeleted()) {
            throw new ResourceNotFoundException("Strategic Task", taskId);
        }
        return task;
    }

    /**
     * Convert StrategicTask entity to TaskVO
     */
    private TaskVO toTaskVO(StrategicTask task) {
        TaskVO vo = new TaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setPlanId(task.getPlanId());
        vo.setTaskName(task.getTaskName());
        vo.setTaskDesc(task.getTaskDesc());
        vo.setTaskType(task.getTaskType());
        vo.setSortOrder(task.getSortOrder());
        vo.setRemark(task.getRemark());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        
        // Compute and set approval status from associated indicators
        List<Indicator> indicators = indicatorRepository.findByTaskId(task.getTaskId());
        String approvalStatus = computeApprovalStatus(indicators);
        vo.setApprovalStatus(approvalStatus);
        
        return vo;
    }

    /**
     * Convert list of StrategicTask entities to TaskVOs with batch loading optimization
     * Reduces N+1 query problem by loading all indicators in a single query
     * 
     * @param tasks list of tasks to convert
     * @return list of TaskVOs with computed approval statuses
     */
    private List<TaskVO> toTaskVOsWithBatchLoading(List<StrategicTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        
        // Extract all task IDs
        List<Long> taskIds = tasks.stream()
                .map(StrategicTask::getTaskId)
                .collect(Collectors.toList());
        
        // Batch query all indicators for these tasks
        List<Indicator> allIndicators = indicatorRepository.findByTaskIdIn(taskIds);
        
        // Group indicators by taskId for efficient lookup
        Map<Long, List<Indicator>> indicatorsByTaskId = allIndicators.stream()
                .collect(Collectors.groupingBy(Indicator::getTaskId));
        
        // Convert tasks to VOs using the pre-loaded indicators
        return tasks.stream()
                .map(task -> {
                    TaskVO vo = new TaskVO();
                    vo.setTaskId(task.getTaskId());
                    vo.setPlanId(task.getPlanId());
                    vo.setTaskName(task.getTaskName());
                    vo.setTaskDesc(task.getTaskDesc());
                    vo.setTaskType(task.getTaskType());
                    vo.setSortOrder(task.getSortOrder());
                    vo.setRemark(task.getRemark());
                    vo.setCreatedAt(task.getCreatedAt());
                    vo.setUpdatedAt(task.getUpdatedAt());
                    
                    // Compute approval status using pre-loaded indicators
                    List<Indicator> taskIndicators = indicatorsByTaskId.getOrDefault(task.getTaskId(), List.of());
                    String approvalStatus = computeApprovalStatus(taskIndicators);
                    vo.setApprovalStatus(approvalStatus);
                    
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Compute approval status from associated indicators
     * Implements status aggregation algorithm per design specification
     * 
     * @param indicators list of indicators associated with a task
     * @return computed approval status (DRAFT, PENDING, APPROVED, REJECTED, MIXED)
     */
    private String computeApprovalStatus(List<Indicator> indicators) {
        // Edge case: no indicators
        if (indicators == null || indicators.isEmpty()) {
            return "DRAFT";
        }
        
        // Extract all progressApprovalStatus values
        Set<String> uniqueStatuses = indicators.stream()
                .map(Indicator::getProgressApprovalStatus)
                .filter(status -> status != null && !status.isEmpty())
                .collect(Collectors.toSet());
        
        // If no valid statuses, default to DRAFT
        if (uniqueStatuses.isEmpty()) {
            return "DRAFT";
        }
        
        // Priority 1: If ANY indicator is REJECTED, task is REJECTED
        if (uniqueStatuses.contains("REJECTED")) {
            return "REJECTED";
        }
        
        // Priority 2: If ANY indicator is PENDING, task is PENDING
        if (uniqueStatuses.contains("PENDING")) {
            return "PENDING";
        }
        
        // Priority 3: If ALL indicators are APPROVED, task is APPROVED
        if (uniqueStatuses.size() == 1 && uniqueStatuses.contains("APPROVED")) {
            return "APPROVED";
        }
        
        // Priority 4: If mix of APPROVED and DRAFT/NONE, task is PENDING
        if (uniqueStatuses.contains("APPROVED") && 
            (uniqueStatuses.contains("DRAFT") || uniqueStatuses.contains("NONE"))) {
            return "PENDING";
        }
        
        // Default: All indicators are DRAFT or NONE
        return "DRAFT";
    }
}
