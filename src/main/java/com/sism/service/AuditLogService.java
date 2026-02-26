package com.sism.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.common.PageResult;
import com.sism.entity.SysUser;
import com.sism.entity.AuditLog;
import com.sism.entity.SysOrg;
import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;
import com.sism.repository.AuditLogRepository;
import com.sism.vo.AuditLogVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for audit log management
 * Provides methods for recording audit logs for critical operations
 * and querying audit logs with filtering support
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log a CREATE operation
     * Requirements: 7.1 - Record create operation and complete data snapshot
     * 
     * @param entityType type of entity
     * @param entityId ID of the entity
     * @param afterData data after creation
     * @param actorUser user performing the action
     * @param actorOrg organization of the actor
     * @param reason optional reason for the action
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(AuditEntityType entityType, Long entityId, Object afterData,
                          SysUser actorUser, SysOrg actorOrg, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(AuditAction.CREATE);
        auditLog.setBeforeJson(null);
        auditLog.setAfterJson(convertToMap(afterData));
        auditLog.setChangedFields(null);
        auditLog.setActorUser(actorUser);
        auditLog.setActorOrg(actorOrg);
        auditLog.setReason(reason);
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} {}", AuditAction.CREATE, entityType, entityId);
    }

    /**
     * Log an UPDATE operation
     * Requirements: 7.2 - Record data differences before and after modification
     * 
     * @param entityType type of entity
     * @param entityId ID of the entity
     * @param beforeData data before update
     * @param afterData data after update
     * @param actorUser user performing the action
     * @param actorOrg organization of the actor
     * @param reason optional reason for the action
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(AuditEntityType entityType, Long entityId, Object beforeData, Object afterData,
                          SysUser actorUser, SysOrg actorOrg, String reason) {
        Map<String, Object> beforeMap = convertToMap(beforeData);
        Map<String, Object> afterMap = convertToMap(afterData);
        Map<String, Object> changedFields = calculateChangedFields(beforeMap, afterMap);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(AuditAction.UPDATE);
        auditLog.setBeforeJson(beforeMap);
        auditLog.setAfterJson(afterMap);
        auditLog.setChangedFields(changedFields);
        auditLog.setActorUser(actorUser);
        auditLog.setActorOrg(actorOrg);
        auditLog.setReason(reason);
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} {}", AuditAction.UPDATE, entityType, entityId);
    }

    /**
     * Log a DELETE operation
     * Requirements: 7.3 - Record delete operation and complete snapshot before deletion
     * 
     * @param entityType type of entity
     * @param entityId ID of the entity
     * @param beforeData data before deletion
     * @param actorUser user performing the action
     * @param actorOrg organization of the actor
     * @param reason optional reason for the action
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(AuditEntityType entityType, Long entityId, Object beforeData,
                          SysUser actorUser, SysOrg actorOrg, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(AuditAction.DELETE);
        auditLog.setBeforeJson(convertToMap(beforeData));
        auditLog.setAfterJson(null);
        auditLog.setChangedFields(null);
        auditLog.setActorUser(actorUser);
        auditLog.setActorOrg(actorOrg);
        auditLog.setReason(reason);
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} {}", AuditAction.DELETE, entityType, entityId);
    }

    /**
     * Log an ARCHIVE operation (soft delete)
     * 
     * @param entityType type of entity
     * @param entityId ID of the entity
     * @param beforeData data before archiving
     * @param actorUser user performing the action
     * @param actorOrg organization of the actor
     * @param reason optional reason for the action
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logArchive(AuditEntityType entityType, Long entityId, Object beforeData,
                           SysUser actorUser, SysOrg actorOrg, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(AuditAction.ARCHIVE);
        auditLog.setBeforeJson(convertToMap(beforeData));
        auditLog.setAfterJson(null);
        auditLog.setChangedFields(null);
        auditLog.setActorUser(actorUser);
        auditLog.setActorOrg(actorOrg);
        auditLog.setReason(reason);
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} {}", AuditAction.ARCHIVE, entityType, entityId);
    }

    /**
     * Log an APPROVE operation
     * Requirements: 7.4 - Record approval action, approver, and approval comments
     * 
     * @param entityType type of entity
     * @param entityId ID of the entity
     * @param beforeData data before approval
     * @param afterData data after approval
     * @param actorUser user performing the approval
     * @param actorOrg organization of the approver
     * @param approvalComments approval comments/reason
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logApprove(AuditEntityType entityType, Long entityId, Object beforeData, Object afterData,
                           SysUser actorUser, SysOrg actorOrg, String approvalComments) {
        Map<String, Object> beforeMap = convertToMap(beforeData);
        Map<String, Object> afterMap = convertToMap(afterData);
        Map<String, Object> changedFields = calculateChangedFields(beforeMap, afterMap);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(AuditAction.APPROVE);
        auditLog.setBeforeJson(beforeMap);
        auditLog.setAfterJson(afterMap);
        auditLog.setChangedFields(changedFields);
        auditLog.setActorUser(actorUser);
        auditLog.setActorOrg(actorOrg);
        auditLog.setReason(approvalComments);
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} {}", AuditAction.APPROVE, entityType, entityId);
    }

    /**
     * Log a RESTORE operation
     * 
     * @param entityType type of entity
     * @param entityId ID of the entity
     * @param afterData data after restoration
     * @param actorUser user performing the action
     * @param actorOrg organization of the actor
     * @param reason optional reason for the action
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRestore(AuditEntityType entityType, Long entityId, Object afterData,
                           SysUser actorUser, SysOrg actorOrg, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(AuditAction.RESTORE);
        auditLog.setBeforeJson(null);
        auditLog.setAfterJson(convertToMap(afterData));
        auditLog.setChangedFields(null);
        auditLog.setActorUser(actorUser);
        auditLog.setActorOrg(actorOrg);
        auditLog.setReason(reason);
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} {} {}", AuditAction.RESTORE, entityType, entityId);
    }

    // ==================== Query Methods ====================

    /**
     * Query audit logs with filtering support
     * Requirements: 7.5 - Support filtering by entity type, operation type, and time range
     * 
     * @param entityType filter by entity type (optional)
     * @param action filter by action type (optional)
     * @param startDate filter by start date (optional)
     * @param endDate filter by end date (optional)
     * @param actorUserId filter by actor user ID (optional)
     * @param actorOrgId filter by actor organization ID (optional)
     * @param page page number (0-based)
     * @param pageSize page size
     * @return paginated audit log results
     */
    @Transactional(readOnly = true)
    public PageResult<AuditLogVO> queryAuditLogs(
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long actorUserId,
            Long actorOrgId,
            int page,
            int pageSize) {
        
        Specification<AuditLog> spec = buildSpecification(entityType, action, startDate, endDate, actorUserId, actorOrgId);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<AuditLog> auditLogPage = auditLogRepository.findAll(spec, pageable);
        
        List<AuditLogVO> voList = auditLogPage.getContent().stream()
                .map(this::toAuditLogVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, auditLogPage.getTotalElements(), page, pageSize);
    }

    /**
     * Get audit logs by entity type
     * 
     * @param entityType entity type
     * @param page page number
     * @param pageSize page size
     * @return paginated audit log results
     */
    @Transactional(readOnly = true)
    public PageResult<AuditLogVO> getAuditLogsByEntityType(AuditEntityType entityType, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditLogPage = auditLogRepository.findByEntityType(entityType, pageable);
        
        List<AuditLogVO> voList = auditLogPage.getContent().stream()
                .map(this::toAuditLogVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, auditLogPage.getTotalElements(), page, pageSize);
    }

    /**
     * Get audit logs by action type
     * 
     * @param action action type
     * @param page page number
     * @param pageSize page size
     * @return paginated audit log results
     */
    @Transactional(readOnly = true)
    public PageResult<AuditLogVO> getAuditLogsByAction(AuditAction action, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditLogPage = auditLogRepository.findByAction(action, pageable);
        
        List<AuditLogVO> voList = auditLogPage.getContent().stream()
                .map(this::toAuditLogVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, auditLogPage.getTotalElements(), page, pageSize);
    }

    /**
     * Get audit logs by time range
     * 
     * @param startDate start date
     * @param endDate end date
     * @param page page number
     * @param pageSize page size
     * @return paginated audit log results
     */
    @Transactional(readOnly = true)
    public PageResult<AuditLogVO> getAuditLogsByTimeRange(LocalDateTime startDate, LocalDateTime endDate, 
                                                          int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditLogPage = auditLogRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        
        List<AuditLogVO> voList = auditLogPage.getContent().stream()
                .map(this::toAuditLogVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, auditLogPage.getTotalElements(), page, pageSize);
    }

    /**
     * Get audit trail for a specific entity
     * Returns all audit logs for an entity ordered by time (ascending)
     * 
     * @param entityType entity type
     * @param entityId entity ID
     * @return list of audit logs for the entity
     */
    @Transactional(readOnly = true)
    public List<AuditLogVO> getAuditTrail(AuditEntityType entityType, Long entityId) {
        List<AuditLog> auditLogs = auditLogRepository.findAuditTrail(entityType, entityId);
        return auditLogs.stream()
                .map(this::toAuditLogVO)
                .collect(Collectors.toList());
    }

    /**
     * Get recent audit logs by actor user
     * 
     * @param actorUserId actor user ID
     * @param limit maximum number of results
     * @return list of recent audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLogVO> getRecentAuditLogsByUser(Long actorUserId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<AuditLog> auditLogs = auditLogRepository.findRecentByActorUser(actorUserId, pageable);
        return auditLogs.stream()
                .map(this::toAuditLogVO)
                .collect(Collectors.toList());
    }

    /**
     * Search audit logs by reason keyword
     * 
     * @param keyword search keyword
     * @param page page number
     * @param pageSize page size
     * @return paginated audit log results
     */
    @Transactional(readOnly = true)
    public PageResult<AuditLogVO> searchAuditLogsByReason(String keyword, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<AuditLog> auditLogPage = auditLogRepository.searchByReason(keyword, pageable);
        
        List<AuditLogVO> voList = auditLogPage.getContent().stream()
                .map(this::toAuditLogVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, auditLogPage.getTotalElements(), page, pageSize);
    }

    // ==================== Data Difference Calculation ====================

    /**
     * Calculate and return detailed data differences between before and after states
     * Requirements: 7.2 - Compare before_json and after_json
     * 
     * @param beforeJson data before change
     * @param afterJson data after change
     * @return map of changed fields with before/after values
     */
    public Map<String, Object> calculateDataDifferences(Map<String, Object> beforeJson, Map<String, Object> afterJson) {
        return calculateChangedFields(beforeJson, afterJson);
    }

    /**
     * Get formatted data differences for display
     * 
     * @param auditLogId audit log ID
     * @return formatted differences or null if not found
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFormattedDifferences(Long auditLogId) {
        return auditLogRepository.findById(auditLogId)
                .map(auditLog -> {
                    if (auditLog.getChangedFields() != null) {
                        return auditLog.getChangedFields();
                    }
                    // Calculate on-the-fly if not stored
                    return calculateChangedFields(auditLog.getBeforeJson(), auditLog.getAfterJson());
                })
                .orElse(null);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Build JPA Specification for dynamic filtering
     */
    private Specification<AuditLog> buildSpecification(
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long actorUserId,
            Long actorOrgId) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (entityType != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityType"), entityType));
            }
            
            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            
            if (actorUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorUser").get("userId"), actorUserId));
            }
            
            if (actorOrgId != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorOrg").get("orgId"), actorOrgId));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Convert AuditLog entity to AuditLogVO
     */
    private AuditLogVO toAuditLogVO(AuditLog auditLog) {
        AuditLogVO vo = new AuditLogVO();
        vo.setLogId(auditLog.getLogId());
        vo.setEntityType(auditLog.getEntityType());
        vo.setEntityId(auditLog.getEntityId());
        vo.setAction(auditLog.getAction());
        vo.setBeforeJson(auditLog.getBeforeJson());
        vo.setAfterJson(auditLog.getAfterJson());
        vo.setChangedFields(auditLog.getChangedFields());
        vo.setReason(auditLog.getReason());
        
        if (auditLog.getActorUser() != null) {
            vo.setActorUserId(auditLog.getActorUser().getId());
            vo.setActorUserName(auditLog.getActorUser().getRealName());
        }
        
        if (auditLog.getActorOrg() != null) {
            vo.setActorOrgId(auditLog.getActorOrg().getId());
            vo.setActorOrgName(auditLog.getActorOrg().getName());
        }
        
        vo.setCreatedAt(auditLog.getCreatedAt());
        return vo;
    }

    /**
     * Convert object to Map for JSON storage
     */
    private Map<String, Object> convertToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to convert object to map: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("toString", obj.toString());
            return fallback;
        }
    }

    /**
     * Calculate changed fields between before and after states
     * Requirements: 7.2 - Record data differences before and after modification
     */
    private Map<String, Object> calculateChangedFields(Map<String, Object> before, Map<String, Object> after) {
        if (before == null || after == null) {
            return null;
        }

        Map<String, Object> changes = new HashMap<>();
        
        // Check fields in after that differ from before
        for (Map.Entry<String, Object> entry : after.entrySet()) {
            String key = entry.getKey();
            Object afterValue = entry.getValue();
            Object beforeValue = before.get(key);

            if (!objectsEqual(beforeValue, afterValue)) {
                Map<String, Object> change = new HashMap<>();
                change.put("before", beforeValue);
                change.put("after", afterValue);
                changes.put(key, change);
            }
        }
        
        // Check fields in before that are not in after (deleted fields)
        for (Map.Entry<String, Object> entry : before.entrySet()) {
            String key = entry.getKey();
            if (!after.containsKey(key)) {
                Map<String, Object> change = new HashMap<>();
                change.put("before", entry.getValue());
                change.put("after", null);
                changes.put(key, change);
            }
        }

        return changes.isEmpty() ? null : changes;
    }

    /**
     * Compare two objects for equality
     */
    private boolean objectsEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
