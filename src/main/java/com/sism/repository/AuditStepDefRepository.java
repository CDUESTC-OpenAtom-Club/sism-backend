package com.sism.repository;

import com.sism.entity.AuditStepDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AuditStepDef entity
 */
@Repository
public interface AuditStepDefRepository extends JpaRepository<AuditStepDef, Long> {

    /**
     * Find all steps for a flow, ordered by step order
     */
    List<AuditStepDef> findByFlowIdOrderByStepOrderAsc(Long flowId);

    /**
     * Find steps by flow ID
     */
    List<AuditStepDef> findByFlowId(Long flowId);

    /**
     * Find required steps for a flow
     */
    List<AuditStepDef> findByFlowIdAndIsRequiredTrue(Long flowId);

    /**
     * Find step by flow ID and step order
     */
    @Query("SELECT s FROM AuditStepDef s WHERE s.flowId = :flowId AND s.stepOrder = :stepOrder")
    AuditStepDef findByFlowIdAndStepOrder(@Param("flowId") Long flowId, @Param("stepOrder") Integer stepOrder);

    /**
     * Delete all steps for a flow
     */
    void deleteByFlowId(Long flowId);
}
