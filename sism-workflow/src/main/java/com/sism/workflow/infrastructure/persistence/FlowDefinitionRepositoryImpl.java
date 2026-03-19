package com.sism.workflow.infrastructure.persistence;

import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.repository.FlowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FlowDefinitionRepositoryImpl implements FlowDefinitionRepository {

    private final JpaWorkflowRepository jpaWorkflowRepository;
    private final AuditFlowDefJpaRepository auditFlowDefJpaRepository;

    @Override
    public List<AuditFlowDef> findAll() {
        return jpaWorkflowRepository.findAllAuditFlowDefs();
    }

    @Override
    public Page<AuditFlowDef> findAll(Pageable pageable) {
        return auditFlowDefJpaRepository.findAll(pageable);
    }

    @Override
    public Optional<AuditFlowDef> findById(Long id) {
        return jpaWorkflowRepository.findAuditFlowDefById(id);
    }

    @Override
    public Optional<AuditFlowDef> findByCode(String flowCode) {
        return jpaWorkflowRepository.findAuditFlowDefByCode(flowCode);
    }

    @Override
    public List<AuditFlowDef> findByEntityType(String entityType) {
        return jpaWorkflowRepository.findAuditFlowDefsByEntityType(entityType);
    }

    @Override
    public AuditFlowDef save(AuditFlowDef flowDef) {
        return auditFlowDefJpaRepository.saveAndFlush(flowDef);
    }
}
