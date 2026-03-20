package com.sism.workflow.application.runtime;

import com.sism.workflow.application.support.WorkflowEventDispatcher;
import com.sism.workflow.application.PlanWorkflowSyncService;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelWorkflowUseCase {

    private final AuditInstanceRepository auditInstanceRepository;
    private final WorkflowEventDispatcher workflowEventDispatcher;
    private final PlanWorkflowSyncService planWorkflowSyncService;

    @Transactional
    public AuditInstance cancel(AuditInstance instance) {
        instance.cancel();
        AuditInstance saved = auditInstanceRepository.save(instance);
        planWorkflowSyncService.syncAfterWorkflowChanged(saved);
        workflowEventDispatcher.publish(saved);
        return saved;
    }
}
