package com.sism.property;

import com.sism.entity.Indicator;
import com.sism.enums.IndicatorStatus;
import com.sism.repository.IndicatorRepository;
import com.sism.service.IndicatorService;
import com.sism.vo.IndicatorVO;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Property-based tests for Soft Deletion Behavior
 * 
 * **Feature: sism-fullstack-integration, Property 17: Soft Deletion Behavior**
 * 
 * For any soft-deletable entity, calling the delete operation SHALL set status 
 * to ARCHIVED rather than physically deleting the record. Subsequent queries 
 * SHALL exclude ARCHIVED records by default unless explicitly requested.
 * 
 * **Validates: Requirements 12.5, 2.5**
 * 
 * Note: These tests use existing data from the database and filter in Java code
 * to avoid PostgreSQL native ENUM type issues with JPA queries.
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class SoftDeletionBehaviorPropertyTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    // ==================== Helper Methods ====================

    /**
     * Get existing ACTIVE indicators from the database for testing.
     * Uses findAll() and filters in Java to avoid PostgreSQL enum type issues.
     */
    private List<Indicator> getExistingActiveIndicators(int limit) {
        return indicatorRepository.findAll().stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .limit(limit)
                .toList();
    }

    /**
     * Get all indicators and filter by status in Java code.
     * Avoids PostgreSQL enum type issues with JPA queries.
     */
    private List<Indicator> getIndicatorsByStatusInJava(IndicatorStatus status) {
        return indicatorRepository.findAll().stream()
                .filter(i -> i.getStatus() == status)
                .toList();
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Integer> indicatorIndices() {
        return Arbitraries.integers().between(0, 9);
    }

    // ==================== Property Tests ====================

    /**
     * Property 17.1: Soft delete sets status to ARCHIVED instead of physical deletion
     * 
     * **Feature: sism-fullstack-integration, Property 17: Soft Deletion Behavior**
     * 
     * For any indicator, calling deleteIndicator SHALL:
     * 1. NOT physically remove the record from the database
     * 2. Set the status field to ARCHIVED
     * 
     * **Validates: Requirements 12.5, 2.5**
     */
    @Property(tries = 100)
    @Transactional
    void softDelete_shouldSetStatusToArchivedNotPhysicallyDelete(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();
        
        // Capture original data for verification
        String originalDesc = indicator.getIndicatorDesc();
        Integer originalYear = indicator.getYear();

        // Verify indicator is ACTIVE before deletion
        assertThat(indicator.getStatus()).isEqualTo(IndicatorStatus.ACTIVE);

        // Act: Delete the indicator (soft delete)
        indicatorService.deleteIndicator(indicatorId, null, "Property test soft delete");

        // Assert 1: Record still exists in database (NOT physically deleted)
        Optional<Indicator> afterDeleteOpt = indicatorRepository.findById(indicatorId);
        assertThat(afterDeleteOpt).isPresent();
        
        Indicator afterDelete = afterDeleteOpt.get();

        // Assert 2: Status is changed to ARCHIVED
        assertThat(afterDelete.getStatus()).isEqualTo(IndicatorStatus.ARCHIVED);

        // Assert 3: All other data remains unchanged
        assertThat(afterDelete.getIndicatorDesc()).isEqualTo(originalDesc);
        assertThat(afterDelete.getYear()).isEqualTo(originalYear);

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 17.2: Default queries exclude ARCHIVED records
     * 
     * **Feature: sism-fullstack-integration, Property 17: Soft Deletion Behavior**
     * 
     * For any soft-deleted indicator, subsequent queries using getAllActiveIndicators
     * or getIndicatorsByTaskId SHALL NOT include the archived record.
     * 
     * **Validates: Requirements 12.5, 2.5**
     */
    @Property(tries = 100)
    @Transactional
    void defaultQueries_shouldExcludeArchivedRecords(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();
        Long taskId = indicator.getTask().getTaskId();

        // Verify indicator appears in active queries before deletion
        List<IndicatorVO> activeIndicatorsBefore = indicatorService.getAllActiveIndicators();
        boolean foundBeforeDelete = activeIndicatorsBefore.stream()
                .anyMatch(i -> i.getIndicatorId().equals(indicatorId));
        assertThat(foundBeforeDelete).isTrue();

        List<IndicatorVO> taskIndicatorsBefore = indicatorService.getIndicatorsByTaskId(taskId);
        boolean foundInTaskBefore = taskIndicatorsBefore.stream()
                .anyMatch(i -> i.getIndicatorId().equals(indicatorId));
        assertThat(foundInTaskBefore).isTrue();

        // Act: Soft delete the indicator
        indicatorService.deleteIndicator(indicatorId, null, "Query exclusion test delete");

        // Assert 1: Archived indicator is excluded from getAllActiveIndicators
        List<IndicatorVO> activeIndicatorsAfter = indicatorService.getAllActiveIndicators();
        boolean foundAfterDelete = activeIndicatorsAfter.stream()
                .anyMatch(i -> i.getIndicatorId().equals(indicatorId));
        assertThat(foundAfterDelete).isFalse();

        // Assert 2: Archived indicator is excluded from getIndicatorsByTaskId
        List<IndicatorVO> taskIndicatorsAfter = indicatorService.getIndicatorsByTaskId(taskId);
        boolean foundInTaskAfter = taskIndicatorsAfter.stream()
                .anyMatch(i -> i.getIndicatorId().equals(indicatorId));
        assertThat(foundInTaskAfter).isFalse();

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 17.3: Direct repository query can still find ARCHIVED records
     * 
     * **Feature: sism-fullstack-integration, Property 17: Soft Deletion Behavior**
     * 
     * For any soft-deleted indicator, direct repository queries (findById)
     * SHALL still be able to retrieve the archived record when explicitly requested.
     * 
     * **Validates: Requirements 12.5**
     */
    @Property(tries = 100)
    @Transactional
    void explicitQueries_shouldFindArchivedRecords(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // Soft delete
        indicatorService.deleteIndicator(indicatorId, null, "Explicit query test delete");

        // Assert 1: findById can still retrieve the archived record
        Optional<Indicator> archivedIndicatorOpt = indicatorRepository.findById(indicatorId);
        assertThat(archivedIndicatorOpt).isPresent();
        assertThat(archivedIndicatorOpt.get().getStatus()).isEqualTo(IndicatorStatus.ARCHIVED);

        // Assert 2: Filter by ARCHIVED status in Java includes the record
        List<Indicator> archivedIndicators = getIndicatorsByStatusInJava(IndicatorStatus.ARCHIVED);
        boolean foundInArchived = archivedIndicators.stream()
                .anyMatch(i -> i.getIndicatorId().equals(indicatorId));
        assertThat(foundInArchived).isTrue();

        // Assert 3: Filter by ACTIVE status in Java excludes the record
        List<Indicator> activeIndicatorsAfter = getIndicatorsByStatusInJava(IndicatorStatus.ACTIVE);
        boolean foundInActive = activeIndicatorsAfter.stream()
                .anyMatch(i -> i.getIndicatorId().equals(indicatorId));
        assertThat(foundInActive).isFalse();

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 17.4: Soft deleted records preserve all original data
     * 
     * **Feature: sism-fullstack-integration, Property 17: Soft Deletion Behavior**
     * 
     * For any soft-deleted indicator, all original data fields SHALL be preserved
     * unchanged except for the status field which becomes ARCHIVED.
     * 
     * **Validates: Requirements 12.5, 2.5**
     */
    @Property(tries = 100)
    @Transactional
    void softDelete_shouldPreserveAllOriginalData(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // Capture original state
        Long originalTaskId = indicator.getTask().getTaskId();
        Long originalOwnerOrgId = indicator.getOwnerOrg().getId();
        Long originalTargetOrgId = indicator.getTargetOrg().getId();
        String originalDesc = indicator.getIndicatorDesc();
        Integer originalYear = indicator.getYear();
        Integer originalSortOrder = indicator.getSortOrder();
        String originalRemark = indicator.getRemark();

        // Act: Soft delete
        indicatorService.deleteIndicator(indicatorId, null, "Data preservation test delete");

        // Assert: All fields preserved except status
        Indicator archivedIndicator = indicatorRepository.findById(indicatorId).orElse(null);
        assertThat(archivedIndicator).isNotNull();
        
        // Status should be ARCHIVED
        assertThat(archivedIndicator.getStatus()).isEqualTo(IndicatorStatus.ARCHIVED);
        
        // All other fields should be unchanged
        assertThat(archivedIndicator.getTask().getTaskId()).isEqualTo(originalTaskId);
        assertThat(archivedIndicator.getOwnerOrg().getId()).isEqualTo(originalOwnerOrgId);
        assertThat(archivedIndicator.getTargetOrg().getId()).isEqualTo(originalTargetOrgId);
        assertThat(archivedIndicator.getIndicatorDesc()).isEqualTo(originalDesc);
        assertThat(archivedIndicator.getYear()).isEqualTo(originalYear);
        assertThat(archivedIndicator.getSortOrder()).isEqualTo(originalSortOrder);
        assertThat(archivedIndicator.getRemark()).isEqualTo(originalRemark);

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 17.5: Cannot soft delete an already archived indicator
     * 
     * **Feature: sism-fullstack-integration, Property 17: Soft Deletion Behavior**
     * 
     * For any already archived indicator, calling deleteIndicator again SHALL
     * throw a BusinessException indicating the indicator is already archived.
     * 
     * **Validates: Requirements 12.5**
     */
    @Property(tries = 50)
    @Transactional
    void softDelete_shouldRejectAlreadyArchivedIndicator(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // First soft delete
        indicatorService.deleteIndicator(indicatorId, null, "First delete");

        // Act & Assert: Second soft delete should throw exception
        org.junit.jupiter.api.Assertions.assertThrows(
                com.sism.exception.BusinessException.class,
                () -> indicatorService.deleteIndicator(indicatorId, null, "Second delete")
        );

        // Rollback will restore the original state due to @Transactional
    }
}
