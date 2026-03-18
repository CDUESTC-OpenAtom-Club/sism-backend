package com.sism.task.application.dto;

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
        TaskResponse response = TaskResponse.fromView(new StubTaskFlatView("发展性"));

        assertEquals(TaskType.DEVELOPMENT, response.getTaskType());
    }

    @Test
    @DisplayName("Should keep blank task type as null")
    void shouldKeepBlankTaskTypeAsNull() {
        TaskResponse response = TaskResponse.fromView(new StubTaskFlatView(" "));

        assertNull(response.getTaskType());
    }

    @Test
    @DisplayName("Should keep unknown task type as null")
    void shouldKeepUnknownTaskTypeAsNull() {
        TaskResponse response = TaskResponse.fromView(new StubTaskFlatView("mystery"));

        assertNull(response.getTaskType());
    }

    private record StubTaskFlatView(String taskType) implements TaskFlatView {

        @Override
        public Long getId() {
            return 1L;
        }

        @Override
        public String getTaskName() {
            return "示例任务";
        }

        @Override
        public String getTaskDesc() {
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
        public String getStatus() {
            return "DISTRIBUTED";
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
