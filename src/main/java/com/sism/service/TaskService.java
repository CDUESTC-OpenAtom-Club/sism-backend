package com.sism.service;

import com.sism.dto.TaskCreateRequest;
import com.sism.dto.TaskUpdateRequest;
import com.sism.entity.AssessmentCycle;
import com.sism.entity.SysOrg;
import com.sism.entity.StrategicTask;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.AssessmentCycleRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
        
        return activeTasks.stream()
                .map(this::toTaskVO)
                .collect(Collectors.toList());
    }

    /**
     * Get tasks by plan ID
     * 
     * @param planId plan ID
     * @return list of tasks for the plan
     */
    public List<TaskVO> getTasksByCycleId(Long cycleId) {
        return taskRepository.findByCycleIdOrderBySortOrderAsc(cycleId).stream()
                .map(this::toTaskVO)
                .collect(Collectors.toList());
    }

    /**
     * Get tasks by organization ID
     * 
     * @param orgId organization ID
     * @return list of tasks for the organization
     */
    public List<TaskVO> getTasksByOrgId(Long orgId) {
        return taskRepository.findByOrg_Id(orgId).stream()
                .map(this::toTaskVO)
                .collect(Collectors.toList());
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
        return taskRepository.searchByKeyword(keyword).stream()
                .filter(task -> !task.getIsDeleted())
                .map(this::toTaskVO)
                .collect(Collectors.toList());
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
        
        return vo;
    }
}
