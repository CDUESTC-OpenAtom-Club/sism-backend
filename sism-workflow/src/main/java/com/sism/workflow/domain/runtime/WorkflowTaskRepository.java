package com.sism.workflow.domain.runtime;

import com.sism.workflow.domain.runtime.WorkflowTask;

import java.util.List;
import java.util.Optional;

public interface WorkflowTaskRepository {

    List<WorkflowTask> findAll();

    Optional<WorkflowTask> findById(Long id);

    List<WorkflowTask> findByStatus(String status);

    List<WorkflowTask> findByAssigneeId(Long assigneeId);

    WorkflowTask save(WorkflowTask task);
}
