package com.sism.workflow.infrastructure.persistence;

import com.sism.workflow.domain.definition.model.AuditFlowDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AuditFlowDefJpaRepository - 审批流定义 JPA Repository
 */
@Repository
public interface AuditFlowDefJpaRepository extends JpaRepository<AuditFlowDef, Long> {
}
