package com.sism.task.application;

import com.sism.common.PageResult;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.task.application.dto.CreateTaskRequest;
import com.sism.task.application.dto.TaskQueryRequest;
import com.sism.task.application.dto.TaskResponse;
import com.sism.task.domain.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import com.sism.task.infrastructure.persistence.JpaTaskRepositoryInternal;
import com.sism.task.infrastructure.persistence.TaskFlatView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
    private EntityManager entityManager;

    @InjectMocks
    private TaskApplicationService taskApplicationService;

    @Test
    @DisplayName("Should search tasks via flat query and paginate safely")
    void shouldSearchTasksViaFlatQueryAndPaginateSafely() {
        TaskQueryRequest request = new TaskQueryRequest();
        request.setCycleId(90L);
        request.setPage(0);
        request.setSize(2);

        when(jpaTaskRepository.findFlatViewsByCriteria(null, 90L, null, null, "", ""))
                .thenReturn(List.of(
                        new StubTaskFlatView(1L, "任务A", 2),
                        new StubTaskFlatView(2L, "任务B", 1),
                        new StubTaskFlatView(3L, "任务C", 3)
                ));

        PageResult<TaskResponse> result = taskApplicationService.searchTasks(request);

        assertEquals(3, result.getTotal());
        assertEquals(2, result.getItems().size());
        assertEquals(1L, result.getItems().get(0).getId());
        assertEquals(2L, result.getItems().get(1).getId());
        assertEquals("DRAFT", result.getItems().get(0).getPlanStatus());
        assertEquals("DRAFT", result.getItems().get(0).getTaskStatus());

        verify(jpaTaskRepository).findFlatViewsByCriteria(null, 90L, null, null, "", "");
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

        Query query = mock(Query.class);
        when(organizationRepository.findById(36L)).thenReturn(java.util.Optional.of(org));
        when(organizationRepository.findById(35L)).thenReturn(java.util.Optional.of(createdByOrg));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("planId", 705463L)).thenReturn(query);
        when(query.getResultStream()).thenReturn(Stream.of(new Object[] {4L, 36L, 35L, "FUNC_TO_COLLEGE"}));

        assertThrows(IllegalArgumentException.class, () -> taskApplicationService.createTask(request));
        verifyNoInteractions(taskRepository);
    }

    private record StubTaskFlatView(Long id, String name, Integer sortOrder) implements TaskFlatView {

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
            return "BASIC";
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
            return 35L;
        }

        @Override
        public Long getCreatedByOrgId() {
            return 35L;
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
    }
}
