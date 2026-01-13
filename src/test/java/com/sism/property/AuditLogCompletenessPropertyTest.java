package com.sism.property;

import com.sism.entity.AuditLog;
import com.sism.entity.AppUser;
import com.sism.entity.Org;
import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;
import com.sism.repository.AuditLogRepository;
import com.sism.repository.OrgRepository;
import com.sism.repository.UserRepository;
import com.sism.service.AuditLogService;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Property-based tests for Audit Log Completeness
 * 
 * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
 * 
 * For any critical operation (CREATE, UPDATE, DELETE, APPROVE, ARCHIVE, RESTORE) 
 * on auditable entities, the system SHALL create an audit log entry containing: 
 * entity_type, entity_id, action, actor_user_id, actor_org_id, and timestamp. 
 * For UPDATE operations, before_json and after_json SHALL capture the complete state change.
 * 
 * **Validates: Requirements 7.1, 7.2, 7.3, 7.4**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class AuditLogCompletenessPropertyTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    // Use unique entity IDs to avoid conflicts with existing data
    // Use timestamp-based counter to ensure uniqueness across test runs
    private static final AtomicLong uniqueEntityIdCounter = new AtomicLong(
            System.currentTimeMillis() % 100000000L + 900000000L);

    // ==================== Helper Methods ====================

    private AppUser getTestUser() {
        return userRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }

    private Org getTestOrg() {
        return orgRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }

    private Long getUniqueEntityId() {
        return uniqueEntityIdCounter.incrementAndGet();
    }

    /**
     * Generate a unique reason string that can be used to identify the specific audit log
     * created in this test iteration.
     */
    private String generateUniqueReason(String baseReason) {
        return baseReason + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Find the audit log created in this test iteration by matching the unique reason.
     */
    private AuditLog findAuditLogByReason(List<AuditLog> logs, AuditAction action, String uniqueReason) {
        return logs.stream()
                .filter(log -> log.getAction() == action)
                .filter(log -> uniqueReason.equals(log.getReason()))
                .findFirst()
                .orElse(null);
    }

    // ==================== Generators ====================

    /**
     * Generator for entity types that exist in the PostgreSQL database ENUM.
     * The database has: 'ORG', 'USER', 'CYCLE', 'TASK', 'INDICATOR', 'MILESTONE', 'REPORT', 'ADHOC_TASK', 'ALERT'
     * We only use entity types that match the database ENUM values.
     */
    @Provide
    Arbitrary<AuditEntityType> entityTypes() {
        // Only use entity types that exist in the PostgreSQL database ENUM
        return Arbitraries.of(
                AuditEntityType.ORG,
                AuditEntityType.USER,
                AuditEntityType.INDICATOR,
                AuditEntityType.MILESTONE,
                AuditEntityType.ADHOC_TASK
        );
    }

    @Provide
    Arbitrary<String> reasons() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(100)
                .map(s -> "Reason_" + s);
    }

    @Provide
    Arbitrary<Map<String, Object>> entityData() {
        return Arbitraries.maps(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
                Arbitraries.oneOf(
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50).map(s -> (Object) s),
                        Arbitraries.integers().between(0, 1000).map(i -> (Object) i),
                        Arbitraries.of(true, false).map(b -> (Object) b)
                )
        ).ofMinSize(2).ofMaxSize(10);
    }

    // ==================== Property Tests ====================

    /**
     * Property 9.1: CREATE operation generates complete audit log
     * 
     * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
     * 
     * For any CREATE operation, the audit log SHALL contain:
     * - entity_type (non-null)
     * - entity_id (non-null)
     * - action = CREATE
     * - timestamp (non-null)
     * - after_json (non-null, contains created data)
     * - before_json (null, as there's no previous state)
     * 
     * **Validates: Requirements 7.1**
     */
    @Property(tries = 100)
    void createOperation_shouldGenerateCompleteAuditLog(
            @ForAll("entityTypes") AuditEntityType entityType,
            @ForAll("entityData") Map<String, Object> afterData,
            @ForAll("reasons") String reason) {

        // Get test actor
        AppUser actorUser = getTestUser();
        Org actorOrg = getTestOrg();

        // Skip if no test data exists
        assumeThat(actorUser).isNotNull();
        assumeThat(actorOrg).isNotNull();

        // Use unique entity ID to avoid conflicts
        Long entityId = getUniqueEntityId();
        
        // Generate unique reason to identify this specific audit log
        String uniqueReason = generateUniqueReason(reason);

        // Act: Log CREATE operation
        auditLogService.logCreate(entityType, entityId, afterData, actorUser, actorOrg, uniqueReason);

        // Assert: Find the created audit log by unique reason
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        assertThat(logs).isNotEmpty();

        AuditLog createLog = findAuditLogByReason(logs, AuditAction.CREATE, uniqueReason);
        assertThat(createLog).isNotNull();

        // Assert: Required fields are present
        assertThat(createLog.getEntityType()).isEqualTo(entityType);
        assertThat(createLog.getEntityId()).isEqualTo(entityId);
        assertThat(createLog.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(createLog.getCreatedAt()).isNotNull();

        // Assert: Actor information is captured
        assertThat(createLog.getActorUser()).isNotNull();
        assertThat(createLog.getActorUser().getUserId()).isEqualTo(actorUser.getUserId());
        assertThat(createLog.getActorOrg()).isNotNull();
        assertThat(createLog.getActorOrg().getOrgId()).isEqualTo(actorOrg.getOrgId());

        // Assert: CREATE has after_json but no before_json
        assertThat(createLog.getAfterJson()).isNotNull();
        assertThat(createLog.getBeforeJson()).isNull();
        assertThat(createLog.getReason()).isEqualTo(uniqueReason);
    }

    /**
     * Property 9.2: UPDATE operation generates audit log with before/after snapshots
     * 
     * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
     * 
     * For any UPDATE operation, the audit log SHALL contain:
     * - entity_type (non-null)
     * - entity_id (non-null)
     * - action = UPDATE
     * - timestamp (non-null)
     * - before_json (non-null, contains state before update)
     * - after_json (non-null, contains state after update)
     * - changed_fields (captures the differences)
     * 
     * **Validates: Requirements 7.2**
     */
    @Property(tries = 100)
    void updateOperation_shouldGenerateAuditLogWithBeforeAfterSnapshots(
            @ForAll("entityTypes") AuditEntityType entityType,
            @ForAll("entityData") Map<String, Object> beforeData,
            @ForAll("entityData") Map<String, Object> afterData,
            @ForAll("reasons") String reason) {

        // Get test actor
        AppUser actorUser = getTestUser();
        Org actorOrg = getTestOrg();

        // Skip if no test data exists
        assumeThat(actorUser).isNotNull();
        assumeThat(actorOrg).isNotNull();

        // Use unique entity ID to avoid conflicts
        Long entityId = getUniqueEntityId();
        
        // Generate unique reason to identify this specific audit log
        String uniqueReason = generateUniqueReason(reason);

        // Act: Log UPDATE operation
        auditLogService.logUpdate(entityType, entityId, beforeData, afterData, actorUser, actorOrg, uniqueReason);

        // Assert: Find the created audit log by unique reason
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        assertThat(logs).isNotEmpty();

        AuditLog updateLog = findAuditLogByReason(logs, AuditAction.UPDATE, uniqueReason);
        assertThat(updateLog).isNotNull();

        // Assert: Required fields are present
        assertThat(updateLog.getEntityType()).isEqualTo(entityType);
        assertThat(updateLog.getEntityId()).isEqualTo(entityId);
        assertThat(updateLog.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(updateLog.getCreatedAt()).isNotNull();

        // Assert: Actor information is captured
        assertThat(updateLog.getActorUser()).isNotNull();
        assertThat(updateLog.getActorUser().getUserId()).isEqualTo(actorUser.getUserId());
        assertThat(updateLog.getActorOrg()).isNotNull();
        assertThat(updateLog.getActorOrg().getOrgId()).isEqualTo(actorOrg.getOrgId());

        // Assert: UPDATE has both before_json and after_json
        assertThat(updateLog.getBeforeJson()).isNotNull();
        assertThat(updateLog.getAfterJson()).isNotNull();
        assertThat(updateLog.getReason()).isEqualTo(uniqueReason);
    }

    /**
     * Property 9.3: DELETE operation generates complete audit log
     * 
     * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
     * 
     * For any DELETE operation, the audit log SHALL contain:
     * - entity_type (non-null)
     * - entity_id (non-null)
     * - action = DELETE
     * - timestamp (non-null)
     * - before_json (non-null, contains state before deletion)
     * - after_json (null, as entity is deleted)
     * 
     * **Validates: Requirements 7.3**
     */
    @Property(tries = 100)
    void deleteOperation_shouldGenerateCompleteAuditLog(
            @ForAll("entityTypes") AuditEntityType entityType,
            @ForAll("entityData") Map<String, Object> beforeData,
            @ForAll("reasons") String reason) {

        // Get test actor
        AppUser actorUser = getTestUser();
        Org actorOrg = getTestOrg();

        // Skip if no test data exists
        assumeThat(actorUser).isNotNull();
        assumeThat(actorOrg).isNotNull();

        // Use unique entity ID to avoid conflicts
        Long entityId = getUniqueEntityId();
        
        // Generate unique reason to identify this specific audit log
        String uniqueReason = generateUniqueReason(reason);

        // Act: Log DELETE operation
        auditLogService.logDelete(entityType, entityId, beforeData, actorUser, actorOrg, uniqueReason);

        // Assert: Find the created audit log by unique reason
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        assertThat(logs).isNotEmpty();

        AuditLog deleteLog = findAuditLogByReason(logs, AuditAction.DELETE, uniqueReason);
        assertThat(deleteLog).isNotNull();

        // Assert: Required fields are present
        assertThat(deleteLog.getEntityType()).isEqualTo(entityType);
        assertThat(deleteLog.getEntityId()).isEqualTo(entityId);
        assertThat(deleteLog.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(deleteLog.getCreatedAt()).isNotNull();

        // Assert: Actor information is captured
        assertThat(deleteLog.getActorUser()).isNotNull();
        assertThat(deleteLog.getActorUser().getUserId()).isEqualTo(actorUser.getUserId());
        assertThat(deleteLog.getActorOrg()).isNotNull();
        assertThat(deleteLog.getActorOrg().getOrgId()).isEqualTo(actorOrg.getOrgId());

        // Assert: DELETE has before_json but no after_json
        assertThat(deleteLog.getBeforeJson()).isNotNull();
        assertThat(deleteLog.getAfterJson()).isNull();
        assertThat(deleteLog.getReason()).isEqualTo(uniqueReason);
    }

    /**
     * Property 9.4: APPROVE operation generates complete audit log
     * 
     * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
     * 
     * For any APPROVE operation, the audit log SHALL contain:
     * - entity_type (non-null)
     * - entity_id (non-null)
     * - action = APPROVE
     * - timestamp (non-null)
     * - before_json (non-null)
     * - after_json (non-null)
     * - reason (approval comments)
     * 
     * **Validates: Requirements 7.4**
     */
    @Property(tries = 100)
    void approveOperation_shouldGenerateCompleteAuditLog(
            @ForAll("entityTypes") AuditEntityType entityType,
            @ForAll("entityData") Map<String, Object> beforeData,
            @ForAll("entityData") Map<String, Object> afterData,
            @ForAll("reasons") String approvalComments) {

        // Get test actor
        AppUser actorUser = getTestUser();
        Org actorOrg = getTestOrg();

        // Skip if no test data exists
        assumeThat(actorUser).isNotNull();
        assumeThat(actorOrg).isNotNull();

        // Use unique entity ID to avoid conflicts
        Long entityId = getUniqueEntityId();
        
        // Generate unique reason to identify this specific audit log
        String uniqueReason = generateUniqueReason(approvalComments);

        // Act: Log APPROVE operation
        auditLogService.logApprove(entityType, entityId, beforeData, afterData, actorUser, actorOrg, uniqueReason);

        // Assert: Find the created audit log by unique reason
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        assertThat(logs).isNotEmpty();

        AuditLog approveLog = findAuditLogByReason(logs, AuditAction.APPROVE, uniqueReason);
        assertThat(approveLog).isNotNull();

        // Assert: Required fields are present
        assertThat(approveLog.getEntityType()).isEqualTo(entityType);
        assertThat(approveLog.getEntityId()).isEqualTo(entityId);
        assertThat(approveLog.getAction()).isEqualTo(AuditAction.APPROVE);
        assertThat(approveLog.getCreatedAt()).isNotNull();

        // Assert: Actor (approver) information is captured
        assertThat(approveLog.getActorUser()).isNotNull();
        assertThat(approveLog.getActorUser().getUserId()).isEqualTo(actorUser.getUserId());
        assertThat(approveLog.getActorOrg()).isNotNull();
        assertThat(approveLog.getActorOrg().getOrgId()).isEqualTo(actorOrg.getOrgId());

        // Assert: APPROVE has both before_json and after_json
        assertThat(approveLog.getBeforeJson()).isNotNull();
        assertThat(approveLog.getAfterJson()).isNotNull();
        assertThat(approveLog.getReason()).isEqualTo(uniqueReason);
    }


    /**
     * Property 9.5: ARCHIVE operation generates complete audit log
     * 
     * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
     * 
     * For any ARCHIVE (soft delete) operation, the audit log SHALL contain:
     * - entity_type (non-null)
     * - entity_id (non-null)
     * - action = ARCHIVE
     * - timestamp (non-null)
     * - before_json (non-null, contains state before archiving)
     * - after_json (null)
     * 
     * **Validates: Requirements 7.3**
     */
    @Property(tries = 100)
    void archiveOperation_shouldGenerateCompleteAuditLog(
            @ForAll("entityTypes") AuditEntityType entityType,
            @ForAll("entityData") Map<String, Object> beforeData,
            @ForAll("reasons") String reason) {

        // Get test actor
        AppUser actorUser = getTestUser();
        Org actorOrg = getTestOrg();

        // Skip if no test data exists
        assumeThat(actorUser).isNotNull();
        assumeThat(actorOrg).isNotNull();

        // Use unique entity ID to avoid conflicts
        Long entityId = getUniqueEntityId();
        
        // Generate unique reason to identify this specific audit log
        String uniqueReason = generateUniqueReason(reason);

        // Act: Log ARCHIVE operation
        auditLogService.logArchive(entityType, entityId, beforeData, actorUser, actorOrg, uniqueReason);

        // Assert: Find the created audit log by unique reason
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        assertThat(logs).isNotEmpty();

        AuditLog archiveLog = findAuditLogByReason(logs, AuditAction.ARCHIVE, uniqueReason);
        assertThat(archiveLog).isNotNull();

        // Assert: Required fields are present
        assertThat(archiveLog.getEntityType()).isEqualTo(entityType);
        assertThat(archiveLog.getEntityId()).isEqualTo(entityId);
        assertThat(archiveLog.getAction()).isEqualTo(AuditAction.ARCHIVE);
        assertThat(archiveLog.getCreatedAt()).isNotNull();

        // Assert: Actor information is captured
        assertThat(archiveLog.getActorUser()).isNotNull();
        assertThat(archiveLog.getActorUser().getUserId()).isEqualTo(actorUser.getUserId());
        assertThat(archiveLog.getActorOrg()).isNotNull();
        assertThat(archiveLog.getActorOrg().getOrgId()).isEqualTo(actorOrg.getOrgId());

        // Assert: ARCHIVE has before_json but no after_json
        assertThat(archiveLog.getBeforeJson()).isNotNull();
        assertThat(archiveLog.getAfterJson()).isNull();
        assertThat(archiveLog.getReason()).isEqualTo(uniqueReason);
    }

    /**
     * Property 9.6: RESTORE operation generates complete audit log
     * 
     * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
     * 
     * For any RESTORE operation, the audit log SHALL contain:
     * - entity_type (non-null)
     * - entity_id (non-null)
     * - action = RESTORE
     * - timestamp (non-null)
     * - before_json (null)
     * - after_json (non-null, contains restored state)
     * 
     * **Validates: Requirements 7.1**
     */
    @Property(tries = 100)
    void restoreOperation_shouldGenerateCompleteAuditLog(
            @ForAll("entityTypes") AuditEntityType entityType,
            @ForAll("entityData") Map<String, Object> afterData,
            @ForAll("reasons") String reason) {

        // Get test actor
        AppUser actorUser = getTestUser();
        Org actorOrg = getTestOrg();

        // Skip if no test data exists
        assumeThat(actorUser).isNotNull();
        assumeThat(actorOrg).isNotNull();

        // Use unique entity ID to avoid conflicts
        Long entityId = getUniqueEntityId();
        
        // Generate unique reason to identify this specific audit log
        String uniqueReason = generateUniqueReason(reason);

        // Act: Log RESTORE operation
        auditLogService.logRestore(entityType, entityId, afterData, actorUser, actorOrg, uniqueReason);

        // Assert: Find the created audit log by unique reason
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        assertThat(logs).isNotEmpty();

        AuditLog restoreLog = findAuditLogByReason(logs, AuditAction.RESTORE, uniqueReason);
        assertThat(restoreLog).isNotNull();

        // Assert: Required fields are present
        assertThat(restoreLog.getEntityType()).isEqualTo(entityType);
        assertThat(restoreLog.getEntityId()).isEqualTo(entityId);
        assertThat(restoreLog.getAction()).isEqualTo(AuditAction.RESTORE);
        assertThat(restoreLog.getCreatedAt()).isNotNull();

        // Assert: Actor information is captured
        assertThat(restoreLog.getActorUser()).isNotNull();
        assertThat(restoreLog.getActorUser().getUserId()).isEqualTo(actorUser.getUserId());
        assertThat(restoreLog.getActorOrg()).isNotNull();
        assertThat(restoreLog.getActorOrg().getOrgId()).isEqualTo(actorOrg.getOrgId());

        // Assert: RESTORE has after_json but no before_json
        assertThat(restoreLog.getBeforeJson()).isNull();
        assertThat(restoreLog.getAfterJson()).isNotNull();
        assertThat(restoreLog.getReason()).isEqualTo(uniqueReason);
    }

    /**
     * Property 9.7: UPDATE operation correctly calculates changed fields
     * 
     * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
     * 
     * For any UPDATE operation where before and after data differ,
     * the changed_fields SHALL correctly identify which fields changed.
     * 
     * **Validates: Requirements 7.2**
     */
    @Property(tries = 100)
    void updateOperation_shouldCorrectlyCalculateChangedFields(
            @ForAll("entityTypes") AuditEntityType entityType,
            @ForAll("reasons") String reason) {

        // Get test actor
        AppUser actorUser = getTestUser();
        Org actorOrg = getTestOrg();

        // Skip if no test data exists
        assumeThat(actorUser).isNotNull();
        assumeThat(actorOrg).isNotNull();

        // Use unique entity ID to avoid conflicts
        Long entityId = getUniqueEntityId();
        
        // Generate unique reason to identify this specific audit log
        String uniqueReason = generateUniqueReason(reason);

        // Create before and after data with known differences
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("name", "Original Name");
        beforeData.put("status", "ACTIVE");
        beforeData.put("count", 10);
        beforeData.put("unchanged", "same value");

        Map<String, Object> afterData = new HashMap<>();
        afterData.put("name", "Updated Name");
        afterData.put("status", "INACTIVE");
        afterData.put("count", 20);
        afterData.put("unchanged", "same value");

        // Act: Log UPDATE operation
        auditLogService.logUpdate(entityType, entityId, beforeData, afterData, actorUser, actorOrg, uniqueReason);

        // Assert: Find the created audit log by unique reason
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        AuditLog updateLog = findAuditLogByReason(logs, AuditAction.UPDATE, uniqueReason);

        assertThat(updateLog).isNotNull();
        assertThat(updateLog.getChangedFields()).isNotNull();

        // Assert: Changed fields are correctly identified
        Map<String, Object> changedFields = updateLog.getChangedFields();
        
        // name, status, count should be in changed fields
        assertThat(changedFields).containsKey("name");
        assertThat(changedFields).containsKey("status");
        assertThat(changedFields).containsKey("count");
        
        // unchanged should NOT be in changed fields
        assertThat(changedFields).doesNotContainKey("unchanged");
    }

    /**
     * Property 9.8: Audit logs preserve actor information when provided
     * 
     * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
     * 
     * For any audit operation with actor information provided,
     * the audit log SHALL correctly capture the actor user and organization.
     * 
     * **Validates: Requirements 7.1, 7.2, 7.3, 7.4**
     */
    @Property(tries = 100)
    void auditLogs_shouldPreserveActorInformation(
            @ForAll("entityTypes") AuditEntityType entityType,
            @ForAll("entityData") Map<String, Object> data,
            @ForAll("reasons") String reason) {

        // Get test actor
        AppUser actorUser = getTestUser();
        Org actorOrg = getTestOrg();

        // Skip if no test data exists
        assumeThat(actorUser).isNotNull();
        assumeThat(actorOrg).isNotNull();

        // Use unique entity ID to avoid conflicts
        Long entityId = getUniqueEntityId();
        
        // Generate unique reason to identify this specific audit log
        String uniqueReason = generateUniqueReason(reason);

        // Act: Log CREATE operation with actor
        auditLogService.logCreate(entityType, entityId, data, actorUser, actorOrg, uniqueReason);

        // Assert: Find the created audit log by unique reason
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        assertThat(logs).isNotEmpty();

        AuditLog createLog = findAuditLogByReason(logs, AuditAction.CREATE, uniqueReason);
        assertThat(createLog).isNotNull();

        // Assert: Actor information is correctly preserved
        assertThat(createLog.getActorUser()).isNotNull();
        assertThat(createLog.getActorUser().getUserId()).isEqualTo(actorUser.getUserId());
        assertThat(createLog.getActorOrg()).isNotNull();
        assertThat(createLog.getActorOrg().getOrgId()).isEqualTo(actorOrg.getOrgId());
    }

    /**
     * Property 9.9: All audit actions generate logs with valid timestamps
     * 
     * **Feature: sism-fullstack-integration, Property 9: Audit Log Completeness**
     * 
     * For any audit action, the created_at timestamp SHALL be non-null.
     * 
     * **Validates: Requirements 7.1, 7.2, 7.3, 7.4**
     */
    @Property(tries = 100)
    void allAuditActions_shouldHaveNonNullTimestamps(
            @ForAll("entityTypes") AuditEntityType entityType,
            @ForAll("entityData") Map<String, Object> data,
            @ForAll("reasons") String reason) {

        // Get test actor
        AppUser actorUser = getTestUser();
        Org actorOrg = getTestOrg();

        // Skip if no test data exists
        assumeThat(actorUser).isNotNull();
        assumeThat(actorOrg).isNotNull();

        // Use unique entity IDs for each action type
        Long createEntityId = getUniqueEntityId();
        Long updateEntityId = getUniqueEntityId();
        Long deleteEntityId = getUniqueEntityId();
        Long approveEntityId = getUniqueEntityId();
        Long archiveEntityId = getUniqueEntityId();
        Long restoreEntityId = getUniqueEntityId();
        
        // Generate unique reasons for each action
        String createReason = generateUniqueReason(reason + "_CREATE");
        String updateReason = generateUniqueReason(reason + "_UPDATE");
        String deleteReason = generateUniqueReason(reason + "_DELETE");
        String approveReason = generateUniqueReason(reason + "_APPROVE");
        String archiveReason = generateUniqueReason(reason + "_ARCHIVE");
        String restoreReason = generateUniqueReason(reason + "_RESTORE");

        // Create logs for all action types with different entity IDs
        auditLogService.logCreate(entityType, createEntityId, data, actorUser, actorOrg, createReason);
        auditLogService.logUpdate(entityType, updateEntityId, data, data, actorUser, actorOrg, updateReason);
        auditLogService.logDelete(entityType, deleteEntityId, data, actorUser, actorOrg, deleteReason);
        auditLogService.logApprove(entityType, approveEntityId, data, data, actorUser, actorOrg, approveReason);
        auditLogService.logArchive(entityType, archiveEntityId, data, actorUser, actorOrg, archiveReason);
        auditLogService.logRestore(entityType, restoreEntityId, data, actorUser, actorOrg, restoreReason);

        // Assert: All logs have non-null timestamps (find by unique reason)
        AuditLog createLog = findAuditLogByReason(
                auditLogRepository.findByEntityTypeAndEntityId(entityType, createEntityId), 
                AuditAction.CREATE, createReason);
        assertThat(createLog).isNotNull();
        assertThat(createLog.getCreatedAt()).isNotNull();
        
        AuditLog updateLog = findAuditLogByReason(
                auditLogRepository.findByEntityTypeAndEntityId(entityType, updateEntityId), 
                AuditAction.UPDATE, updateReason);
        assertThat(updateLog).isNotNull();
        assertThat(updateLog.getCreatedAt()).isNotNull();
        
        AuditLog deleteLog = findAuditLogByReason(
                auditLogRepository.findByEntityTypeAndEntityId(entityType, deleteEntityId), 
                AuditAction.DELETE, deleteReason);
        assertThat(deleteLog).isNotNull();
        assertThat(deleteLog.getCreatedAt()).isNotNull();
        
        AuditLog approveLog = findAuditLogByReason(
                auditLogRepository.findByEntityTypeAndEntityId(entityType, approveEntityId), 
                AuditAction.APPROVE, approveReason);
        assertThat(approveLog).isNotNull();
        assertThat(approveLog.getCreatedAt()).isNotNull();
        
        AuditLog archiveLog = findAuditLogByReason(
                auditLogRepository.findByEntityTypeAndEntityId(entityType, archiveEntityId), 
                AuditAction.ARCHIVE, archiveReason);
        assertThat(archiveLog).isNotNull();
        assertThat(archiveLog.getCreatedAt()).isNotNull();
        
        AuditLog restoreLog = findAuditLogByReason(
                auditLogRepository.findByEntityTypeAndEntityId(entityType, restoreEntityId), 
                AuditAction.RESTORE, restoreReason);
        assertThat(restoreLog).isNotNull();
        assertThat(restoreLog.getCreatedAt()).isNotNull();
    }
}
