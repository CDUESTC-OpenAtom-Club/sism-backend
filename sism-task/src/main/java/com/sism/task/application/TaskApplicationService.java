package com.sism.task.application;

import com.sism.common.PageResult;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.shared.domain.exception.AuthorizationException;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.task.application.dto.*;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskStatus;
import com.sism.task.domain.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import com.sism.task.infrastructure.persistence.JpaTaskRepositoryInternal;
import com.sism.task.infrastructure.persistence.PlanBindingRepository;
import com.sism.task.infrastructure.persistence.TaskFlatView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskApplicationService {

    private final TaskRepository taskRepository;
    private final JpaTaskRepositoryInternal jpaTaskRepository;
    private final OrganizationRepository organizationRepository;
    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;
    private final PlanBindingRepository planBindingRepository;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, CurrentUser currentUser, boolean isAdmin) {
        ensureCanOperateOnOrg(currentUser, isAdmin, request.getOrgId());
        ensureCanOperateOnOrg(currentUser, isAdmin, request.getCreatedByOrgId());
        SysOrg org = loadOrganization(request.getOrgId(), "组织不存在");
        SysOrg createdByOrg = loadOrganization(request.getCreatedByOrgId(), "创建组织不存在");

        validatePlanBinding(request.getPlanId(), request.getCycleId(), org, createdByOrg);

        StrategicTask task = StrategicTask.create(
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
    public TaskResponse activateTask(Long id, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = loadTaskWithAccess(id, currentUser, isAdmin);
        task.activate();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse completeTask(Long id, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = loadTaskWithAccess(id, currentUser, isAdmin);
        task.complete();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse cancelTask(Long id, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = loadTaskWithAccess(id, currentUser, isAdmin);
        task.cancel();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = loadTaskWithAccess(id, currentUser, isAdmin);
        ensureCanOperateOnOrg(currentUser, isAdmin, request.getOrgId());
        ensureCanOperateOnOrg(currentUser, isAdmin, request.getCreatedByOrgId());

        task.updateName(request.getName());
        task.updateDesc(request.getDesc());
        task.setTaskType(request.getTaskType());
        task.setPlanId(request.getPlanId());
        task.setCycleId(request.getCycleId());

        if (request.getSortOrder() != null) {
            task.updateSortOrder(request.getSortOrder());
        }
        if (request.getRemark() != null) {
            task.setRemark(request.getRemark());
        }

        if (request.getOrgId() != null) {
            SysOrg org = loadOrganization(request.getOrgId(), "组织不存在");
            task.setOrg(org);
        }

        if (request.getCreatedByOrgId() != null) {
            SysOrg createdByOrg = loadOrganization(request.getCreatedByOrgId(), "创建组织不存在");
            task.setCreatedByOrg(createdByOrg);
        }

        validatePlanBinding(task.getPlanId(), task.getCycleId(), task.getOrg(), task.getCreatedByOrg());

        task.validate();
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskName(Long id, String name, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = loadTaskWithAccess(id, currentUser, isAdmin);
        task.updateName(name);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskDesc(Long id, String desc, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = loadTaskWithAccess(id, currentUser, isAdmin);
        task.updateDesc(desc);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskRemark(Long id, String remark, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = loadTaskWithAccess(id, currentUser, isAdmin);
        task.setRemark(remark);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public TaskResponse updateSortOrder(Long id, Integer sortOrder, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = loadTaskWithAccess(id, currentUser, isAdmin);
        task.updateSortOrder(sortOrder);
        taskRepository.save(task);
        publishAndSaveEvents(task);
        return toCommandResponse(task);
    }

    @Transactional
    public void deleteTask(Long id, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = loadTaskWithAccess(id, currentUser, isAdmin);
        ensureTaskDeletable(task);
        task.setIsDeleted(true);
        taskRepository.save(task);
        publishAndSaveEvents(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return jpaTaskRepository.findFlatViewById(id)
                .map(TaskResponse::fromView)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return jpaTaskRepository.findFlatViewsByCriteria(null, null, null, null, "", "").stream()
                .map(TaskResponse::fromView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAccessibleTasksByOrgId(Long orgId) {
        if (orgId == null) {
            return List.of();
        }
        return jpaTaskRepository.findFlatViewsByAccessibleOrgId(orgId).stream()
                .map(TaskResponse::fromView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<TaskResponse> searchTasks(TaskQueryRequest request, Long accessibleOrgId) {
        Page<TaskFlatView> resultPage = jpaTaskRepository.findPagedFlatViewsByCriteria(
                request.getPlanId(),
                request.getCycleId(),
                request.getOrgId(),
                request.getCreatedByOrgId(),
                request.getTaskType() != null ? request.getTaskType().name() : null,
                request.getName(),
                request.getPlanStatus(),
                request.getTaskStatus(),
                accessibleOrgId,
                buildSearchPageable(request)
        );

        Page<TaskResponse> responsePage = resultPage.map(TaskResponse::fromView);
        return PageResult.of(responsePage);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByOrgId(Long orgId) {
        return jpaTaskRepository.findFlatViewsByCriteria(null, null, orgId, null, "", "").stream()
                .map(TaskResponse::fromView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksCreatedByOrgId(Long orgId) {
        return jpaTaskRepository.findFlatViewsByCriteria(null, null, null, orgId, "", "").stream()
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
        return TaskResponse.fromEntity(task);
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

        Optional<PlanBindingRepository.PlanBindingInfo> planRow = planBindingRepository.findByPlanId(planId);
        if (planRow.isEmpty()) {
            throw new IllegalArgumentException("计划不存在");
        }
        PlanBindingRepository.PlanBindingInfo binding = planRow.get();

        Long planCycleId = binding.cycleId();
        Long planTargetOrgId = binding.targetOrgId();
        Long planCreatedByOrgId = binding.createdByOrgId();
        String planLevel = binding.planLevel();

        if (!Objects.equals(planCycleId, cycleId)) {
            throw new IllegalArgumentException("任务周期与计划周期不一致");
        }
        if (!Objects.equals(planTargetOrgId, org.getId())) {
            throw new IllegalArgumentException("任务归属组织与计划目标组织不一致");
        }
        if (!Objects.equals(planCreatedByOrgId, createdByOrg.getId())) {
            throw new IllegalArgumentException("任务创建组织与计划创建组织不一致");
        }
        if ("FUNC_TO_COLLEGE".equals(planLevel) && org.getType() != OrgType.academic) {
            throw new IllegalArgumentException("FUNC_TO_COLLEGE 计划只能绑定二级学院任务");
        }
        if ("STRAT_TO_FUNC".equals(planLevel) && org.getType() != OrgType.functional) {
            throw new IllegalArgumentException("STRAT_TO_FUNC 计划只能绑定职能部门任务");
        }
    }

    private StrategicTask loadTaskWithAccess(Long id, CurrentUser currentUser, boolean isAdmin) {
        StrategicTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        ensureTaskAccessible(task, currentUser, isAdmin);
        return task;
    }

    private SysOrg loadOrganization(Long orgId, String errorMessage) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException(errorMessage));
    }

    private void ensureCanOperateOnOrg(CurrentUser currentUser, boolean isAdmin, Long orgId) {
        if (isAdmin || orgId == null) {
            return;
        }
        if (currentUser == null) {
            throw new AuthorizationException("无法获取当前用户信息，请联系管理员");
        }
        Long currentOrgId = currentUser.getOrgId();
        if (currentOrgId == null) {
            throw new AuthorizationException("当前用户缺少组织信息，无法操作任务数据");
        }
        if (!Objects.equals(currentOrgId, orgId)) {
            throw new AuthorizationException("无权操作该组织下的任务");
        }
    }

    private void ensureTaskAccessible(StrategicTask task, CurrentUser currentUser, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (currentUser == null) {
            throw new AuthorizationException("无法获取当前用户信息，请联系管理员");
        }
        Long currentOrgId = currentUser.getOrgId();
        if (currentOrgId == null) {
            throw new AuthorizationException("当前用户缺少组织信息，无法访问任务数据");
        }

        Long taskOrgId = task.getOrg() != null ? task.getOrg().getId() : null;
        Long createdByOrgId = task.getCreatedByOrg() != null ? task.getCreatedByOrg().getId() : null;
        if (!Objects.equals(taskOrgId, currentOrgId) && !Objects.equals(createdByOrgId, currentOrgId)) {
            throw new AuthorizationException("无权操作该任务");
        }
    }

    private void ensureTaskDeletable(StrategicTask task) {
        String status = task.getStatus();
        if (!Objects.equals(status, TaskStatus.DRAFT.value())
                && !Objects.equals(status, TaskStatus.CANCELLED.value())) {
            throw new IllegalStateException("只能删除草稿或已取消的任务");
        }
    }

    private void publishAndSaveEvents(com.sism.shared.domain.model.base.AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();

        for (DomainEvent event : events) {
            eventStore.save(event);
        }

        eventPublisher.publishAll(events);

        aggregate.clearEvents();
    }

    private Pageable buildSearchPageable(TaskQueryRequest request) {
        int page = request.getPage() != null ? Math.max(request.getPage(), 0) : 0;
        int size = request.getSize() != null ? Math.max(request.getSize(), 1) : 10;

        Sort sort = buildSearchSort(request.getSortBy(), request.getSortDirection());
        return PageRequest.of(page, size, sort);
    }

    private Sort buildSearchSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Order.asc("t.sort_order"), Sort.Order.asc("t.task_id"));
        }

        return switch (sortBy) {
            case "id" -> Sort.by(direction, "t.task_id");
            case "name" -> Sort.by(direction, "t.name");
            case "taskType" -> Sort.by(direction, "t.task_type");
            case "planId" -> Sort.by(direction, "t.plan_id");
            case "cycleId" -> Sort.by(direction, "t.cycle_id");
            case "orgId" -> Sort.by(direction, "t.org_id");
            case "createdByOrgId" -> Sort.by(direction, "t.created_by_org_id");
            case "sortOrder" -> Sort.by(direction, "t.sort_order");
            case "planStatus" -> Sort.by(direction, "p.status");
            case "taskStatus" -> Sort.by(direction, "t.status");
            case "createdAt" -> Sort.by(direction, "t.created_at");
            case "updatedAt" -> Sort.by(direction, "t.updated_at");
            default -> Sort.by(Sort.Order.asc("t.sort_order"), Sort.Order.asc("t.task_id"));
        };
    }
}
