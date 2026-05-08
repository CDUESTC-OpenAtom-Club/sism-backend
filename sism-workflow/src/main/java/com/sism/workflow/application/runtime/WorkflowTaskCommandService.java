package com.sism.workflow.application.runtime;

import com.sism.workflow.application.support.WorkflowEventDispatcher;
import com.sism.workflow.domain.runtime.WorkflowTask;
import com.sism.workflow.domain.runtime.WorkflowTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkflowTaskCommandService {

    private final WorkflowTaskRepository workflowTaskRepository;
    private final WorkflowEventDispatcher workflowEventDispatcher;

    @Transactional
    public WorkflowTask start(WorkflowTask task, Long operatorId, Long operatorOrgId) {
        task.validate();
        task.start(operatorId, operatorOrgId);
        WorkflowTask saved = workflowTaskRepository.save(task);
        workflowEventDispatcher.publish(saved);
        return saved;
    }

    @Transactional
    public WorkflowTask complete(WorkflowTask task, String result) {
        task.complete(result);
        WorkflowTask saved = workflowTaskRepository.save(task);
        workflowEventDispatcher.publish(saved);
        return saved;
    }

    @Transactional
    public WorkflowTask fail(WorkflowTask task, String errorMessage) {
        task.fail(errorMessage);
        WorkflowTask saved = workflowTaskRepository.save(task);
        workflowEventDispatcher.publish(saved);
        return saved;
    }

    @Transactional
    public WorkflowTask approve(WorkflowTask task, Long approverId, String comment) {
        task.approve(approverId, comment);
        WorkflowTask saved = workflowTaskRepository.save(task);
        workflowEventDispatcher.publish(saved);
        return saved;
    }

    @Transactional
    public WorkflowTask reject(WorkflowTask task, Long approverId, String comment) {
        task.reject(approverId, comment);
        WorkflowTask saved = workflowTaskRepository.save(task);
        workflowEventDispatcher.publish(saved);
        return saved;
    }
}
