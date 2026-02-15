package com.sism.property;

import com.sism.dto.ApprovalRequest;
import com.sism.dto.MilestoneCreateRequest;
import com.sism.dto.ReportCreateRequest;
import com.sism.entity.*;
import com.sism.enums.*;
import com.sism.repository.*;
import com.sism.service.ApprovalService;
import com.sism.service.MilestoneService;
import com.sism.service.ReportService;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Property-based tests for Approval Workflow with Milestone Status Update
 * 
 * **Feature: sism-fullstack-integration, Property 5: Approval Workflow with Milestone Status Update**
 * 
 * For any report approval where achieved_milestone is true and a milestone is associated,
 * the system SHALL automatically update the milestone status to COMPLETED after the report is approved.
 * 
 * **Validates: Requirements 4.5**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class ApprovalMilestoneStatusUpdatePropertyTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private MilestoneService milestoneService;

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
     * Create a test milestone for an indicator.
     * Uses sortOrder=0 to ensure it's the first milestone for catch-up rule compliance.
     */
    private Milestone createTestMilestone(Indicator indicator, MilestoneStatus initialStatus) {
        String uniqueName = "Test Milestone " + UUID.randomUUID().toString().substring(0, 8);
        
        MilestoneCreateRequest request = new MilestoneCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setMilestoneName(uniqueName);
        request.setMilestoneDesc("Test milestone for property testing");
        // Use a past due date to ensure this milestone is the first unpaired one
        request.setDueDate(LocalDate.now().minusDays(30));
        request.setTargetProgress(25);
        request.setSortOrder(0);
        
        var milestoneVO = milestoneService.createMilestone(request);
        Milestone milestone = milestoneRepository.findById(milestoneVO.getMilestoneId()).orElseThrow();
        
        // Set initial status if not NOT_STARTED
        if (initialStatus != MilestoneStatus.NOT_STARTED) {
            milestone.setStatus(initialStatus);
            milestone = milestoneRepository.save(milestone);
        }
        
        return milestone;
    }

    /**
     * Get the first unpaired milestone for an indicator, or create one if none exists.
     */
    private Milestone getOrCreateFirstUnpairedMilestone(Indicator indicator, MilestoneStatus initialStatus) {
        // First, try to find an existing unpaired milestone
        Optional<Milestone> firstUnpaired = milestoneRepository.findFirstUnpairedMilestone(indicator.getIndicatorId());
        
        if (firstUnpaired.isPresent()) {
            Milestone milestone = firstUnpaired.get();
            // Update status if needed
            if (initialStatus != milestone.getStatus()) {
                milestone.setStatus(initialStatus);
                milestone = milestoneRepository.save(milestone);
            }
            return milestone;
        }
        
        // No unpaired milestone exists, create a new one
        return createTestMilestone(indicator, initialStatus);
    }

    /**
     * Create a test report associated with a milestone.
     */
    private ProgressReport createTestReportWithMilestone(
            Indicator indicator, 
            Milestone milestone, 
            SysUser reporter,
            boolean achievedMilestone) {
        
        ReportCreateRequest request = new ReportCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setMilestoneId(milestone.getMilestoneId());
        request.setReporterId(reporter.getId());
        request.setPercentComplete(achievedMilestone ? BigDecimal.valueOf(100) : BigDecimal.valueOf(50));
        request.setNarrative("Test report for milestone status update property testing");
        request.setAchievedMilestone(achievedMilestone);
        
        var reportVO = reportService.createReport(request);
        return reportRepository.findById(reportVO.getReportId()).orElseThrow();
    }

    /**
     * Create a test report without a milestone.
     */
    private ProgressReport createTestReportWithoutMilestone(
            Indicator indicator, 
            SysUser reporter,
            boolean achievedMilestone) {
        
        ReportCreateRequest request = new ReportCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setReporterId(reporter.getId());
        request.setPercentComplete(BigDecimal.valueOf(50));
        request.setNarrative("Test report without milestone for property testing");
        request.setAchievedMilestone(achievedMilestone);
        
        var reportVO = reportService.createReport(request);
        return reportRepository.findById(reportVO.getReportId()).orElseThrow();
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
    Arbitrary<MilestoneStatus> nonCompletedStatuses() {
        return Arbitraries.of(
                MilestoneStatus.NOT_STARTED, 
                MilestoneStatus.IN_PROGRESS, 
                MilestoneStatus.DELAYED
        );
    }


    // ==================== Property Tests ====================

    /**
     * Property 5.1: Approving report with achievedMilestone=true updates milestone to COMPLETED
     * 
     * **Feature: sism-fullstack-integration, Property 5: Approval Workflow with Milestone Status Update**
     * 
     * For any report approval where achieved_milestone is true and a milestone is associated,
     * the system SHALL automatically update the milestone status to COMPLETED.
     * 
     * **Validates: Requirements 4.5**
     */
    @Property(tries = 50)
    @Transactional
    void approvalWithAchievedMilestone_shouldUpdateMilestoneToCompleted(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("nonCompletedStatuses") MilestoneStatus initialStatus) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Get or create the first unpaired milestone (to comply with catch-up rule)
        Milestone milestone = getOrCreateFirstUnpairedMilestone(indicator, initialStatus);
        assertThat(milestone.getStatus()).isEqualTo(initialStatus);

        // Create a report with achievedMilestone=true
        ProgressReport report = createTestReportWithMilestone(indicator, milestone, reporter, true);
        assertThat(report.getAchievedMilestone()).isTrue();
        assertThat(report.getMilestone()).isNotNull();

        // Submit the report
        reportService.submitReport(report.getReportId());

        // Approve the report
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setReportId(report.getReportId());
        approvalRequest.setApproverId(approver.getId());
        approvalRequest.setAction(ApprovalAction.APPROVE);
        approvalRequest.setComment("Approved - milestone achieved");

        approvalService.processApproval(approvalRequest);

        // Verify milestone status is updated to COMPLETED
        Milestone updatedMilestone = milestoneRepository.findById(milestone.getMilestoneId()).orElseThrow();
        assertThat(updatedMilestone.getStatus())
                .as("Milestone status should be COMPLETED after approving report with achievedMilestone=true")
                .isEqualTo(MilestoneStatus.COMPLETED);
    }

    /**
     * Property 5.2: Approving report with achievedMilestone=false does NOT update milestone status
     * 
     * **Feature: sism-fullstack-integration, Property 5: Approval Workflow with Milestone Status Update**
     * 
     * For any report approval where achieved_milestone is false, the milestone status
     * SHALL remain unchanged regardless of approval.
     * 
     * **Validates: Requirements 4.5**
     */
    @Property(tries = 50)
    @Transactional
    void approvalWithoutAchievedMilestone_shouldNotUpdateMilestoneStatus(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("nonCompletedStatuses") MilestoneStatus initialStatus) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Get or create the first unpaired milestone (to comply with catch-up rule)
        Milestone milestone = getOrCreateFirstUnpairedMilestone(indicator, initialStatus);
        assertThat(milestone.getStatus()).isEqualTo(initialStatus);

        // Create a report with achievedMilestone=false
        ProgressReport report = createTestReportWithMilestone(indicator, milestone, reporter, false);
        assertThat(report.getAchievedMilestone()).isFalse();
        assertThat(report.getMilestone()).isNotNull();

        // Submit the report
        reportService.submitReport(report.getReportId());

        // Approve the report
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setReportId(report.getReportId());
        approvalRequest.setApproverId(approver.getId());
        approvalRequest.setAction(ApprovalAction.APPROVE);
        approvalRequest.setComment("Approved - milestone not yet achieved");

        approvalService.processApproval(approvalRequest);

        // Verify milestone status remains unchanged
        Milestone unchangedMilestone = milestoneRepository.findById(milestone.getMilestoneId()).orElseThrow();
        assertThat(unchangedMilestone.getStatus())
                .as("Milestone status should remain %s when achievedMilestone=false", initialStatus)
                .isEqualTo(initialStatus);
    }

    /**
     * Property 5.3: Approving report without milestone does not cause errors
     * 
     * **Feature: sism-fullstack-integration, Property 5: Approval Workflow with Milestone Status Update**
     * 
     * For any report approval where no milestone is associated, the approval
     * SHALL succeed without attempting to update any milestone status.
     * 
     * **Validates: Requirements 4.5**
     */
    @Property(tries = 50)
    @Transactional
    void approvalWithoutMilestone_shouldSucceedWithoutError(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create a report without milestone (achievedMilestone can be true or false)
        ProgressReport report = createTestReportWithoutMilestone(indicator, reporter, true);
        assertThat(report.getMilestone()).isNull();

        // Submit the report
        reportService.submitReport(report.getReportId());

        // Approve the report - should succeed without error
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setReportId(report.getReportId());
        approvalRequest.setApproverId(approver.getId());
        approvalRequest.setAction(ApprovalAction.APPROVE);
        approvalRequest.setComment("Approved - no milestone associated");

        var approvedReport = approvalService.processApproval(approvalRequest);

        // Verify approval succeeded
        assertThat(approvedReport.getStatus()).isEqualTo(ReportStatus.APPROVED);
        assertThat(approvedReport.getIsFinal()).isTrue();
    }

    /**
     * Property 5.4: Milestone status update is idempotent for already COMPLETED milestones
     * 
     * **Feature: sism-fullstack-integration, Property 5: Approval Workflow with Milestone Status Update**
     * 
     * For any report approval where the milestone is already COMPLETED,
     * the approval SHALL succeed and the milestone SHALL remain COMPLETED.
     * 
     * **Validates: Requirements 4.5**
     */
    @Property(tries = 30)
    @Transactional
    void approvalWithAlreadyCompletedMilestone_shouldRemainCompleted(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Get or create the first unpaired milestone that is already COMPLETED
        Milestone milestone = getOrCreateFirstUnpairedMilestone(indicator, MilestoneStatus.COMPLETED);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.COMPLETED);

        // Create a report with achievedMilestone=true
        ProgressReport report = createTestReportWithMilestone(indicator, milestone, reporter, true);

        // Submit the report
        reportService.submitReport(report.getReportId());

        // Approve the report
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setReportId(report.getReportId());
        approvalRequest.setApproverId(approver.getId());
        approvalRequest.setAction(ApprovalAction.APPROVE);
        approvalRequest.setComment("Approved - milestone was already completed");

        var approvedReport = approvalService.processApproval(approvalRequest);

        // Verify approval succeeded and milestone remains COMPLETED
        assertThat(approvedReport.getStatus()).isEqualTo(ReportStatus.APPROVED);
        
        Milestone stillCompletedMilestone = milestoneRepository.findById(milestone.getMilestoneId()).orElseThrow();
        assertThat(stillCompletedMilestone.getStatus())
                .as("Milestone should remain COMPLETED")
                .isEqualTo(MilestoneStatus.COMPLETED);
    }

    /**
     * Property 5.5: Rejection and Return actions do NOT update milestone status
     * 
     * **Feature: sism-fullstack-integration, Property 5: Approval Workflow with Milestone Status Update**
     * 
     * For any report with achievedMilestone=true that is rejected or returned,
     * the milestone status SHALL NOT be updated to COMPLETED.
     * 
     * **Validates: Requirements 4.5**
     */
    @Property(tries = 50)
    @Transactional
    void rejectionOrReturn_shouldNotUpdateMilestoneStatus(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("nonCompletedStatuses") MilestoneStatus initialStatus,
            @ForAll Arbitrary<ApprovalAction> actionArb) {

        // Only test REJECT and RETURN actions
        ApprovalAction action = Arbitraries.of(ApprovalAction.REJECT, ApprovalAction.RETURN).sample();

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Get or create the first unpaired milestone (to comply with catch-up rule)
        Milestone milestone = getOrCreateFirstUnpairedMilestone(indicator, initialStatus);
        assertThat(milestone.getStatus()).isEqualTo(initialStatus);

        // Create a report with achievedMilestone=true
        ProgressReport report = createTestReportWithMilestone(indicator, milestone, reporter, true);
        assertThat(report.getAchievedMilestone()).isTrue();

        // Submit the report
        reportService.submitReport(report.getReportId());

        // Reject or Return the report
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setReportId(report.getReportId());
        approvalRequest.setApproverId(approver.getId());
        approvalRequest.setAction(action);
        approvalRequest.setComment("Test " + action + " - milestone should not be updated");

        approvalService.processApproval(approvalRequest);

        // Verify milestone status remains unchanged
        Milestone unchangedMilestone = milestoneRepository.findById(milestone.getMilestoneId()).orElseThrow();
        assertThat(unchangedMilestone.getStatus())
                .as("Milestone status should remain %s after %s action", initialStatus, action)
                .isEqualTo(initialStatus);
    }
}
