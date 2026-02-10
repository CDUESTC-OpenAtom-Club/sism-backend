package com.sism.property;

import com.sism.dto.ReportCreateRequest;
import com.sism.dto.ReportUpdateRequest;
import com.sism.entity.*;
import com.sism.enums.*;
import com.sism.exception.BusinessException;
import com.sism.repository.*;
import com.sism.service.ReportService;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Property-based tests for Milestone-AdhocTask Mutual Exclusion
 * 
 * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
 * 
 * For any progress report, the milestone_id and adhoc_task_id fields SHALL NOT both be 
 * non-null simultaneously. The system SHALL reject any attempt to create or update a 
 * report violating this constraint.
 * 
 * **Validates: Requirements 3.4**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class MilestoneAdhocTaskMutualExclusionPropertyTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private UserRepository userRepository;

    // ==================== Helper Methods ====================

    /**
     * Get existing ACTIVE indicators from the database for testing.
     */
    private List<Indicator> getExistingActiveIndicators(int limit) {
        return indicatorRepository.findAll().stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .limit(limit)
                .toList();
    }

    /**
     * Get existing users from the database for testing.
     */
    private List<SysUser> getExistingUsers(int limit) {
        return userRepository.findAll().stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get milestones for a specific indicator.
     */
    private List<Milestone> getMilestonesForIndicator(Long indicatorId) {
        return milestoneRepository.findByIndicator_IndicatorId(indicatorId);
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Integer> indicatorIndices() {
        return Arbitraries.integers().between(0, 9);
    }

    @Provide
    Arbitrary<Integer> userIndices() {
        return Arbitraries.integers().between(0, 4);
    }

    @Provide
    Arbitrary<BigDecimal> percentComplete() {
        return Arbitraries.integers().between(0, 100)
                .map(BigDecimal::valueOf);
    }

    @Provide
    Arbitrary<Long> fakeAdhocTaskIds() {
        // Generate fake adhoc task IDs for testing mutual exclusion
        // These don't need to exist - we're testing the validation logic
        return Arbitraries.longs().between(1L, 1000L);
    }


    // ==================== Property Tests ====================

    /**
     * Property 4.1: Reports with only milestone (no adhocTask) are valid
     * 
     * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
     * 
     * For any report with only a milestone_id set (adhoc_task_id is null), 
     * the system SHALL accept the report creation.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 50)
    @Transactional
    void reportsWithOnlyMilestone_shouldBeValid(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("percentComplete") BigDecimal percent) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        
        // Get milestones for this indicator
        List<Milestone> milestones = getMilestonesForIndicator(indicator.getIndicatorId());
        assumeThat(milestones).isNotEmpty();
        
        Milestone milestone = milestones.get(0);

        // Create report with only milestone (no adhocTask)
        ReportCreateRequest request = new ReportCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setReporterId(reporter.getId());
        request.setMilestoneId(milestone.getMilestoneId());
        request.setAdhocTaskId(null);  // Explicitly null
        request.setPercentComplete(percent);
        request.setNarrative("Property test - milestone only");
        request.setAchievedMilestone(false);

        // Should succeed
        var reportVO = reportService.createReport(request);

        // Assert: Report created successfully with milestone
        assertThat(reportVO).isNotNull();
        assertThat(reportVO.getReportId()).isNotNull();
        assertThat(reportVO.getMilestoneId()).isEqualTo(milestone.getMilestoneId());
        assertThat(reportVO.getAdhocTaskId()).isNull();
    }

    /**
     * Property 4.2: Reports with neither milestone nor adhocTask are valid
     * 
     * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
     * 
     * For any report with both milestone_id and adhoc_task_id set to null, 
     * the system SHALL accept the report creation.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 50)
    @Transactional
    void reportsWithNeitherMilestoneNorAdhocTask_shouldBeValid(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("percentComplete") BigDecimal percent) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());

        // Create report with neither milestone nor adhocTask
        ReportCreateRequest request = new ReportCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setReporterId(reporter.getId());
        request.setMilestoneId(null);
        request.setAdhocTaskId(null);
        request.setPercentComplete(percent);
        request.setNarrative("Property test - neither milestone nor adhocTask");
        request.setAchievedMilestone(false);

        // Should succeed
        var reportVO = reportService.createReport(request);

        // Assert: Report created successfully without milestone or adhocTask
        assertThat(reportVO).isNotNull();
        assertThat(reportVO.getReportId()).isNotNull();
        assertThat(reportVO.getMilestoneId()).isNull();
        assertThat(reportVO.getAdhocTaskId()).isNull();
    }


    /**
     * Property 4.3: Reports with BOTH milestone AND adhocTask are rejected on creation
     * 
     * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
     * 
     * For any attempt to create a report with both milestone_id and adhoc_task_id non-null,
     * the system SHALL reject the creation with a BusinessException.
     * 
     * Note: This test uses the service-level validation which checks the mutual exclusion
     * constraint BEFORE attempting to look up the adhoc task in the database.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 50)
    @Transactional
    void reportsWithBothMilestoneAndAdhocTask_shouldBeRejectedOnCreate(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("fakeAdhocTaskIds") Long fakeAdhocTaskId,
            @ForAll("percentComplete") BigDecimal percent) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        
        // Get milestones for this indicator
        List<Milestone> milestones = getMilestonesForIndicator(indicator.getIndicatorId());
        assumeThat(milestones).isNotEmpty();
        
        Milestone milestone = milestones.get(0);

        // Attempt to create report with BOTH milestone AND adhocTask
        ReportCreateRequest request = new ReportCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setReporterId(reporter.getId());
        request.setMilestoneId(milestone.getMilestoneId());
        request.setAdhocTaskId(fakeAdhocTaskId);  // Both set - violates constraint
        request.setPercentComplete(percent);
        request.setNarrative("Property test - both milestone and adhocTask (should fail)");
        request.setAchievedMilestone(false);

        // Should be rejected by service-level validation BEFORE database lookup
        assertThatThrownBy(() -> reportService.createReport(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Milestone and adhoc task cannot be associated simultaneously");
    }

    /**
     * Property 4.4: Reports with BOTH milestone AND adhocTask are rejected on update
     * 
     * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
     * 
     * For any attempt to update a report to have both milestone_id and adhoc_task_id non-null,
     * the system SHALL reject the update with a BusinessException.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 50)
    @Transactional
    void reportsWithBothMilestoneAndAdhocTask_shouldBeRejectedOnUpdate(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("fakeAdhocTaskIds") Long fakeAdhocTaskId,
            @ForAll("percentComplete") BigDecimal percent) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        
        // Get milestones for this indicator
        List<Milestone> milestones = getMilestonesForIndicator(indicator.getIndicatorId());
        assumeThat(milestones).isNotEmpty();
        
        Milestone milestone = milestones.get(0);

        // First, create a valid report with only milestone
        ReportCreateRequest createRequest = new ReportCreateRequest();
        createRequest.setIndicatorId(indicator.getIndicatorId());
        createRequest.setReporterId(reporter.getId());
        createRequest.setMilestoneId(milestone.getMilestoneId());
        createRequest.setAdhocTaskId(null);
        createRequest.setPercentComplete(percent);
        createRequest.setNarrative("Property test - initial report with milestone");
        createRequest.setAchievedMilestone(false);

        var reportVO = reportService.createReport(createRequest);
        Long reportId = reportVO.getReportId();

        // Now attempt to update to add adhocTask (violating mutual exclusion)
        ReportUpdateRequest updateRequest = new ReportUpdateRequest();
        updateRequest.setAdhocTaskId(fakeAdhocTaskId);  // Adding adhocTask while milestone exists

        // Should be rejected by service-level validation
        assertThatThrownBy(() -> reportService.updateReport(reportId, updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Milestone and adhoc task cannot be associated simultaneously");
    }


    /**
     * Property 4.5: Service-level validation enforces mutual exclusion
     * 
     * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
     * 
     * The ReportService.validateMutualExclusion method SHALL reject any combination
     * where both milestoneId and adhocTaskId are non-null.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 100)
    void serviceLevelValidation_shouldEnforceMutualExclusion(
            @ForAll("fakeAdhocTaskIds") Long milestoneId,
            @ForAll("fakeAdhocTaskIds") Long adhocTaskId) {

        // When both IDs are non-null, validation should fail
        assertThatThrownBy(() -> reportService.validateMutualExclusion(milestoneId, adhocTaskId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Milestone and adhoc task cannot be associated simultaneously");
    }

    /**
     * Property 4.6: Service-level validation allows milestone-only
     * 
     * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
     * 
     * The ReportService.validateMutualExclusion method SHALL accept when only
     * milestoneId is non-null and adhocTaskId is null.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 100)
    void serviceLevelValidation_shouldAllowMilestoneOnly(
            @ForAll("fakeAdhocTaskIds") Long milestoneId) {

        // When only milestoneId is set, validation should pass (no exception)
        reportService.validateMutualExclusion(milestoneId, null);
        // If we reach here, the test passes
    }

    /**
     * Property 4.7: Service-level validation allows adhocTask-only
     * 
     * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
     * 
     * The ReportService.validateMutualExclusion method SHALL accept when only
     * adhocTaskId is non-null and milestoneId is null.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 100)
    void serviceLevelValidation_shouldAllowAdhocTaskOnly(
            @ForAll("fakeAdhocTaskIds") Long adhocTaskId) {

        // When only adhocTaskId is set, validation should pass (no exception)
        reportService.validateMutualExclusion(null, adhocTaskId);
        // If we reach here, the test passes
    }

    /**
     * Property 4.8: Service-level validation allows neither
     * 
     * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
     * 
     * The ReportService.validateMutualExclusion method SHALL accept when both
     * milestoneId and adhocTaskId are null.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 10)
    void serviceLevelValidation_shouldAllowNeither() {

        // When both are null, validation should pass (no exception)
        reportService.validateMutualExclusion(null, null);
        // If we reach here, the test passes
    }

    /**
     * Property 4.9: Entity-level validation also enforces mutual exclusion
     * 
     * **Feature: sism-fullstack-integration, Property 4: Milestone-AdhocTask Mutual Exclusion**
     * 
     * The ProgressReport entity has @PrePersist/@PreUpdate validation that enforces
     * the mutual exclusion constraint at the JPA level as a safety net.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 30)
    @Transactional
    void entityLevelValidation_shouldEnforceMutualExclusion(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        
        // Get milestones for this indicator
        List<Milestone> milestones = getMilestonesForIndicator(indicator.getIndicatorId());
        assumeThat(milestones).isNotEmpty();
        
        Milestone milestone = milestones.get(0);

        // Create entity directly with both milestone and a fake adhocTask reference
        ProgressReport report = new ProgressReport();
        report.setIndicator(indicator);
        report.setReporter(reporter);
        report.setMilestone(milestone);
        report.setPercentComplete(BigDecimal.valueOf(50));
        report.setNarrative("Direct entity test");

        // Create a fake AdhocTask object (not persisted, just for testing validation)
        AdhocTask fakeAdhocTask = new AdhocTask();
        fakeAdhocTask.setAdhocTaskId(999L);
        report.setAdhocTask(fakeAdhocTask);  // Both set - violates constraint

        // Attempting to save should trigger @PrePersist validation
        assertThatThrownBy(() -> reportRepository.saveAndFlush(report))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Milestone and adhoc task cannot be associated simultaneously");
    }
}
