package com.sism.shared.domain.repository;

import com.sism.shared.domain.model.audit.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AuditLog aggregate
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    List<AuditLog> findByActorUserId(Long actorUserId);
    
    List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    Optional<AuditLog> findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}
