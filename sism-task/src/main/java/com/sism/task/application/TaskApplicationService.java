package com.sism.task.application;

import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.task.application.dto.CreateTaskRequest;
import com.sism.task.application.dto.TaskResponse;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
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
    public TaskResponse updateSortOrder(Long id, Integer sortOrder) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.updateSortOrder(sortOrder);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return TaskResponse.fromEntity(task);
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

    private void publishAndSaveEvents(com.sism.shared.domain.model.base.AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();

        for (DomainEvent event : events) {
            eventStore.save(event);
        }

        eventPublisher.publishAll(events);

        aggregate.clearEvents();
    }
}
