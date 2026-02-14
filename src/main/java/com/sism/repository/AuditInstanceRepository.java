package com.sism.repository;

import com.sism.entity.AuditInstance;
import com.sism.enums.AuditEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AuditInstance entity
 */
@Repository
public interface AuditInstanceRepository extends JpaRepository<AuditInstance, Long> {

    /**
     * Find audit instances by entity type and entity ID
     */
    List<AuditInstance> findByEntityTypeAndEntityId(AuditEntityType entityType, Long entityId);

    /**
     * Find audit instances by flow ID
     */
    List<AuditInstance> findByFlowId(Long flowId);

    /**
     * Find audit instances by status
     */
    List<AuditInstance> findByStatus(String status);

    /**
     * Find audit instances initiated by a user
     */
    List<AuditInstance> findByInitiatedBy(Long userId);

    /**
     * Find active audit instance for an entity
     */
    @Query("SELECT a FROM AuditInstance a WHERE a.entityType = :entityType AND a.entityId = :entityId " +
           "AND a.status IN ('PENDING', 'IN_PROGRESS') ORDER BY a.initiatedAt DESC")
    Optional<AuditInstance> findActiveInstanceByEntity(@Param("entityType") AuditEntityType entityType, 
                                                        @Param("entityId") Long entityId);

    /**
     * Find latest audit instance for an entity
     */
    @Query("SELECT a FROM AuditInstance a WHERE a.entityType = :entityType AND a.entityId = :entityId " +
           "ORDER BY a.initiatedAt DESC")
    List<AuditInstance> findLatestByEntity(@Param("entityType") AuditEntityType entityType, 
                                           @Param("entityId") Long entityId);
}
