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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Property-based tests for Single Final Version Per Milestone constraint
 * 
 * **Feature: sism-fullstack-integration, Property 6: Single Final Version Per Milestone**
 * 
 * For any milestone, there SHALL be at most one progress report with is_final=true 
 * and status=APPROVED. When a new report is approved for the same milestone, 
 * the previous final report SHALL have its is_final flag set to false.
 * 
 * **Validates: Requirements 4.6**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class SingleFinalVersionPropertyTest {

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
     * Uses a past due date to ensure it's the first unpaired milestone for catch-up rule compliance.
     */
    private Milestone createTestMilestone(Indicator indicator) {
        String uniqueName = "Test Milestone " + UUID.randomUUID().toString().substring(0, 8);
        
        MilestoneCreateRequest request = new MilestoneCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setMilestoneName(uniqueName);
        request.setMilestoneDesc("Test milestone for single final version property testing");
        // Use a past due date to ensure this milestone is the first unpaired one
        request.setDueDate(LocalDate.now().minusDays(30));
        request.setWeightPercent(BigDecimal.valueOf(25));
        request.setSortOrder(0);
        
        var milestoneVO = milestoneService.createMilestone(request);
        return milestoneRepository.findById(milestoneVO.getMilestoneId()).orElseThrow();
    }

    /**
     * Get the first unpaired milestone for an indicator, or create one if none exists.
     */
    private Milestone getOrCreateFirstUnpairedMilestone(Indicator indicator) {
        // First, try to find an existing unpaired milestone
        Optional<Milestone> firstUnpaired = milestoneRepository.findFirstUnpairedMilestone(indicator.getIndicatorId());
        
        if (firstUnpaired.isPresent()) {
            return firstUnpaired.get();
        }
        
        // No unpaired milestone exists, create a new one
        return createTestMilestone(indicator);
    }

    /**
     * Create a test report associated with a milestone.
     * The milestone is used directly - the catch-up rule allows creating reports
     * for milestones that already have approved reports (revision scenario).
     */
    private ProgressReport createTestReportWithMilestone(
            Indicator indicator, 
            Milestone milestone, 
            SysUser reporter) {
        
        ReportCreateRequest request = new ReportCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setMilestoneId(milestone.getMilestoneId());
        request.setReporterId(reporter.getId());
        request.setPercentComplete(BigDecimal.valueOf(50));
        request.setNarrative("Test report for single final version property testing - " + UUID.randomUUID());
        request.setAchievedMilestone(false);
        
        var reportVO = reportService.createReport(request);
        return reportRepository.findById(reportVO.getReportId()).orElseThrow();
    }

    /**
     * Submit and approve a report.
     */
    private ProgressReport submitAndApproveReport(ProgressReport report, SysUser approver) {
        // Submit the report
        reportService.submitReport(report.getReportId());
        
        // Approve the report
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setReportId(report.getReportId());
        approvalRequest.setApproverId(approver.getId());
        approvalRequest.setAction(ApprovalAction.APPROVE);
        approvalRequest.setComment("Approved for single final version test");
        
        approvalService.processApproval(approvalRequest);
        
        return reportRepository.findById(report.getReportId()).orElseThrow();
    }

    /**
     * Count final approved reports for a milestone.
     */
    private long countFinalApprovedReports(Long milestoneId) {
        return reportRepository.findByMilestone_MilestoneIdAndStatus(milestoneId, ReportStatus.APPROVED)
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .count();
    }

    /**
     * Get all final approved reports for a milestone.
     */
    private List<ProgressReport> getFinalApprovedReports(Long milestoneId) {
        return reportRepository.findByMilestone_MilestoneIdAndStatus(milestoneId, ReportStatus.APPROVED)
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .collect(Collectors.toList());
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
    Arbitrary<Integer> reportCounts() {
        return Arbitraries.integers().between(2, 5);
    }


    // ==================== Property Tests ====================

    /**
     * Property 6.1: At most one final approved report per milestone
     * 
     * **Feature: sism-fullstack-integration, Property 6: Single Final Version Per Milestone**
     * 
     * For any milestone, there SHALL be at most one progress report with 
     * is_final=true and status=APPROVED at any given time.
     * 
     * **Validates: Requirements 4.6**
     */
    @Property(tries = 50)
    @Transactional
    void atMostOneFinalApprovedReportPerMilestone(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("reportCounts") Integer reportCount) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Get or create the first unpaired milestone (to comply with catch-up rule)
        Milestone milestone = getOrCreateFirstUnpairedMilestone(indicator);

        // Create and approve multiple reports for the same milestone
        List<ProgressReport> approvedReports = new ArrayList<>();
        for (int i = 0; i < reportCount; i++) {
            ProgressReport report = createTestReportWithMilestone(indicator, milestone, reporter);
            ProgressReport approvedReport = submitAndApproveReport(report, approver);
            approvedReports.add(approvedReport);
        }

        // Verify: at most one final approved report exists
        long finalCount = countFinalApprovedReports(milestone.getMilestoneId());
        assertThat(finalCount)
                .as("There should be at most one final approved report per milestone, but found %d", finalCount)
                .isLessThanOrEqualTo(1);
    }

    /**
     * Property 6.2: New approval sets previous final to false
     * 
     * **Feature: sism-fullstack-integration, Property 6: Single Final Version Per Milestone**
     * 
     * When a new report is approved for the same milestone, the previous final 
     * report SHALL have its is_final flag set to false.
     * 
     * **Validates: Requirements 4.6**
     */
    @Property(tries = 50)
    @Transactional
    void newApprovalSetsPreviousFinalToFalse(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Get or create the first unpaired milestone (to comply with catch-up rule)
        Milestone milestone = getOrCreateFirstUnpairedMilestone(indicator);

        // Create and approve first report
        ProgressReport firstReport = createTestReportWithMilestone(indicator, milestone, reporter);
        ProgressReport firstApproved = submitAndApproveReport(firstReport, approver);
        
        // Verify first report is final
        assertThat(firstApproved.getIsFinal())
                .as("First approved report should be final")
                .isTrue();

        // Create and approve second report
        ProgressReport secondReport = createTestReportWithMilestone(indicator, milestone, reporter);
        ProgressReport secondApproved = submitAndApproveReport(secondReport, approver);

        // Verify second report is now final
        assertThat(secondApproved.getIsFinal())
                .as("Second approved report should be final")
                .isTrue();

        // Verify first report is no longer final
        ProgressReport firstReportRefreshed = reportRepository.findById(firstApproved.getReportId()).orElseThrow();
        assertThat(firstReportRefreshed.getIsFinal())
                .as("First report should no longer be final after second approval")
                .isFalse();
    }

    /**
     * Property 6.3: Latest approved report is always final
     * 
     * **Feature: sism-fullstack-integration, Property 6: Single Final Version Per Milestone**
     * 
     * For any sequence of approvals on the same milestone, the most recently 
     * approved report SHALL be the one with is_final=true.
     * 
     * **Validates: Requirements 4.6**
     */
    @Property(tries = 30)
    @Transactional
    void latestApprovedReportIsAlwaysFinal(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("reportCounts") Integer reportCount) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Get or create the first unpaired milestone (to comply with catch-up rule)
        Milestone milestone = getOrCreateFirstUnpairedMilestone(indicator);

        // Create and approve multiple reports, tracking the last one
        ProgressReport lastApprovedReport = null;
        for (int i = 0; i < reportCount; i++) {
            ProgressReport report = createTestReportWithMilestone(indicator, milestone, reporter);
            lastApprovedReport = submitAndApproveReport(report, approver);
        }

        // Verify: the last approved report is the final one
        assertThat(lastApprovedReport).isNotNull();
        
        List<ProgressReport> finalReports = getFinalApprovedReports(milestone.getMilestoneId());
        assertThat(finalReports)
                .as("There should be exactly one final report")
                .hasSize(1);
        
        assertThat(finalReports.get(0).getReportId())
                .as("The final report should be the last approved one")
                .isEqualTo(lastApprovedReport.getReportId());
    }

    /**
     * Property 6.4: Exactly one final report after any number of approvals
     * 
     * **Feature: sism-fullstack-integration, Property 6: Single Final Version Per Milestone**
     * 
     * After approving N reports for the same milestone (where N >= 1), 
     * there SHALL be exactly one report with is_final=true.
     * 
     * **Validates: Requirements 4.6**
     */
    @Property(tries = 50)
    @Transactional
    void exactlyOneFinalReportAfterApprovals(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("reportCounts") Integer reportCount) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Get or create the first unpaired milestone (to comply with catch-up rule)
        Milestone milestone = getOrCreateFirstUnpairedMilestone(indicator);

        // Create and approve multiple reports
        for (int i = 0; i < reportCount; i++) {
            ProgressReport report = createTestReportWithMilestone(indicator, milestone, reporter);
            submitAndApproveReport(report, approver);
        }

        // Verify: exactly one final report exists
        long finalCount = countFinalApprovedReports(milestone.getMilestoneId());
        assertThat(finalCount)
                .as("There should be exactly one final approved report after %d approvals", reportCount)
                .isEqualTo(1);
    }

    /**
     * Property 6.5: Non-final approved reports remain approved
     * 
     * **Feature: sism-fullstack-integration, Property 6: Single Final Version Per Milestone**
     * 
     * When a new report is approved and becomes final, the previous final report 
     * SHALL remain in APPROVED status (only is_final changes to false).
     * 
     * **Validates: Requirements 4.6**
     */
    @Property(tries = 50)
    @Transactional
    void nonFinalApprovedReportsRemainApproved(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("reportCounts") Integer reportCount) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Get or create the first unpaired milestone (to comply with catch-up rule)
        Milestone milestone = getOrCreateFirstUnpairedMilestone(indicator);

        // Create and approve multiple reports
        List<Long> approvedReportIds = new ArrayList<>();
        for (int i = 0; i < reportCount; i++) {
            ProgressReport report = createTestReportWithMilestone(indicator, milestone, reporter);
            ProgressReport approved = submitAndApproveReport(report, approver);
            approvedReportIds.add(approved.getReportId());
        }

        // Verify: all reports remain in APPROVED status
        for (Long reportId : approvedReportIds) {
            ProgressReport report = reportRepository.findById(reportId).orElseThrow();
            assertThat(report.getStatus())
                    .as("Report %d should remain in APPROVED status", reportId)
                    .isEqualTo(ReportStatus.APPROVED);
        }
    }
}
