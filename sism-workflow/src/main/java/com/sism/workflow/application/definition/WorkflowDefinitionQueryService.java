package com.sism.workflow.application.definition;

import com.sism.workflow.domain.definition.AuditFlowDef;
import com.sism.workflow.domain.definition.FlowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkflowDefinitionQueryService {

    private final FlowDefinitionRepository flowDefinitionRepository;

    public List<AuditFlowDef> getAllAuditFlowDefs() {
        return flowDefinitionRepository.findAll();
    }

    public Page<AuditFlowDef> getAllAuditFlowDefs(Pageable pageable) {
        return flowDefinitionRepository.findAll(pageable);
    }

    public AuditFlowDef getAuditFlowDefById(Long id) {
        return flowDefinitionRepository.findById(id).orElse(null);
    }

    public AuditFlowDef getAuditFlowDefByCode(String flowCode) {
        return flowDefinitionRepository.findByCode(flowCode).orElse(null);
    }

    public List<AuditFlowDef> getAuditFlowDefsByEntityType(String entityType) {
        return flowDefinitionRepository.findByEntityType(entityType);
    }

    public AuditFlowDef createAuditFlowDef(AuditFlowDef flowDef) {
        flowDef.validate();
        return flowDefinitionRepository.save(flowDef);
    }
}
