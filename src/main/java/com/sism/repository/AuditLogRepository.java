package com.sism.repository;

import com.sism.entity.AuditLog;
import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AuditLog entity
 * Provides data access methods for audit log management with Specification support for complex filtering
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Find all audit logs by entity type
     */
    List<AuditLog> findByEntityType(AuditEntityType entityType);

    /**
     * Find all audit logs by entity type with pagination
     */
    Page<AuditLog> findByEntityType(AuditEntityType entityType, Pageable pageable);

    /**
     * Find all audit logs by entity type and entity ID
     */
    List<AuditLog> findByEntityTypeAndEntityId(AuditEntityType entityType, Long entityId);

    /**
     * Find all audit logs by entity type and entity ID with pagination
     */
    Page<AuditLog> findByEntityTypeAndEntityId(AuditEntityType entityType, Long entityId, Pageable pageable);

    /**
     * Find all audit logs by entity type and entity ID ordered by creation time
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(AuditEntityType entityType, Long entityId);

    /**
     * Find all audit logs by action
     */
    List<AuditLog> findByAction(AuditAction action);

    /**
     * Find all audit logs by action with pagination
     */
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    /**
     * Find all audit logs by actor user ID
     */
    List<AuditLog> findByActorUser_Id(Long actorUserId);

    /**
     * Find all audit logs by actor user ID with pagination
     */
    Page<AuditLog> findByActorUser_Id(Long actorUserId, Pageable pageable);

    /**
     * Find all audit logs by actor organization ID
     */
    List<AuditLog> findByActorOrg_Id(Long actorOrgId);

    /**
     * Find all audit logs by actor organization ID with pagination
     */
    Page<AuditLog> findByActorOrg_Id(Long actorOrgId, Pageable pageable);

    /**
     * Find audit logs by date range
     */
    List<AuditLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find audit logs by date range with pagination
     */
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find audit logs by entity type and action
     */
    List<AuditLog> findByEntityTypeAndAction(AuditEntityType entityType, AuditAction action);

    /**
     * Find audit logs by entity type and action with pagination
     */
    Page<AuditLog> findByEntityTypeAndAction(AuditEntityType entityType, AuditAction action, Pageable pageable);

    /**
     * Count audit logs by entity type
     */
    long countByEntityType(AuditEntityType entityType);

    /**
     * Count audit logs by action
     */
    long countByAction(AuditAction action);

    /**
     * Count audit logs by actor user ID
     */
    long countByActorUser_Id(Long actorUserId);

    /**
     * Find audit logs by multiple entity types with pagination
     */
    @Query("SELECT al FROM AuditLog al WHERE al.entityType IN :entityTypes ORDER BY al.createdAt DESC")
    Page<AuditLog> findByEntityTypeIn(@Param("entityTypes") List<AuditEntityType> entityTypes, Pageable pageable);

    /**
     * Find audit logs by multiple actions with pagination
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action IN :actions ORDER BY al.createdAt DESC")
    Page<AuditLog> findByActionIn(@Param("actions") List<AuditAction> actions, Pageable pageable);

    /**
     * Find audit logs by entity type, action, and date range
     */
    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType " +
           "AND al.action = :action " +
           "AND al.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findByEntityTypeActionAndDateRange(@Param("entityType") AuditEntityType entityType,
                                                       @Param("action") AuditAction action,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find audit logs by entity type, action, and date range with pagination
     */
    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType " +
           "AND al.action = :action " +
           "AND al.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> findByEntityTypeActionAndDateRange(@Param("entityType") AuditEntityType entityType,
                                                       @Param("action") AuditAction action,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate,
                                                       Pageable pageable);

    /**
     * Find recent audit logs by actor user
     */
    @Query("SELECT al FROM AuditLog al WHERE al.actorUser.id = :actorUserId " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentByActorUser(@Param("actorUserId") Long actorUserId, Pageable pageable);

    /**
     * Find audit logs with changes (UPDATE actions only)
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action = 'UPDATE' " +
           "AND al.changedFields IS NOT NULL " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> findUpdateLogsWithChanges(Pageable pageable);

    /**
     * Search audit logs by reason keyword
     */
    @Query("SELECT al FROM AuditLog al WHERE al.reason LIKE %:keyword% ORDER BY al.createdAt DESC")
    Page<AuditLog> searchByReason(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find audit logs by entity and actor organization
     */
    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType " +
           "AND al.entityId = :entityId " +
           "AND al.actorOrg.id = :actorOrgId " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findByEntityAndActorOrg(@Param("entityType") AuditEntityType entityType,
                                           @Param("entityId") Long entityId,
                                           @Param("actorOrgId") Long actorOrgId);

    /**
     * Find audit logs for a specific entity ordered by time (for audit trail)
     */
    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType " +
           "AND al.entityId = :entityId " +
           "ORDER BY al.createdAt ASC")
    List<AuditLog> findAuditTrail(@Param("entityType") AuditEntityType entityType,
                                   @Param("entityId") Long entityId);
}
