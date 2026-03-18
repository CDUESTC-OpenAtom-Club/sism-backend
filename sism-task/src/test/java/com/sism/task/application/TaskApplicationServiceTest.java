package com.sism.task.application;

import com.sism.common.PageResult;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.task.application.dto.TaskQueryRequest;
import com.sism.task.application.dto.TaskResponse;
import com.sism.task.domain.repository.TaskRepository;
import com.sism.task.infrastructure.persistence.JpaTaskRepositoryInternal;
import com.sism.task.infrastructure.persistence.TaskFlatView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        verify(jpaTaskRepository).findFlatViewsByCriteria(null, 90L, null, null, "", "");
        verifyNoInteractions(taskRepository);
    }

    private record StubTaskFlatView(Long id, String taskName, Integer sortOrder) implements TaskFlatView {

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getTaskName() {
            return taskName;
        }

        @Override
        public String getTaskDesc() {
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
        public String getStatus() {
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
