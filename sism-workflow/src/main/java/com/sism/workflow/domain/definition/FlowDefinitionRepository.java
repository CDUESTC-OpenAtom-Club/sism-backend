package com.sism.workflow.domain.definition;

import com.sism.workflow.domain.definition.AuditFlowDef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Workflow definition repository.
 */
public interface FlowDefinitionRepository {

    List<AuditFlowDef> findAll();

    Page<AuditFlowDef> findAll(Pageable pageable);

    Optional<AuditFlowDef> findById(Long id);

    Optional<AuditFlowDef> findByCode(String flowCode);

    List<AuditFlowDef> findByEntityType(String entityType);

    AuditFlowDef save(AuditFlowDef flowDef);
}
