package com.sism.repository;

import com.sism.entity.AuditFlowDef;
import com.sism.enums.AuditEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AuditFlowDef entity
 */
@Repository
public interface AuditFlowDefRepository extends JpaRepository<AuditFlowDef, Long> {

    /**
     * Find audit flow by flow code
     */
    Optional<AuditFlowDef> findByFlowCode(String flowCode);

    /**
     * Find audit flows by entity type
     */
    List<AuditFlowDef> findByEntityType(AuditEntityType entityType);

    /**
     * Check if flow code exists
     */
    boolean existsByFlowCode(String flowCode);
}
