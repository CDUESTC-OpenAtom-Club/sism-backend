package com.sism.task.application.dto;

import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import com.sism.task.infrastructure.persistence.TaskFlatView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("TaskResponse Mapping Tests")
class TaskResponseTest {

    @Test
    @DisplayName("Should map Chinese development task type from flat view")
    void shouldMapChineseDevelopmentTaskTypeFromFlatView() {
        TaskResponse response = TaskResponse.fromView(new StubTaskFlatView("发展性", "STRATEGIC", "DRAFT"));

        assertEquals(TaskType.DEVELOPMENT, response.getTaskType());
        assertEquals("DISTRIBUTED", response.getPlanStatus());
        assertEquals("DRAFT", response.getTaskStatus());
    }

    @Test
    @DisplayName("Should keep blank task type as null")
    void shouldKeepBlankTaskTypeAsNull() {
        TaskResponse response = TaskResponse.fromView(new StubTaskFlatView(" ", "STRATEGIC", "DRAFT"));

        assertNull(response.getTaskType());
    }

    @Test
    @DisplayName("Should keep unknown task type as null")
    void shouldKeepUnknownTaskTypeAsNull() {
        TaskResponse response = TaskResponse.fromView(new StubTaskFlatView("mystery", "STRATEGIC", "DRAFT"));

        assertNull(response.getTaskType());
    }

    @Test
    @DisplayName("Should default missing flat view task status to draft")
    void shouldDefaultMissingFlatViewTaskStatusToDraft() {
        TaskResponse response = TaskResponse.fromView(new StubTaskFlatView("BASIC", "STRATEGIC", null));

        assertEquals("DRAFT", response.getTaskStatus());
    }

    @Test
    @DisplayName("Should default blank flat view task status to draft")
    void shouldDefaultBlankFlatViewTaskStatusToDraft() {
        TaskResponse response = TaskResponse.fromView(new StubTaskFlatView("BASIC", "STRATEGIC", " "));

        assertEquals("DRAFT", response.getTaskStatus());
    }

    @Test
    @DisplayName("Should default unknown flat view task category to strategic")
    void shouldDefaultUnknownFlatViewTaskCategoryToStrategic() {
        TaskResponse response = TaskResponse.fromView(new StubTaskFlatView("BASIC", "legacy", "DRAFT"));

        assertEquals(com.sism.task.domain.TaskCategory.STRATEGIC, response.getTaskCategory());
    }

    @Test
    @DisplayName("Should default missing entity task category and status")
    void shouldDefaultMissingEntityTaskCategoryAndStatus() {
        StrategicTask task = new StrategicTask();
        task.setName("示例任务");
        task.setTaskType(TaskType.BASIC);
        task.setPlanId(10L);
        task.setCycleId(90L);
        task.setStatus((String) null);
        task.setTaskCategory(null);

        TaskResponse response = TaskResponse.fromEntity(task);

        assertEquals("DRAFT", response.getTaskStatus());
        assertEquals(com.sism.task.domain.TaskCategory.STRATEGIC, response.getTaskCategory());
    }

    private record StubTaskFlatView(String taskType, String taskCategory, String taskStatus) implements TaskFlatView {

        @Override
        public Long getId() {
            return 1L;
        }

        @Override
        public String getName() {
            return "示例任务";
        }

        @Override
        public String getDesc() {
            return "示例描述";
        }

        @Override
        public String getTaskType() {
            return taskType;
        }

        @Override
        public Long getPlanId() {
            return 10L;
        }

        @Override
        public Long getCycleId() {
            return 90L;
        }

        @Override
        public Long getOrgId() {
            return 44L;
        }

        @Override
        public Long getCreatedByOrgId() {
            return 35L;
        }

        @Override
        public Integer getSortOrder() {
            return 1;
        }

        @Override
        public String getPlanStatus() {
            return "DISTRIBUTED";
        }

        @Override
        public String getTaskStatus() {
            return taskStatus;
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
