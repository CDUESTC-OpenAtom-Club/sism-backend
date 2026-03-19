package com.sism.workflow.application.runtime;

import com.sism.workflow.application.support.FlowResolver;
import com.sism.workflow.application.support.StepInstanceFactory;
import com.sism.workflow.application.support.WorkflowEventDispatcher;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.repository.FlowDefinitionRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StartWorkflowUseCase {

    private final FlowResolver flowResolver;
    private final FlowDefinitionRepository flowDefinitionRepository;
    private final AuditInstanceRepository auditInstanceRepository;
    private final StepInstanceFactory stepInstanceFactory;
    private final WorkflowEventDispatcher workflowEventDispatcher;

    @Transactional
    public AuditInstance startAuditInstance(AuditInstance instance, Long requesterId, Long requesterOrgId) {
        instance.validate();
        flowResolver.resolveAndAttachFlow(instance);
        instance.start(requesterId, requesterOrgId);

        AuditFlowDef flowDef = instance.getFlowDefId() == null
                ? null
                : flowDefinitionRepository.findById(instance.getFlowDefId()).orElse(null);
        stepInstanceFactory.initialize(instance, flowDef, requesterId, requesterOrgId);

        AuditInstance saved = auditInstanceRepository.save(instance);
        workflowEventDispatcher.publish(saved);
        return saved;
    }
}
