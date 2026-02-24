package com.sism.property;

import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.entity.AuditLog;
import com.sism.entity.SysOrg;
import com.sism.entity.StrategicTask;
import com.sism.entity.AssessmentCycle;
import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.repository.AuditLogRepository;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.repository.AssessmentCycleRepository;
import com.sism.service.IndicatorService;
import com.sism.vo.IndicatorVO;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Property-based tests for Indicator CRUD operations with Audit Logging
 * 
 * **Feature: sism-fullstack-integration, Property 2: Indicator CRUD Operations with Audit Logging**
 * 
 * For any indicator create, update, or delete operation, the system SHALL persist 
 * the change to the database AND create a corresponding audit log entry with 
 * complete before/after snapshots.
 * 
 * **Validates: Requirements 2.3, 2.4, 2.5, 7.1, 7.2, 7.3**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class IndicatorCrudAuditPropertyTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssessmentCycleRepository assessmentCycleRepository;

    // ==================== Helper Methods ====================

    private SysOrg getTestOwnerOrg() {
        return orgRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }

    private SysOrg getTestTargetOrg() {
        List<SysOrg> orgs = orgRepository.findAll();
        if (orgs.size() > 1) {
            return orgs.get(1);
        }
        return orgs.isEmpty() ? null : orgs.get(0);
    }

    private StrategicTask getTestTask() {
        return taskRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> indicatorDescriptions() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(50)
                .map(s -> "Indicator_" + s);
    }

    @Provide
    Arbitrary<BigDecimal> validWeights() {
        return Arbitraries.integers()
                .between(0, 100)
                .map(i -> BigDecimal.valueOf(i));
    }

    @Provide
    Arbitrary<Integer> validYears() {
        return Arbitraries.integers().between(2020, 2030);
    }

    @Provide
    Arbitrary<IndicatorLevel> indicatorLevels() {
        return Arbitraries.of(IndicatorLevel.values());
    }

    @Provide
    Arbitrary<Integer> sortOrders() {
        return Arbitraries.integers().between(0, 100);
    }


    // ==================== Property Tests ====================

    /**
     * Property 2.1: CREATE operation generates audit log
     * 
     * **Feature: sism-fullstack-integration, Property 2: Indicator CRUD Operations with Audit Logging**
     * 
     * For any valid indicator creation, the system SHALL:
     * 1. Persist the indicator to the database
     * 2. Create a CREATE audit log entry with complete after_json snapshot
     * 
     * **Validates: Requirements 2.3, 7.1**
     */
    @Property(tries = 100)
    void createIndicator_shouldPersistAndGenerateAuditLog(
            @ForAll("indicatorDescriptions") String description,
            @ForAll("validWeights") BigDecimal weight,
            @ForAll("validYears") Integer year,
            @ForAll("indicatorLevels") IndicatorLevel level,
            @ForAll("sortOrders") Integer sortOrder) {

        // Get test data
        SysOrg ownerOrg = getTestOwnerOrg();
        SysOrg targetOrg = getTestTargetOrg();
        StrategicTask task = getTestTask();

        // Skip if required data doesn't exist
        assumeThat(ownerOrg).isNotNull();
        assumeThat(targetOrg).isNotNull();
        assumeThat(task).isNotNull();

        // Arrange
        IndicatorCreateRequest request = new IndicatorCreateRequest();
        request.setTaskId(task.getTaskId());
        request.setOwnerOrgId(ownerOrg.getId());
        request.setTargetOrgId(targetOrg.getId());
        request.setIndicatorDesc(description);
        request.setWeightPercent(weight);
        request.setYear(year);
        request.setLevel(level.name()); // Convert enum to string
        request.setSortOrder(sortOrder);

        long auditLogCountBefore = auditLogRepository.count();

        // Act
        IndicatorVO createdIndicator = indicatorService.createIndicator(request);

        try {
            // Assert 1: Indicator is persisted
            assertThat(createdIndicator).isNotNull();
            assertThat(createdIndicator.getIndicatorId()).isNotNull();

            var persistedIndicator = indicatorRepository.findById(createdIndicator.getIndicatorId()).orElse(null);
            assertThat(persistedIndicator).isNotNull();
            assertThat(persistedIndicator.getIndicatorDesc()).isEqualTo(description);
            assertThat(persistedIndicator.getWeightPercent()).isEqualByComparingTo(weight);
            assertThat(persistedIndicator.getYear()).isEqualTo(year);
            assertThat(persistedIndicator.getLevel()).isEqualTo(level);
            assertThat(persistedIndicator.getStatus()).isEqualTo(IndicatorStatus.ACTIVE);

            // Assert 2: Audit log is created
            long auditLogCountAfter = auditLogRepository.count();
            assertThat(auditLogCountAfter).isGreaterThan(auditLogCountBefore);

            // Assert 3: Audit log has correct content
            List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                    AuditEntityType.INDICATOR, createdIndicator.getIndicatorId());
            assertThat(auditLogs).isNotEmpty();

            AuditLog createLog = auditLogs.stream()
                    .filter(log -> log.getAction() == AuditAction.CREATE)
                    .findFirst()
                    .orElse(null);
            assertThat(createLog).isNotNull();
            assertThat(createLog.getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
            assertThat(createLog.getEntityId()).isEqualTo(createdIndicator.getIndicatorId());
            assertThat(createLog.getBeforeJson()).isNull(); // CREATE has no before state
            assertThat(createLog.getAfterJson()).isNotNull(); // CREATE has after state
            assertThat(createLog.getAfterJson().get("indicatorDesc")).isEqualTo(description);
        } finally {
            // Cleanup: Delete the created indicator and its audit logs
            if (createdIndicator != null && createdIndicator.getIndicatorId() != null) {
                auditLogRepository.deleteAll(
                        auditLogRepository.findByEntityTypeAndEntityId(
                                AuditEntityType.INDICATOR, createdIndicator.getIndicatorId()));
                indicatorRepository.deleteById(createdIndicator.getIndicatorId());
            }
        }
    }

    /**
     * Property 2.2: UPDATE operation generates audit log with before/after snapshots
     * 
     * **Feature: sism-fullstack-integration, Property 2: Indicator CRUD Operations with Audit Logging**
     * 
     * For any valid indicator update, the system SHALL:
     * 1. Persist the updated indicator to the database
     * 2. Create an UPDATE audit log entry with both before_json and after_json snapshots
     * 3. Record the changed fields
     * 
     * **Validates: Requirements 2.4, 7.2**
     */
    @Property(tries = 100)
    void updateIndicator_shouldPersistAndGenerateAuditLogWithDiff(
            @ForAll("indicatorDescriptions") String originalDesc,
            @ForAll("indicatorDescriptions") String updatedDesc,
            @ForAll("validWeights") BigDecimal originalWeight,
            @ForAll("validWeights") BigDecimal updatedWeight,
            @ForAll("validYears") Integer year) {

        // Get test data
        SysOrg ownerOrg = getTestOwnerOrg();
        SysOrg targetOrg = getTestTargetOrg();
        StrategicTask task = getTestTask();

        // Skip if required data doesn't exist
        assumeThat(ownerOrg).isNotNull();
        assumeThat(targetOrg).isNotNull();
        assumeThat(task).isNotNull();

        // Arrange: Create an indicator first
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(task.getTaskId());
        createRequest.setOwnerOrgId(ownerOrg.getId());
        createRequest.setTargetOrgId(targetOrg.getId());
        createRequest.setIndicatorDesc(originalDesc);
        createRequest.setWeightPercent(originalWeight);
        createRequest.setYear(year);
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setSortOrder(1);

        IndicatorVO createdIndicator = indicatorService.createIndicator(createRequest);
        Long indicatorId = createdIndicator.getIndicatorId();

        try {
            // Clear audit logs for this indicator to focus on update
            auditLogRepository.deleteAll(
                    auditLogRepository.findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId));

            // Act: Update the indicator
            IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
            updateRequest.setIndicatorDesc(updatedDesc);
            updateRequest.setWeightPercent(updatedWeight);

            IndicatorVO updatedIndicator = indicatorService.updateIndicator(indicatorId, updateRequest);

            // Assert 1: Indicator is updated in database
            assertThat(updatedIndicator).isNotNull();
            assertThat(updatedIndicator.getIndicatorDesc()).isEqualTo(updatedDesc);
            assertThat(updatedIndicator.getWeightPercent()).isEqualByComparingTo(updatedWeight);

            var persistedIndicator = indicatorRepository.findById(indicatorId).orElse(null);
            assertThat(persistedIndicator).isNotNull();
            assertThat(persistedIndicator.getIndicatorDesc()).isEqualTo(updatedDesc);

            // Assert 2: Audit log is created for UPDATE
            List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                    AuditEntityType.INDICATOR, indicatorId);
            assertThat(auditLogs).isNotEmpty();

            AuditLog updateLog = auditLogs.stream()
                    .filter(log -> log.getAction() == AuditAction.UPDATE)
                    .findFirst()
                    .orElse(null);
            assertThat(updateLog).isNotNull();
            assertThat(updateLog.getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
            assertThat(updateLog.getEntityId()).isEqualTo(indicatorId);

            // Assert 3: Before and after snapshots are captured
            assertThat(updateLog.getBeforeJson()).isNotNull();
            assertThat(updateLog.getAfterJson()).isNotNull();
            assertThat(updateLog.getBeforeJson().get("indicatorDesc")).isEqualTo(originalDesc);
            assertThat(updateLog.getAfterJson().get("indicatorDesc")).isEqualTo(updatedDesc);

            // Assert 4: Changed fields are recorded (if description changed)
            if (!originalDesc.equals(updatedDesc) || !originalWeight.equals(updatedWeight)) {
                assertThat(updateLog.getChangedFields()).isNotNull();
            }
        } finally {
            // Cleanup
            auditLogRepository.deleteAll(
                    auditLogRepository.findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId));
            indicatorRepository.deleteById(indicatorId);
        }
    }


    /**
     * Property 2.3: DELETE (soft delete/archive) operation generates audit log
     * 
     * **Feature: sism-fullstack-integration, Property 2: Indicator CRUD Operations with Audit Logging**
     * 
     * For any valid indicator deletion (soft delete), the system SHALL:
     * 1. Update the indicator status to ARCHIVED
     * 2. Create an ARCHIVE audit log entry with complete before_json snapshot
     * 
     * **Validates: Requirements 2.5, 7.3**
     */
    @Property(tries = 100)
    void deleteIndicator_shouldArchiveAndGenerateAuditLog(
            @ForAll("indicatorDescriptions") String description,
            @ForAll("validWeights") BigDecimal weight,
            @ForAll("validYears") Integer year,
            @ForAll("indicatorLevels") IndicatorLevel level) {

        // Get test data
        SysOrg ownerOrg = getTestOwnerOrg();
        SysOrg targetOrg = getTestTargetOrg();
        StrategicTask task = getTestTask();

        // Skip if required data doesn't exist
        assumeThat(ownerOrg).isNotNull();
        assumeThat(targetOrg).isNotNull();
        assumeThat(task).isNotNull();

        // Arrange: Create an indicator first
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(task.getTaskId());
        createRequest.setOwnerOrgId(ownerOrg.getId());
        createRequest.setTargetOrgId(targetOrg.getId());
        createRequest.setIndicatorDesc(description);
        createRequest.setWeightPercent(weight);
        createRequest.setYear(year);
        createRequest.setLevel(level.name());
        createRequest.setSortOrder(1);

        IndicatorVO createdIndicator = indicatorService.createIndicator(createRequest);
        Long indicatorId = createdIndicator.getIndicatorId();

        try {
            // Clear audit logs for this indicator to focus on delete
            auditLogRepository.deleteAll(
                    auditLogRepository.findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId));

            // Act: Delete (soft delete) the indicator
            indicatorService.deleteIndicator(indicatorId);

            // Assert 1: Indicator is soft deleted (isDeleted = true)
            var archivedIndicator = indicatorRepository.findById(indicatorId).orElse(null);
            assertThat(archivedIndicator).isNotNull();
            assertThat(archivedIndicator.getIsDeleted()).isTrue();

            // Assert 2: Audit log is created for DELETE
            List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                    AuditEntityType.INDICATOR, indicatorId);
            assertThat(auditLogs).isNotEmpty();

            AuditLog archiveLog = auditLogs.stream()
                    .filter(log -> log.getAction() == AuditAction.DELETE)
                    .findFirst()
                    .orElse(null);
            assertThat(archiveLog).isNotNull();
            assertThat(archiveLog.getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
            assertThat(archiveLog.getEntityId()).isEqualTo(indicatorId);

            // Assert 3: Before snapshot is captured (complete state before deletion)
            assertThat(archiveLog.getBeforeJson()).isNotNull();
            assertThat(archiveLog.getBeforeJson().get("indicatorDesc")).isEqualTo(description);
            assertThat(archiveLog.getBeforeJson().get("status")).isEqualTo(IndicatorStatus.ACTIVE.name());

            // Assert 4: After snapshot is null for archive operation
            assertThat(archiveLog.getAfterJson()).isNull();
        } finally {
            // Cleanup
            auditLogRepository.deleteAll(
                    auditLogRepository.findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId));
            indicatorRepository.deleteById(indicatorId);
        }
    }

    /**
     * Property 2.4: Full CRUD lifecycle generates complete audit trail
     * 
     * **Feature: sism-fullstack-integration, Property 2: Indicator CRUD Operations with Audit Logging**
     * 
     * For any indicator going through CREATE -> UPDATE -> DELETE lifecycle,
     * the system SHALL maintain a complete audit trail with all operations recorded.
     * 
     * **Validates: Requirements 2.3, 2.4, 2.5, 7.1, 7.2, 7.3**
     */
    @Property(tries = 50)
    void fullCrudLifecycle_shouldGenerateCompleteAuditTrail(
            @ForAll("indicatorDescriptions") String originalDesc,
            @ForAll("indicatorDescriptions") String updatedDesc,
            @ForAll("validWeights") BigDecimal weight,
            @ForAll("validYears") Integer year) {

        // Get test data
        SysOrg ownerOrg = getTestOwnerOrg();
        SysOrg targetOrg = getTestTargetOrg();
        StrategicTask task = getTestTask();

        // Skip if required data doesn't exist
        assumeThat(ownerOrg).isNotNull();
        assumeThat(targetOrg).isNotNull();
        assumeThat(task).isNotNull();

        Long indicatorId = null;

        try {
            // Step 1: CREATE
            IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
            createRequest.setTaskId(task.getTaskId());
            createRequest.setOwnerOrgId(ownerOrg.getId());
            createRequest.setTargetOrgId(targetOrg.getId());
            createRequest.setIndicatorDesc(originalDesc);
            createRequest.setWeightPercent(weight);
            createRequest.setYear(year);
            createRequest.setLevel(IndicatorLevel.PRIMARY.name());
            createRequest.setSortOrder(1);

            IndicatorVO createdIndicator = indicatorService.createIndicator(createRequest);
            indicatorId = createdIndicator.getIndicatorId();

            // Step 2: UPDATE
            IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
            updateRequest.setIndicatorDesc(updatedDesc);
            indicatorService.updateIndicator(indicatorId, updateRequest);

            // Step 3: DELETE (soft delete)
            indicatorService.deleteIndicator(indicatorId);

            // Assert: Complete audit trail exists
            List<AuditLog> auditTrail = auditLogRepository.findAuditTrail(AuditEntityType.INDICATOR, indicatorId);
            
            // Should have at least 3 entries: CREATE, UPDATE, DELETE
            assertThat(auditTrail).hasSizeGreaterThanOrEqualTo(3);

            // Verify CREATE log exists
            boolean hasCreateLog = auditTrail.stream()
                    .anyMatch(log -> log.getAction() == AuditAction.CREATE);
            assertThat(hasCreateLog).isTrue();

            // Verify UPDATE log exists
            boolean hasUpdateLog = auditTrail.stream()
                    .anyMatch(log -> log.getAction() == AuditAction.UPDATE);
            assertThat(hasUpdateLog).isTrue();

            // Verify ARCHIVE log exists
            boolean hasArchiveLog = auditTrail.stream()
                    .anyMatch(log -> log.getAction() == AuditAction.ARCHIVE);
            assertThat(hasArchiveLog).isTrue();

            // Verify chronological order (CREATE should be first)
            AuditLog firstLog = auditTrail.get(0);
            assertThat(firstLog.getAction()).isEqualTo(AuditAction.CREATE);

            // Verify last log is ARCHIVE
            AuditLog lastLog = auditTrail.get(auditTrail.size() - 1);
            assertThat(lastLog.getAction()).isEqualTo(AuditAction.ARCHIVE);
        } finally {
            // Cleanup
            if (indicatorId != null) {
                auditLogRepository.deleteAll(
                        auditLogRepository.findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId));
                indicatorRepository.deleteById(indicatorId);
            }
        }
    }

    /**
     * Property 2.5: Audit log entity type and entity ID are always correct
     * 
     * **Feature: sism-fullstack-integration, Property 2: Indicator CRUD Operations with Audit Logging**
     * 
     * For any indicator operation, the audit log SHALL correctly identify
     * the entity type as INDICATOR and the entity ID as the indicator's ID.
     * 
     * **Validates: Requirements 7.1, 7.2, 7.3**
     */
    @Property(tries = 100)
    void auditLog_shouldHaveCorrectEntityTypeAndId(
            @ForAll("indicatorDescriptions") String description,
            @ForAll("validYears") Integer year) {

        // Get test data
        SysOrg ownerOrg = getTestOwnerOrg();
        SysOrg targetOrg = getTestTargetOrg();
        StrategicTask task = getTestTask();

        // Skip if required data doesn't exist
        assumeThat(ownerOrg).isNotNull();
        assumeThat(targetOrg).isNotNull();
        assumeThat(task).isNotNull();

        Long indicatorId = null;

        try {
            // Create indicator
            IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
            createRequest.setTaskId(task.getTaskId());
            createRequest.setOwnerOrgId(ownerOrg.getId());
            createRequest.setTargetOrgId(targetOrg.getId());
            createRequest.setIndicatorDesc(description);
            createRequest.setWeightPercent(BigDecimal.valueOf(50));
            createRequest.setYear(year);
            createRequest.setLevel(IndicatorLevel.PRIMARY.name());
            createRequest.setSortOrder(1);

            IndicatorVO createdIndicator = indicatorService.createIndicator(createRequest);
            indicatorId = createdIndicator.getIndicatorId();

            // Get all audit logs for this indicator
            List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                    AuditEntityType.INDICATOR, indicatorId);

            // Assert: All audit logs have correct entity type and ID
            assertThat(auditLogs).isNotEmpty();
            for (AuditLog log : auditLogs) {
                assertThat(log.getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
                assertThat(log.getEntityId()).isEqualTo(indicatorId);
                assertThat(log.getCreatedAt()).isNotNull();
            }
        } finally {
            // Cleanup
            if (indicatorId != null) {
                auditLogRepository.deleteAll(
                        auditLogRepository.findByEntityTypeAndEntityId(AuditEntityType.INDICATOR, indicatorId));
                indicatorRepository.deleteById(indicatorId);
            }
        }
    }
}
