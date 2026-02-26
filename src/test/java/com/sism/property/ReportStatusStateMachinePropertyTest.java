package com.sism.property;

import com.sism.entity.*;
import com.sism.enums.*;
import com.sism.exception.BusinessException;
import com.sism.repository.*;
import com.sism.service.ApprovalService;
import com.sism.service.ReportService;
import com.sism.dto.ApprovalRequest;
import com.sism.dto.ReportCreateRequest;
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
 * Property-based tests for Report Status State Machine
 * 
 * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
 * 
 * For any progress report, the status transitions SHALL follow the valid state machine:
 * DRAFT → SUBMITTED → (APPROVED | REJECTED | RETURNED), and RETURNED → DRAFT (for resubmission).
 * No other transitions are permitted.
 * 
 * Valid transitions:
 * - DRAFT → SUBMITTED (submit)
 * - SUBMITTED → APPROVED (approve)
 * - SUBMITTED → REJECTED (reject)
 * - SUBMITTED → RETURNED (return)
 * - SUBMITTED → DRAFT (withdraw)
 * - RETURNED → SUBMITTED (resubmit)
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 4.2, 4.3, 4.4**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class ReportStatusStateMachinePropertyTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private UserRepository userRepository;


    // ==================== Valid State Transitions ====================
    
    /**
     * Define valid state transitions for the report status state machine.
     */
    private static final Map<ReportStatus, Set<ReportStatus>> VALID_TRANSITIONS = Map.of(
            ReportStatus.DRAFT, Set.of(ReportStatus.SUBMITTED),
            ReportStatus.SUBMITTED, Set.of(ReportStatus.APPROVED, ReportStatus.REJECTED, ReportStatus.RETURNED, ReportStatus.DRAFT),
            ReportStatus.RETURNED, Set.of(ReportStatus.SUBMITTED, ReportStatus.DRAFT),
            ReportStatus.APPROVED, Set.of(),  // Terminal state
            ReportStatus.REJECTED, Set.of()   // Terminal state
    );

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
     * Create a test report in DRAFT status.
     */
    private ProgressReport createTestReportInDraft(Indicator indicator, SysUser reporter) {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setReporterId(reporter.getId());
        request.setPercentComplete(BigDecimal.valueOf(50));
        request.setNarrative("Test report for property testing");
        request.setAchievedMilestone(false);
        
        var reportVO = reportService.createReport(request);
        return reportRepository.findById(reportVO.getReportId()).orElseThrow();
    }

    /**
     * Check if a transition is valid according to the state machine.
     */
    private boolean isValidTransition(ReportStatus from, ReportStatus to) {
        Set<ReportStatus> validTargets = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        return validTargets.contains(to);
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
    Arbitrary<ReportStatus> allStatuses() {
        return Arbitraries.of(ReportStatus.values());
    }

    @Provide
    Arbitrary<ApprovalAction> approvalActions() {
        return Arbitraries.of(ApprovalAction.APPROVE, ApprovalAction.REJECT, ApprovalAction.RETURN);
    }


    // ==================== Property Tests ====================

    /**
     * Property 3.1: New reports are created in DRAFT status
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any newly created report, the initial status SHALL be DRAFT.
     * 
     * **Validates: Requirements 3.1**
     */
    @Property(tries = 50)
    @Transactional
    void newReports_shouldBeCreatedInDraftStatus(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());

        // Create a new report
        ReportCreateRequest request = new ReportCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setReporterId(reporter.getId());
        request.setPercentComplete(BigDecimal.valueOf(25));
        request.setNarrative("Property test report");
        request.setAchievedMilestone(false);

        var reportVO = reportService.createReport(request);

        // Assert: New report should be in DRAFT status
        assertThat(reportVO.getStatus()).isEqualTo(ReportStatus.DRAFT);
        assertThat(reportVO.getIsFinal()).isFalse();
    }

    /**
     * Property 3.2: DRAFT reports can be submitted
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in DRAFT status, submitting SHALL transition it to SUBMITTED status.
     * 
     * **Validates: Requirements 3.2**
     */
    @Property(tries = 50)
    @Transactional
    void draftReports_canBeSubmitted(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());

        // Create a report in DRAFT status
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DRAFT);

        // Submit the report
        var submittedReport = reportService.submitReport(report.getReportId());

        // Assert: Status should transition to SUBMITTED
        assertThat(submittedReport.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(submittedReport.getReportedAt()).isNotNull();
    }

    /**
     * Property 3.3: SUBMITTED reports can be withdrawn to DRAFT
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in SUBMITTED status, withdrawing SHALL transition it back to DRAFT status.
     * 
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 50)
    @Transactional
    void submittedReports_canBeWithdrawn(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());

        // Create and submit a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());

        // Withdraw the report
        var withdrawnReport = reportService.withdrawReport(report.getReportId());

        // Assert: Status should transition back to DRAFT
        assertThat(withdrawnReport.getStatus()).isEqualTo(ReportStatus.DRAFT);
        assertThat(withdrawnReport.getReportedAt()).isNull();
    }


    /**
     * Property 3.4: SUBMITTED reports can be approved
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in SUBMITTED status, approving SHALL transition it to APPROVED status.
     * 
     * **Validates: Requirements 4.2**
     */
    @Property(tries = 50)
    @Transactional
    void submittedReports_canBeApproved(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create and submit a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());

        // Approve the report
        ApprovalRequest request = new ApprovalRequest();
        request.setReportId(report.getReportId());
        request.setApproverId(approver.getId());
        request.setAction(ApprovalAction.APPROVE);
        request.setComment("Approved via property test");

        var approvedReport = approvalService.processApproval(request);

        // Assert: Status should transition to APPROVED
        assertThat(approvedReport.getStatus()).isEqualTo(ReportStatus.APPROVED);
        assertThat(approvedReport.getIsFinal()).isTrue();
    }

    /**
     * Property 3.5: SUBMITTED reports can be rejected
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in SUBMITTED status, rejecting SHALL transition it to REJECTED status.
     * 
     * **Validates: Requirements 4.3**
     */
    @Property(tries = 50)
    @Transactional
    void submittedReports_canBeRejected(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create and submit a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());

        // Reject the report
        ApprovalRequest request = new ApprovalRequest();
        request.setReportId(report.getReportId());
        request.setApproverId(approver.getId());
        request.setAction(ApprovalAction.REJECT);
        request.setComment("Rejected via property test - data incomplete");

        var rejectedReport = approvalService.processApproval(request);

        // Assert: Status should transition to REJECTED
        assertThat(rejectedReport.getStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    /**
     * Property 3.6: SUBMITTED reports can be returned
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in SUBMITTED status, returning SHALL transition it to RETURNED status.
     * 
     * **Validates: Requirements 4.4**
     */
    @Property(tries = 50)
    @Transactional
    void submittedReports_canBeReturned(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create and submit a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());

        // Return the report
        ApprovalRequest request = new ApprovalRequest();
        request.setReportId(report.getReportId());
        request.setApproverId(approver.getId());
        request.setAction(ApprovalAction.RETURN);
        request.setComment("Returned via property test - needs revision");

        var returnedReport = approvalService.processApproval(request);

        // Assert: Status should transition to RETURNED
        assertThat(returnedReport.getStatus()).isEqualTo(ReportStatus.RETURNED);
    }


    /**
     * Property 3.7: RETURNED reports can be resubmitted
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in RETURNED status, submitting SHALL transition it to SUBMITTED status.
     * 
     * **Validates: Requirements 3.2**
     */
    @Property(tries = 50)
    @Transactional
    void returnedReports_canBeResubmitted(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create, submit, and return a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());
        
        ApprovalRequest returnRequest = new ApprovalRequest();
        returnRequest.setReportId(report.getReportId());
        returnRequest.setApproverId(approver.getId());
        returnRequest.setAction(ApprovalAction.RETURN);
        returnRequest.setComment("Needs revision");
        approvalService.processApproval(returnRequest);

        // Resubmit the report
        var resubmittedReport = reportService.submitReport(report.getReportId());

        // Assert: Status should transition to SUBMITTED
        assertThat(resubmittedReport.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
    }

    /**
     * Property 3.8: Invalid transitions from DRAFT are rejected
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in DRAFT status, attempting to withdraw SHALL be rejected.
     * 
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 30)
    @Transactional
    void draftReports_cannotBeWithdrawn(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());

        // Create a report in DRAFT status
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DRAFT);

        // Attempt to withdraw (should fail)
        assertThatThrownBy(() -> reportService.withdrawReport(report.getReportId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SUBMITTED");
    }

    /**
     * Property 3.9: Invalid transitions from APPROVED are rejected
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in APPROVED status (terminal state), no further transitions are allowed.
     * 
     * **Validates: Requirements 4.2**
     */
    @Property(tries = 30)
    @Transactional
    void approvedReports_cannotBeModified(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create, submit, and approve a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());
        
        ApprovalRequest approveRequest = new ApprovalRequest();
        approveRequest.setReportId(report.getReportId());
        approveRequest.setApproverId(approver.getId());
        approveRequest.setAction(ApprovalAction.APPROVE);
        approveRequest.setComment("Approved");
        approvalService.processApproval(approveRequest);

        // Attempt to submit again (should fail)
        assertThatThrownBy(() -> reportService.submitReport(report.getReportId()))
                .isInstanceOf(BusinessException.class);

        // Attempt to withdraw (should fail)
        assertThatThrownBy(() -> reportService.withdrawReport(report.getReportId()))
                .isInstanceOf(BusinessException.class);
    }


    /**
     * Property 3.10: Invalid transitions from REJECTED are rejected
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in REJECTED status (terminal state), no further transitions are allowed.
     * 
     * **Validates: Requirements 4.3**
     */
    @Property(tries = 30)
    @Transactional
    void rejectedReports_cannotBeModified(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create, submit, and reject a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());
        
        ApprovalRequest rejectRequest = new ApprovalRequest();
        rejectRequest.setReportId(report.getReportId());
        rejectRequest.setApproverId(approver.getId());
        rejectRequest.setAction(ApprovalAction.REJECT);
        rejectRequest.setComment("Rejected - invalid data");
        approvalService.processApproval(rejectRequest);

        // Attempt to submit again (should fail)
        assertThatThrownBy(() -> reportService.submitReport(report.getReportId()))
                .isInstanceOf(BusinessException.class);

        // Attempt to withdraw (should fail)
        assertThatThrownBy(() -> reportService.withdrawReport(report.getReportId()))
                .isInstanceOf(BusinessException.class);
    }

    /**
     * Property 3.11: Non-SUBMITTED reports cannot be approved
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report NOT in SUBMITTED status, approval actions SHALL be rejected.
     * 
     * **Validates: Requirements 4.2, 4.3, 4.4**
     */
    @Property(tries = 30)
    @Transactional
    void nonSubmittedReports_cannotBeApproved(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex,
            @ForAll("approvalActions") ApprovalAction action) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create a report in DRAFT status (not submitted)
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DRAFT);

        // Attempt approval action (should fail)
        ApprovalRequest request = new ApprovalRequest();
        request.setReportId(report.getReportId());
        request.setApproverId(approver.getId());
        request.setAction(action);
        request.setComment("Test comment for " + action);

        assertThatThrownBy(() -> approvalService.processApproval(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SUBMITTED");
    }

    /**
     * Property 3.12: Only DRAFT status reports can be updated
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in DRAFT status, updating content SHALL be allowed.
     * 
     * **Validates: Requirements 3.1**
     */
    @Property(tries = 50)
    @Transactional
    void draftReports_canBeUpdated(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());

        // Create a report in DRAFT status
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DRAFT);

        // Update the report content
        com.sism.dto.ReportUpdateRequest updateRequest = new com.sism.dto.ReportUpdateRequest();
        updateRequest.setPercentComplete(BigDecimal.valueOf(75));
        updateRequest.setNarrative("Updated narrative for property test");

        var updatedReport = reportService.updateReport(report.getReportId(), updateRequest);

        // Assert: Update should succeed
        assertThat(updatedReport.getPercentComplete()).isEqualByComparingTo(BigDecimal.valueOf(75));
        assertThat(updatedReport.getNarrative()).isEqualTo("Updated narrative for property test");
        assertThat(updatedReport.getStatus()).isEqualTo(ReportStatus.DRAFT);
    }

    /**
     * Property 3.13: RETURNED status reports can be updated
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in RETURNED status, updating content SHALL be allowed.
     * 
     * **Validates: Requirements 3.1, 4.4**
     */
    @Property(tries = 50)
    @Transactional
    void returnedReports_canBeUpdated(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create, submit, and return a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());
        
        ApprovalRequest returnRequest = new ApprovalRequest();
        returnRequest.setReportId(report.getReportId());
        returnRequest.setApproverId(approver.getId());
        returnRequest.setAction(ApprovalAction.RETURN);
        returnRequest.setComment("Needs revision");
        approvalService.processApproval(returnRequest);

        // Verify report is in RETURNED status
        var returnedReport = reportService.getReportById(report.getReportId());
        assertThat(returnedReport.getStatus()).isEqualTo(ReportStatus.RETURNED);

        // Update the report content
        com.sism.dto.ReportUpdateRequest updateRequest = new com.sism.dto.ReportUpdateRequest();
        updateRequest.setPercentComplete(BigDecimal.valueOf(80));
        updateRequest.setNarrative("Revised narrative after return");

        var updatedReport = reportService.updateReport(report.getReportId(), updateRequest);

        // Assert: Update should succeed
        assertThat(updatedReport.getPercentComplete()).isEqualByComparingTo(BigDecimal.valueOf(80));
        assertThat(updatedReport.getNarrative()).isEqualTo("Revised narrative after return");
        assertThat(updatedReport.getStatus()).isEqualTo(ReportStatus.RETURNED);
    }

    /**
     * Property 3.14: SUBMITTED status reports cannot be updated
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in SUBMITTED status, updating content SHALL be rejected.
     * 
     * **Validates: Requirements 3.1**
     */
    @Property(tries = 50)
    @Transactional
    void submittedReports_cannotBeUpdated(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).isNotEmpty();

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());

        // Create and submit a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());

        // Attempt to update the report content (should fail)
        com.sism.dto.ReportUpdateRequest updateRequest = new com.sism.dto.ReportUpdateRequest();
        updateRequest.setPercentComplete(BigDecimal.valueOf(90));
        updateRequest.setNarrative("Attempted update while submitted");

        assertThatThrownBy(() -> reportService.updateReport(report.getReportId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT")
                .hasMessageContaining("RETURNED");
    }

    /**
     * Property 3.15: APPROVED status reports cannot be updated
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in APPROVED status (terminal state), updating content SHALL be rejected.
     * 
     * **Validates: Requirements 3.1, 4.2**
     */
    @Property(tries = 30)
    @Transactional
    void approvedReports_cannotBeUpdated(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create, submit, and approve a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());
        
        ApprovalRequest approveRequest = new ApprovalRequest();
        approveRequest.setReportId(report.getReportId());
        approveRequest.setApproverId(approver.getId());
        approveRequest.setAction(ApprovalAction.APPROVE);
        approveRequest.setComment("Approved");
        approvalService.processApproval(approveRequest);

        // Attempt to update the report content (should fail)
        com.sism.dto.ReportUpdateRequest updateRequest = new com.sism.dto.ReportUpdateRequest();
        updateRequest.setPercentComplete(BigDecimal.valueOf(100));
        updateRequest.setNarrative("Attempted update after approval");

        assertThatThrownBy(() -> reportService.updateReport(report.getReportId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT")
                .hasMessageContaining("RETURNED");
    }

    /**
     * Property 3.16: REJECTED status reports cannot be updated
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any report in REJECTED status (terminal state), updating content SHALL be rejected.
     * 
     * **Validates: Requirements 3.1, 4.3**
     */
    @Property(tries = 30)
    @Transactional
    void rejectedReports_cannotBeUpdated(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create, submit, and reject a report
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        reportService.submitReport(report.getReportId());
        
        ApprovalRequest rejectRequest = new ApprovalRequest();
        rejectRequest.setReportId(report.getReportId());
        rejectRequest.setApproverId(approver.getId());
        rejectRequest.setAction(ApprovalAction.REJECT);
        rejectRequest.setComment("Rejected - invalid data");
        approvalService.processApproval(rejectRequest);

        // Attempt to update the report content (should fail)
        com.sism.dto.ReportUpdateRequest updateRequest = new com.sism.dto.ReportUpdateRequest();
        updateRequest.setPercentComplete(BigDecimal.valueOf(100));
        updateRequest.setNarrative("Attempted update after rejection");

        assertThatThrownBy(() -> reportService.updateReport(report.getReportId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT")
                .hasMessageContaining("RETURNED");
    }

    /**
     * Property 3.17: State machine transitions are deterministic
     * 
     * **Feature: sism-fullstack-integration, Property 3: Report Status State Machine**
     * 
     * For any valid transition, the resulting state SHALL be deterministic and consistent.
     * 
     * **Validates: Requirements 3.1, 3.2, 3.3, 4.2, 4.3, 4.4**
     */
    @Property(tries = 50)
    @Transactional
    void stateTransitions_areDeterministic(
            @ForAll("indicatorIndices") Integer indicatorIndex,
            @ForAll("userIndices") Integer userIndex) {

        List<Indicator> indicators = getExistingActiveIndicators(10);
        List<SysUser> users = getExistingUsers(5);
        
        assumeThat(indicators).isNotEmpty();
        assumeThat(users).size().isGreaterThanOrEqualTo(2);

        Indicator indicator = indicators.get(indicatorIndex % indicators.size());
        SysUser reporter = users.get(userIndex % users.size());
        SysUser approver = users.get((userIndex + 1) % users.size());

        // Create report - should always start in DRAFT
        ProgressReport report = createTestReportInDraft(indicator, reporter);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DRAFT);

        // Submit - should always go to SUBMITTED
        var submitted = reportService.submitReport(report.getReportId());
        assertThat(submitted.getStatus()).isEqualTo(ReportStatus.SUBMITTED);

        // Return - should always go to RETURNED
        ApprovalRequest returnRequest = new ApprovalRequest();
        returnRequest.setReportId(report.getReportId());
        returnRequest.setApproverId(approver.getId());
        returnRequest.setAction(ApprovalAction.RETURN);
        returnRequest.setComment("Needs revision");
        var returned = approvalService.processApproval(returnRequest);
        assertThat(returned.getStatus()).isEqualTo(ReportStatus.RETURNED);

        // Resubmit - should always go back to SUBMITTED
        var resubmitted = reportService.submitReport(report.getReportId());
        assertThat(resubmitted.getStatus()).isEqualTo(ReportStatus.SUBMITTED);

        // Approve - should always go to APPROVED
        ApprovalRequest approveRequest = new ApprovalRequest();
        approveRequest.setReportId(report.getReportId());
        approveRequest.setApproverId(approver.getId());
        approveRequest.setAction(ApprovalAction.APPROVE);
        approveRequest.setComment("Approved");
        var approved = approvalService.processApproval(approveRequest);
        assertThat(approved.getStatus()).isEqualTo(ReportStatus.APPROVED);
    }
}
