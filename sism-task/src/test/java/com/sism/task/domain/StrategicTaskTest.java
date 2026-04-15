package com.sism.task.domain;

import com.sism.organization.domain.SysOrg;
import com.sism.task.domain.event.TaskCreatedEvent;
import com.sism.task.domain.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StrategicTask Aggregate Root Tests")
class StrategicTaskTest {

    private SysOrg org;
    private SysOrg createdByOrg;

    @BeforeEach
    void setUp() {
        org = SysOrg.create("测试学院", com.sism.organization.domain.OrgType.academic);
        createdByOrg = SysOrg.create("战略发展部", com.sism.organization.domain.OrgType.admin);
    }

    @Test
    @DisplayName("Should create StrategicTask with valid parameters")
    void shouldCreateStrategicTaskWithValidParameters() {
        StrategicTask task = StrategicTask.create("测试任务", TaskType.BASIC, 1L, 1L, org, createdByOrg);

        assertNotNull(task);
        assertEquals("测试任务", task.getName());
        assertEquals(TaskType.BASIC, task.getTaskType());
        assertEquals(1L, task.getPlanId());
        assertEquals(1L, task.getCycleId());
        assertEquals(org, task.getOrg());
        assertEquals(createdByOrg, task.getCreatedByOrg());
        assertEquals(StrategicTask.STATUS_DRAFT, task.getStatus());
        assertFalse(task.getIsDeleted());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    @DisplayName("Should queue TaskCreatedEvent only after ID is assigned")
    void shouldQueueTaskCreatedEventOnlyAfterIdIsAssigned() {
        org.setId(10L);
        createdByOrg.setId(11L);

        StrategicTask task = StrategicTask.create("测试任务", TaskType.BASIC, 1L, 1L, org, createdByOrg);

        assertTrue(task.getDomainEvents().isEmpty());

        task.setId(99L);
        task.onCreated();

        assertEquals(1, task.getDomainEvents().size());
        TaskCreatedEvent createdEvent = (TaskCreatedEvent) task.getDomainEvents().get(0);
        assertEquals(99L, createdEvent.getTaskId());
        assertEquals("测试任务", createdEvent.getTaskName());
        assertEquals(10L, createdEvent.getOrgId());
    }

    @Test
    @DisplayName("Should expose strongly typed task status")
    void shouldExposeStronglyTypedTaskStatus() {
        StrategicTask task = StrategicTask.create("测试任务", TaskType.BASIC, 1L, 1L, org, createdByOrg);

        assertEquals(TaskStatus.DRAFT, task.getStatusEnum());
        assertEquals(TaskStatus.DRAFT.value(), task.getPlanStatus());
        task.activate();
        assertEquals(TaskStatus.ACTIVE, task.getStatusEnum());
        assertEquals(TaskStatus.ACTIVE.value(), task.getPlanStatus());
    }

    @Test
    @DisplayName("Should throw exception when creating StrategicTask with null task name")
    void shouldThrowExceptionWhenCreatingStrategicTaskWithNullTaskName() {
        assertThrows(IllegalArgumentException.class, () ->
            StrategicTask.create(null, TaskType.BASIC, 1L, 1L, org, createdByOrg)
        );
    }

    @Test
    @DisplayName("Should throw exception when creating StrategicTask with null task type")
    void shouldThrowExceptionWhenCreatingStrategicTaskWithNullTaskType() {
        assertThrows(IllegalArgumentException.class, () ->
            StrategicTask.create("测试任务", null, 1L, 1L, org, createdByOrg)
        );
    }

    @Test
    @DisplayName("Should activate StrategicTask successfully")
    void shouldActivateStrategicTaskSuccessfully() {
        StrategicTask task = StrategicTask.create("测试任务", TaskType.BASIC, 1L, 1L, org, createdByOrg);

        task.activate();

        assertEquals(StrategicTask.STATUS_ACTIVE, task.getStatus());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    @DisplayName("Should complete StrategicTask successfully")
    void shouldCompleteStrategicTaskSuccessfully() {
        StrategicTask task = StrategicTask.create("测试任务", TaskType.BASIC, 1L, 1L, org, createdByOrg);
        task.activate();

        task.complete();

        assertEquals(StrategicTask.STATUS_COMPLETED, task.getStatus());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    @DisplayName("Should cancel StrategicTask successfully")
    void shouldCancelStrategicTaskSuccessfully() {
        StrategicTask task = StrategicTask.create("测试任务", TaskType.BASIC, 1L, 1L, org, createdByOrg);
        task.activate();

        task.cancel();

        assertEquals(StrategicTask.STATUS_CANCELLED, task.getStatus());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update task name successfully")
    void shouldUpdateTaskNameSuccessfully() {
        StrategicTask task = StrategicTask.create("旧任务名称", TaskType.BASIC, 1L, 1L, org, createdByOrg);

        task.updateName("新任务名称");

        assertEquals("新任务名称", task.getName());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    @DisplayName("Should reassign task boundary through domain method")
    void shouldReassignTaskBoundaryThroughDomainMethod() {
        StrategicTask task = StrategicTask.create("测试任务", TaskType.BASIC, 1L, 1L, org, createdByOrg);
        SysOrg newOrg = SysOrg.create("党委办公室", com.sism.organization.domain.OrgType.functional);
        newOrg.setId(99L);
        SysOrg newCreatedByOrg = SysOrg.create("战略发展部", com.sism.organization.domain.OrgType.admin);
        newCreatedByOrg.setId(100L);

        task.reassign(TaskType.DEVELOPMENT, 2L, 3L, newOrg, newCreatedByOrg);

        assertEquals(TaskType.DEVELOPMENT, task.getTaskType());
        assertEquals(2L, task.getPlanId());
        assertEquals(3L, task.getCycleId());
        assertEquals(99L, task.getOrg().getId());
        assertEquals(100L, task.getCreatedByOrg().getId());
    }

    @Test
    @DisplayName("Should validate StrategicTask with valid parameters")
    void shouldValidateStrategicTaskWithValidParameters() {
        StrategicTask task = StrategicTask.create("有效任务", TaskType.BASIC, 1L, 1L, org, createdByOrg);

        assertDoesNotThrow(task::validate);
    }

    @Test
    @DisplayName("Should validate StrategicTask with invalid parameters")
    void shouldValidateStrategicTaskWithInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            StrategicTask.create(null, null, null, null, null, null);
        });
    }
}
