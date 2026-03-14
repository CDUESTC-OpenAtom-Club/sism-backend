package com.sism.task.domain.repository;

import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;

import java.util.List;
import java.util.Optional;

/**
 * TaskRepository - 任务仓储接口
 * 定义在领域层，由基础设施层实现
 */
public interface TaskRepository {

    Optional<StrategicTask> findById(Long id);

    List<StrategicTask> findAll();

    List<StrategicTask> findByOrgId(Long orgId);

    List<StrategicTask> findByStatus(String status);

    List<StrategicTask> findByTaskType(TaskType taskType);

    List<StrategicTask> findByIndicatorId(Long indicatorId);

    List<StrategicTask> findByAssigneeId(Long assigneeId);

    StrategicTask save(StrategicTask task);

    void delete(StrategicTask task);

    boolean existsById(Long id);
}
