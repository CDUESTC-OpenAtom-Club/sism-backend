package com.sism.task.domain;

import com.sism.organization.domain.SysOrg;
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
        assertEquals("测试任务", task.getTaskName());
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

        task.updateTaskName("新任务名称");

        assertEquals("新任务名称", task.getTaskName());
        assertNotNull(task.getUpdatedAt());
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

    @Test
    @DisplayName("Should initialize redundant fields in create()")
    void shouldInitializeRedundantFieldsInCreate() {
        StrategicTask task = StrategicTask.create("测试任务", TaskType.BASIC, 1L, 1L, org, createdByOrg);

        // 验证冗余字段是否正确初始化
        assertEquals("测试任务", task.getName(), "冗余字段 'name' 应该等于 taskName");
        assertEquals(TaskType.BASIC, task.getType(), "冗余字段 'type' 应该等于 taskType");
        assertNull(task.getDesc(), "冗余字段 'desc' 应该为 null");
    }

    @Test
    @DisplayName("Should sync redundant fields via @PrePersist")
    void shouldSyncRedundantFieldsViaPrePersist() {
        StrategicTask task = new StrategicTask();
        task.setTaskName("预持久化任务");
        task.setTaskType(TaskType.DEVELOPMENT);

        // 模拟 @PrePersist 行为
        task.onCreate();

        assertEquals("预持久化任务", task.getName(), "@PrePersist 应该同步 name 字段");
        assertEquals(TaskType.DEVELOPMENT, task.getType(), "@PrePersist 应该同步 type 字段");
    }
}
