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
        return taskRepository.findAll().stream()
                .map(this::toTaskVO)
                .collect(Collectors.toList());
    }

    /**
     * Get tasks by assessment cycle ID
     * Requirements: 2.1 - Load strategic task list for current assessment cycle
     * 
     * @param cycleId assessment cycle ID
     * @return list of tasks for the cycle
     */
    public List<TaskVO> getTasksByCycleId(Long cycleId) {
        return taskRepository.findByCycle_CycleIdOrderBySortOrderAsc(cycleId).stream()
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
        
        // Validate cycle exists
        AssessmentCycle cycle = cycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Cycle", request.getCycleId()));

        // Validate organization exists
        SysOrg org = sysOrgRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrgId()));

        // Validate created by organization exists
        SysOrg createdByOrg = sysOrgRepository.findById(request.getCreatedByOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Created By Organization", request.getCreatedByOrgId()));

        StrategicTask task = new StrategicTask();
        task.setCycle(cycle);
        task.setTaskName(request.getTaskName());
        task.setTaskDesc(request.getTaskDesc());
        task.setTaskType(request.getTaskType());
        task.setOrg(org);
        task.setCreatedByOrg(createdByOrg);
        task.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        task.setRemark(request.getRemark());

        StrategicTask savedTask = taskRepository.save(task);
        taskRepository.flush(); // Force immediate database write
        
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

        StrategicTask updatedTask = taskRepository.save(task);
        return toTaskVO(updatedTask);
    }

    /**
     * Delete a strategic task
     * 
     * @param taskId task ID
     */
    @Transactional
    public void deleteTask(Long taskId) {
        StrategicTask task = findTaskById(taskId);
        taskRepository.delete(task);
    }

    /**
     * Search tasks by keyword
     * 
     * @param keyword search keyword
     * @return list of matching tasks
     */
    public List<TaskVO> searchTasks(String keyword) {
        return taskRepository.searchByKeyword(keyword).stream()
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
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Strategic Task", taskId));
    }

    /**
     * Convert StrategicTask entity to TaskVO
     */
    private TaskVO toTaskVO(StrategicTask task) {
        TaskVO vo = new TaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setCycleId(task.getCycle().getCycleId());
        vo.setCycleName(task.getCycle().getCycleName());
        vo.setYear(task.getCycle().getYear());  // 从关联的 AssessmentCycle 获取年份
        vo.setTaskName(task.getTaskName());
        vo.setTaskDesc(task.getTaskDesc());
        vo.setTaskType(task.getTaskType());
        vo.setOrgId(task.getOrg().getId());
        vo.setOrgName(task.getOrg().getName());
        vo.setCreatedByOrgId(task.getCreatedByOrg().getId());
        vo.setCreatedByOrgName(task.getCreatedByOrg().getName());
        vo.setSortOrder(task.getSortOrder());
        vo.setRemark(task.getRemark());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        return vo;
    }
}
