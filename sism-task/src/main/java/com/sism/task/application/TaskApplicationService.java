package com.sism.task.application;

import com.sism.common.PageResult;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.task.application.dto.*;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskApplicationService {

    private final TaskRepository taskRepository;
    private final OrganizationRepository organizationRepository;
    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        // 查询组织信息
        SysOrg org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + request.getOrgId()));
        SysOrg createdByOrg = organizationRepository.findById(request.getCreatedByOrgId())
                .orElseThrow(() -> new IllegalArgumentException("创建组织不存在: " + request.getCreatedByOrgId()));

        StrategicTask task = StrategicTask.create(
                request.getTaskName(),
                request.getTaskType(),
                request.getPlanId(),
                request.getCycleId(),
                org,
                createdByOrg
        );

        if (request.getSortOrder() != null) {
            task.updateSortOrder(request.getSortOrder());
        }
        if (request.getTaskDesc() != null) {
            task.updateTaskDesc(request.getTaskDesc());
        }
        if (request.getRemark() != null) {
            task.setRemark(request.getRemark());
        }

        task.validate();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse activateTask(Long id) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.activate();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse completeTask(Long id) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.complete();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse cancelTask(Long id) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.cancel();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));

        // 更新基本属性
        task.updateTaskName(request.getTaskName());
        task.updateTaskDesc(request.getTaskDesc());
        task.setTaskType(request.getTaskType());
        task.setPlanId(request.getPlanId());
        task.setCycleId(request.getCycleId());
        task.setStatus(request.getStatus());

        if (request.getSortOrder() != null) {
            task.updateSortOrder(request.getSortOrder());
        }
        if (request.getRemark() != null) {
            task.setRemark(request.getRemark());
        }

        // 更新组织信息
        if (request.getOrgId() != null) {
            SysOrg org = organizationRepository.findById(request.getOrgId())
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + request.getOrgId()));
            task.setOrg(org);
        }

        if (request.getCreatedByOrgId() != null) {
            SysOrg createdByOrg = organizationRepository.findById(request.getCreatedByOrgId())
                    .orElseThrow(() -> new IllegalArgumentException("创建组织不存在: " + request.getCreatedByOrgId()));
            task.setCreatedByOrg(createdByOrg);
        }

        task.validate();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse updateTaskName(Long id, String taskName) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.updateTaskName(taskName);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse updateTaskDesc(Long id, String taskDesc) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.updateTaskDesc(taskDesc);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse updateTaskRemark(Long id, String remark) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.setRemark(remark);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse updateSortOrder(Long id, Integer sortOrder) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.updateSortOrder(sortOrder);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.setIsDeleted(true);
        taskRepository.save(task);
        publishAndSaveEvents(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        return TaskResponse.fromEntity(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<TaskResponse> searchTasks(TaskQueryRequest request) {
        Sort sort = Sort.unsorted();
        if (request.getSortBy() != null && !request.getSortBy().isEmpty()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if (request.getSortDirection() != null && "desc".equalsIgnoreCase(request.getSortDirection())) {
                direction = Sort.Direction.DESC;
            }
            sort = Sort.by(direction, request.getSortBy());
        }

        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 10,
                sort
        );

        Page<StrategicTask> page = taskRepository.findByCriteria(
                request.getPlanId(),
                request.getCycleId(),
                request.getOrgId(),
                request.getCreatedByOrgId(),
                request.getTaskType(),
                request.getStatus(),
                request.getTaskName(),
                pageable
        );

        return PageResult.of(
                page.getContent().stream().map(TaskResponse::fromEntity).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByOrgId(Long orgId) {
        List<StrategicTask> tasks = taskRepository.findByOrgId(orgId);
        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByPlanId(Long planId) {
        List<StrategicTask> tasks = taskRepository.findByPlanId(planId);
        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByCycleId(Long cycleId) {
        List<StrategicTask> tasks = taskRepository.findByCycleId(cycleId);
        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByType(TaskType taskType) {
        List<StrategicTask> tasks = taskRepository.findByTaskType(taskType);
        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByStatus(String status) {
        List<StrategicTask> tasks = taskRepository.findByStatus(status);
        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByPlanIdAndCycleId(Long planId, Long cycleId) {
        List<StrategicTask> tasks = taskRepository.findByPlanIdAndCycleId(planId, cycleId);
        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    private void publishAndSaveEvents(com.sism.shared.domain.model.base.AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();

        for (DomainEvent event : events) {
            eventStore.save(event);
        }

        eventPublisher.publishAll(events);

        aggregate.clearEvents();
    }
}
