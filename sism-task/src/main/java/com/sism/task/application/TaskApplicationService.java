package com.sism.task.application;

import com.sism.common.PageResult;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.sism.task.application.dto.*;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskCategory;
import com.sism.task.domain.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import com.sism.task.infrastructure.persistence.JpaTaskRepositoryInternal;
import com.sism.task.infrastructure.persistence.TaskFlatView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskApplicationService {

    private final TaskRepository taskRepository;
    private final JpaTaskRepositoryInternal jpaTaskRepository;
    private final OrganizationRepository organizationRepository;
    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        SysOrg org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + request.getOrgId()));
        SysOrg createdByOrg = organizationRepository.findById(request.getCreatedByOrgId())
                .orElseThrow(() -> new IllegalArgumentException("创建组织不存在: " + request.getCreatedByOrgId()));

        validatePlanBinding(request.getPlanId(), request.getCycleId(), org, createdByOrg);

        StrategicTask task = StrategicTask.create(
                request.getTaskCategory() != null ? request.getTaskCategory() : TaskCategory.STRATEGIC,
                request.getName(),
                request.getTaskType(),
                request.getPlanId(),
                request.getCycleId(),
                org,
                createdByOrg
        );

        if (request.getSortOrder() != null) {
            task.updateSortOrder(request.getSortOrder());
        }
        if (request.getDesc() != null) {
            task.updateDesc(request.getDesc());
        }
        if (request.getRemark() != null) {
            task.setRemark(request.getRemark());
        }

        task.validate();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse activateTask(Long id) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.activate();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse completeTask(Long id) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.complete();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse cancelTask(Long id) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.cancel();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));

        task.updateName(request.getName());
        task.updateDesc(request.getDesc());
        task.setTaskType(request.getTaskType());
        task.setTaskCategory(request.getTaskCategory() != null ? request.getTaskCategory() : TaskCategory.STRATEGIC);
        task.setPlanId(request.getPlanId());
        task.setCycleId(request.getCycleId());

        if (request.getSortOrder() != null) {
            task.updateSortOrder(request.getSortOrder());
        }
        if (request.getRemark() != null) {
            task.setRemark(request.getRemark());
        }

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

        validatePlanBinding(task.getPlanId(), task.getCycleId(), task.getOrg(), task.getCreatedByOrg());

        task.validate();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskName(Long id, String name) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.updateName(name);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskDesc(Long id, String desc) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.updateDesc(desc);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskRemark(Long id, String remark) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.setRemark(remark);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateSortOrder(Long id, Integer sortOrder) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.updateSortOrder(sortOrder);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
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
        return jpaTaskRepository.findFlatViewById(id)
                .map(TaskResponse::fromView)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return jpaTaskRepository.findFlatViewsByCriteria(null, null, null, null, "", "").stream()
                .map(TaskResponse::fromView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<TaskResponse> searchTasks(TaskQueryRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;

        List<TaskResponse> matches = jpaTaskRepository.findFlatViewsByCriteria(
                request.getPlanId(),
                request.getCycleId(),
                request.getOrgId(),
                request.getCreatedByOrgId(),
                request.getTaskType() != null ? request.getTaskType().name() : "",
                request.getName() != null ? request.getName() : ""
        ).stream()
                .map(TaskResponse::fromView)
                .filter(response -> request.getPlanStatus() == null || request.getPlanStatus().isBlank()
                        || request.getPlanStatus().equalsIgnoreCase(response.getPlanStatus()))
                .filter(response -> request.getTaskStatus() == null || request.getTaskStatus().isBlank()
                        || request.getTaskStatus().equalsIgnoreCase(response.getTaskStatus()))
                .toList();

        Comparator<TaskResponse> comparator = buildSearchComparator(request.getSortBy());
        if (comparator != null) {
            if ("desc".equalsIgnoreCase(request.getSortDirection())) {
                comparator = comparator.reversed();
            }
            matches = matches.stream().sorted(comparator).toList();
        }

        int fromIndex = Math.min(page * size, matches.size());
        int toIndex = Math.min(fromIndex + size, matches.size());
        List<TaskResponse> items = matches.subList(fromIndex, toIndex);

        return PageResult.of(
                items,
                matches.size(),
                page,
                size
        );
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByOrgId(Long orgId) {
        return jpaTaskRepository.findFlatViewsByCriteria(null, null, orgId, null, "", "").stream()
                .map(TaskResponse::fromView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByPlanId(Long planId) {
        return jpaTaskRepository.findFlatViewsByCriteria(planId, null, null, null, "", "").stream()
                .map(TaskResponse::fromView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByCycleId(Long cycleId) {
        return jpaTaskRepository.findFlatViewsByCycleId(cycleId).stream()
                .map(TaskResponse::fromView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByType(TaskType taskType) {
        return jpaTaskRepository.findFlatViewsByCriteria(null, null, null, null, taskType.name(), "").stream()
                .map(TaskResponse::fromView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByPlanIdAndCycleId(Long planId, Long cycleId) {
        return jpaTaskRepository.findFlatViewsByCriteria(planId, cycleId, null, null, "", "").stream()
                .map(TaskResponse::fromView)
                .toList();
    }

    private TaskResponse toCommandResponse(StrategicTask task) {
        return TaskResponse.fromEntity(task, loadPlanStatus(task.getId()));
    }

    private void validatePlanBinding(Long planId, Long cycleId, SysOrg org, SysOrg createdByOrg) {
        if (planId == null) {
            throw new IllegalArgumentException("计划ID不能为空");
        }
        if (cycleId == null) {
            throw new IllegalArgumentException("考核周期ID不能为空");
        }
        if (org == null || org.getId() == null) {
            throw new IllegalArgumentException("组织不存在");
        }
        if (createdByOrg == null || createdByOrg.getId() == null) {
            throw new IllegalArgumentException("创建组织不存在");
        }

        Optional<?> planRow = entityManager.createNativeQuery("""
                SELECT p.cycle_id, p.target_org_id, p.created_by_org_id, p.plan_level
                FROM public.plan p
                WHERE p.id = :planId
                  AND COALESCE(p.is_deleted, false) = false
                """)
                .setParameter("planId", planId)
                .getResultStream()
                .findFirst();
        if (planRow.isEmpty()) {
            throw new IllegalArgumentException("计划不存在: " + planId);
        }
        Object[] row = (Object[]) planRow.get();

        long planCycleId = ((Number) row[0]).longValue();
        long planTargetOrgId = ((Number) row[1]).longValue();
        long planCreatedByOrgId = ((Number) row[2]).longValue();
        String planLevel = String.valueOf(row[3]).trim().toUpperCase();

        if (planCycleId != cycleId.longValue()) {
            throw new IllegalArgumentException("任务周期与计划周期不一致");
        }
        if (planTargetOrgId != org.getId()) {
            throw new IllegalArgumentException("任务归属组织与计划目标组织不一致");
        }
        if (planCreatedByOrgId != createdByOrg.getId()) {
            throw new IllegalArgumentException("任务创建组织与计划创建组织不一致");
        }
        if ("FUNC_TO_COLLEGE".equals(planLevel) && org.getType() != OrgType.academic) {
            throw new IllegalArgumentException("FUNC_TO_COLLEGE 计划只能绑定二级学院任务");
        }
        if ("STRAT_TO_FUNC".equals(planLevel) && org.getType() != OrgType.functional) {
            throw new IllegalArgumentException("STRAT_TO_FUNC 计划只能绑定职能部门任务");
        }
    }

    private String loadPlanStatus(Long taskId) {
        return jpaTaskRepository.findFlatViewById(taskId)
                .map(TaskFlatView::getPlanStatus)
                .orElse(StrategicTask.STATUS_DRAFT);
    }

    private void publishAndSaveEvents(com.sism.shared.domain.model.base.AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();

        for (DomainEvent event : events) {
            eventStore.save(event);
        }

        eventPublisher.publishAll(events);

        aggregate.clearEvents();
    }

    private Comparator<TaskResponse> buildSearchComparator(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return null;
        }

        return switch (sortBy) {
            case "id" -> Comparator.comparing(TaskResponse::getId, Comparator.nullsLast(Long::compareTo));
            case "name" -> Comparator.comparing(TaskResponse::getName, Comparator.nullsLast(String::compareTo));
            case "taskType" -> Comparator.comparing(
                    response -> response.getTaskType() != null ? response.getTaskType().name() : null,
                    Comparator.nullsLast(String::compareTo)
            );
            case "planId" -> Comparator.comparing(TaskResponse::getPlanId, Comparator.nullsLast(Long::compareTo));
            case "cycleId" -> Comparator.comparing(TaskResponse::getCycleId, Comparator.nullsLast(Long::compareTo));
            case "orgId" -> Comparator.comparing(TaskResponse::getOrgId, Comparator.nullsLast(Long::compareTo));
            case "createdByOrgId" -> Comparator.comparing(TaskResponse::getCreatedByOrgId, Comparator.nullsLast(Long::compareTo));
            case "sortOrder" -> Comparator.comparing(TaskResponse::getSortOrder, Comparator.nullsLast(Integer::compareTo));
            case "planStatus" -> Comparator.comparing(TaskResponse::getPlanStatus, Comparator.nullsLast(String::compareTo));
            case "taskStatus" -> Comparator.comparing(TaskResponse::getTaskStatus, Comparator.nullsLast(String::compareTo));
            case "createdAt" -> Comparator.comparing(TaskResponse::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
            case "updatedAt" -> Comparator.comparing(TaskResponse::getUpdatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
            default -> null;
        };
    }
}
