package com.sism.task.application;

import com.sism.common.PageResult;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.task.application.dto.CreateTaskRequest;
import com.sism.task.application.dto.UpdateTaskRequest;
import com.sism.task.application.dto.TaskQueryRequest;
import com.sism.task.application.dto.TaskResponse;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskCategory;
import com.sism.task.domain.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import com.sism.task.infrastructure.persistence.JpaTaskRepositoryInternal;
import com.sism.task.infrastructure.persistence.PlanBindingRepository;
import com.sism.task.infrastructure.persistence.TaskFlatView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskApplicationService Tests")
class TaskApplicationServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private JpaTaskRepositoryInternal jpaTaskRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private EventStore eventStore;

    @Mock
    private PlanBindingRepository planBindingRepository;

    @InjectMocks
    private TaskApplicationService taskApplicationService;

    @Test
    @DisplayName("Should search tasks via paged flat query and preserve total size")
    void shouldSearchTasksViaPagedFlatQueryAndPreserveTotalSize() {
        TaskQueryRequest request = new TaskQueryRequest();
        request.setCycleId(90L);
        request.setPage(0);
        request.setSize(2);
        request.setSortBy("name");
        request.setSortDirection("desc");
        request.setPlanStatus("DRAFT");
        request.setTaskStatus("DRAFT");

        when(jpaTaskRepository.findPagedFlatViewsByCriteria(
                eq(null),
                eq(90L),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq("DRAFT"),
                eq("DRAFT"),
                eq(35L),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(
                                new StubTaskFlatView(1L, "任务A", 2, 35L, 35L, "STRATEGIC"),
                                new StubTaskFlatView(2L, "任务B", 1, 35L, 35L, "STRATEGIC")
                        ),
                        org.springframework.data.domain.PageRequest.of(0, 2),
                        3
                ));

        PageResult<TaskResponse> result = taskApplicationService.searchTasks(request, 35L);

        assertEquals(3, result.getTotal());
        assertEquals(2, result.getItems().size());
        assertEquals(1L, result.getItems().get(0).getId());
        assertEquals(2L, result.getItems().get(1).getId());
        assertEquals("DRAFT", result.getItems().get(0).getPlanStatus());
        assertEquals("DRAFT", result.getItems().get(0).getTaskStatus());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaTaskRepository).findPagedFlatViewsByCriteria(
                eq(null),
                eq(90L),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq("DRAFT"),
                eq("DRAFT"),
                eq(35L),
                pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(2, pageableCaptor.getValue().getPageSize());
        assertEquals("t.name: DESC", pageableCaptor.getValue().getSort().toString());
        verifyNoInteractions(taskRepository);
    }

    @Test
    @DisplayName("Should reject creating task on func-to-college plan for functional org")
    void shouldRejectCreatingTaskOnFuncToCollegePlanForFunctionalOrg() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setName("测试任务");
        request.setTaskType(TaskType.BASIC);
        request.setPlanId(705463L);
        request.setCycleId(4L);
        request.setOrgId(36L);
        request.setCreatedByOrgId(35L);

        SysOrg org = SysOrg.create("党委办公室", OrgType.functional);
        org.setId(36L);
        SysOrg createdByOrg = SysOrg.create("战略发展部", OrgType.admin);
        createdByOrg.setId(35L);

        when(organizationRepository.findById(36L)).thenReturn(java.util.Optional.of(org));
        when(organizationRepository.findById(35L)).thenReturn(java.util.Optional.of(createdByOrg));
        when(planBindingRepository.findByPlanId(705463L))
                .thenReturn(Optional.of(new PlanBindingRepository.PlanBindingInfo(4L, 36L, 35L, "FUNC_TO_COLLEGE")));

        assertThrows(IllegalArgumentException.class, () ->
                taskApplicationService.createTask(request, null, true));
        verifyNoInteractions(taskRepository);
    }

    @Test
    @DisplayName("Should reject creating task for foreign org when not admin")
    void shouldRejectCreatingTaskForForeignOrgWhenNotAdmin() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setName("测试任务");
        request.setTaskType(TaskType.BASIC);
        request.setPlanId(100L);
        request.setCycleId(200L);
        request.setOrgId(99L);
        request.setCreatedByOrgId(35L);

        assertThrows(RuntimeException.class, () ->
                taskApplicationService.createTask(request, currentUser(36L), false));
        verifyNoInteractions(taskRepository, organizationRepository, planBindingRepository, eventPublisher, eventStore);
    }

    @Test
    @DisplayName("Should reject deleting an active task")
    void shouldRejectDeletingAnActiveTask() {
        StrategicTask task = createTask();
        task.setId(1L);
        task.setStatus(StrategicTask.STATUS_ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(IllegalStateException.class, () -> taskApplicationService.deleteTask(1L, currentUser(36L), false));
        verify(taskRepository, never()).save(task);
        verifyNoInteractions(eventStore, eventPublisher);
    }

    @Test
    @DisplayName("Should allow deleting a draft task")
    void shouldAllowDeletingADraftTask() {
        StrategicTask task = createTask();
        task.setId(1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertDoesNotThrow(() -> taskApplicationService.deleteTask(1L, currentUser(36L), false));
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("Should reject updating task for foreign org when not admin")
    void shouldRejectUpdatingTaskForForeignOrgWhenNotAdmin() {
        StrategicTask task = createTask();
        task.setId(1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setName("更新任务");
        request.setTaskType(TaskType.BASIC);
        request.setPlanId(100L);
        request.setCycleId(200L);
        request.setOrgId(99L);
        request.setCreatedByOrgId(35L);

        assertThrows(RuntimeException.class, () ->
                taskApplicationService.updateTask(1L, request, currentUser(36L), false));
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(organizationRepository, planBindingRepository, eventPublisher, eventStore);
    }

    @Test
    @DisplayName("Should load accessible tasks with a single repository query")
    void shouldLoadAccessibleTasksWithSingleRepositoryQuery() {
        when(jpaTaskRepository.findFlatViewsByAccessibleOrgId(35L)).thenReturn(List.of(
                new StubTaskFlatView(1L, "任务A", 1, 35L, 99L, "STRATEGIC"),
                new StubTaskFlatView(2L, "任务B", 2, 99L, 35L, "STRATEGIC")
        ));

        List<TaskResponse> result = taskApplicationService.getAccessibleTasksByOrgId(35L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(35L, result.get(0).getOrgId());
        assertEquals(99L, result.get(1).getOrgId());
        verify(jpaTaskRepository).findFlatViewsByAccessibleOrgId(35L);
        verifyNoInteractions(taskRepository, organizationRepository, eventPublisher, eventStore, planBindingRepository);
    }

    private StrategicTask createTask() {
        SysOrg org = SysOrg.create("党委办公室", OrgType.functional);
        org.setId(36L);
        SysOrg createdByOrg = SysOrg.create("战略发展部", OrgType.admin);
        createdByOrg.setId(35L);
        return StrategicTask.create(
                TaskCategory.STRATEGIC,
                "测试任务",
                TaskType.BASIC,
                100L,
                200L,
                org,
                createdByOrg
        );
    }

    private CurrentUser currentUser(Long orgId) {
        return new CurrentUser(1L, "tester", "Tester", null, orgId, List.of());
    }

    private record StubTaskFlatView(
            Long id,
            String name,
            Integer sortOrder,
            Long orgId,
            Long createdByOrgId,
            String taskCategory,
            String taskType) implements TaskFlatView {

        private StubTaskFlatView(Long id, String name, Integer sortOrder, Long orgId, Long createdByOrgId, String taskCategory) {
            this(id, name, sortOrder, orgId, createdByOrgId, taskCategory, "BASIC");
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDesc() {
            return "描述-" + id;
        }

        @Override
        public String getTaskType() {
            return taskType;
        }

        @Override
        public Long getPlanId() {
            return 1L;
        }

        @Override
        public Long getCycleId() {
            return 90L;
        }

        @Override
        public Long getOrgId() {
            return orgId;
        }

        @Override
        public Long getCreatedByOrgId() {
            return createdByOrgId;
        }

        @Override
        public Integer getSortOrder() {
            return sortOrder;
        }

        @Override
        public String getPlanStatus() {
            return "DRAFT";
        }

        @Override
        public String getTaskStatus() {
            return "DRAFT";
        }

        @Override
        public String getRemark() {
            return "remark";
        }

        @Override
        public LocalDateTime getCreatedAt() {
            return LocalDateTime.of(2026, 1, 1, 0, 0);
        }

        @Override
        public LocalDateTime getUpdatedAt() {
            return LocalDateTime.of(2026, 1, 2, 0, 0);
        }

        @Override
        public String getTaskCategory() {
            return taskCategory;
        }
    }
}
