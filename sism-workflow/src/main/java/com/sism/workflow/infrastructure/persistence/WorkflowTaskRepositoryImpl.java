package com.sism.workflow.infrastructure.persistence;

import com.sism.workflow.domain.runtime.model.WorkflowTask;
import com.sism.workflow.domain.runtime.repository.WorkflowTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WorkflowTaskRepositoryImpl implements WorkflowTaskRepository {

    private final WorkflowTaskJpaRepository workflowTaskJpaRepository;

    @Override
    public List<WorkflowTask> findAll() {
        return workflowTaskJpaRepository.findAll();
    }

    @Override
    public Optional<WorkflowTask> findById(Long id) {
        return workflowTaskJpaRepository.findById(id);
    }

    @Override
    public List<WorkflowTask> findByStatus(String status) {
        return workflowTaskJpaRepository.findByStatus(status);
    }

    @Override
    public List<WorkflowTask> findByAssigneeId(Long assigneeId) {
        return workflowTaskJpaRepository.findByAssigneeId(assigneeId);
    }

    @Override
    public WorkflowTask save(WorkflowTask task) {
        return workflowTaskJpaRepository.saveAndFlush(task);
    }
}
