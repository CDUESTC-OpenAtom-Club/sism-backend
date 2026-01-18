package com.sism.service;

import com.sism.dto.ApprovalRequest;
import com.sism.dto.ReportCreateRequest;
import com.sism.entity.AppUser;
import com.sism.entity.Indicator;
import com.sism.enums.ApprovalAction;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.ReportStatus;
import com.sism.exception.BusinessException;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.UserRepository;
import com.sism.vo.ApprovalRecordVO;
import com.sism.vo.ReportVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ApprovalService
 * Tests approval workflow operations
 * 
 * Requirements: 4.2 - Service layer unit test coverage
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApprovalServiceTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private UserRepository userRepository;

    private Indicator testIndicator;
    private AppUser testReporter;
    private AppUser testApprover;

    @BeforeEach
    void setUp() {
        // Get existing test data from database
        testIndicator = indicatorRepository.findAll().stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active indicators found in test database"));

        List<AppUser> users = userRepository.findAll().stream()
                .filter(AppUser::getIsActive)
                .toList();
        
        assertThat(users).hasSizeGreaterThanOrEqualTo(2);
        testReporter = users.get(0);
        testApprover = users.get(1);
    }

    private ReportVO createAndSubmitReport() {
        ReportCreateRequest createRequest = new ReportCreateRequest();
        createRequest.setIndicatorId(testIndicator.getIndicatorId());
        createRequest.setReporterId(testReporter.getUserId());
        createRequest.setPercentComplete(new BigDecimal("50"));
        createRequest.setNarrative("Test report for approval");

        ReportVO created = reportService.createReport(createRequest);
        return reportService.submitReport(created.getReportId());
    }

    @Nested
    @DisplayName("getPendingApprovalReports Tests")
    class GetPendingApprovalReportsTests {

        @Test
        @DisplayName("Should return submitted reports")
        void shouldReturnSubmittedReports() {
            // Given - Create and submit a report
            createAndSubmitReport();

            // When
            List<ReportVO> result = approvalService.getPendingApprovalReports();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(r -> r.getStatus() == ReportStatus.SUBMITTED);
        }
    }

    @Nested
    @DisplayName("processApproval - APPROVE Tests")
    class ProcessApprovalApproveTests {

        @Test
        @DisplayName("Should approve submitted report")
        void shouldApproveSubmittedReport() {
            // Given
            ReportVO submitted = createAndSubmitReport();

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(submitted.getReportId());
            request.setApproverId(testApprover.getUserId());
            request.setAction(ApprovalAction.APPROVE);
            request.setComment("Approved");

            // When
            ReportVO result = approvalService.processApproval(request);

            // Then
            assertThat(result.getStatus()).isEqualTo(ReportStatus.APPROVED);
            assertThat(result.getIsFinal()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when approving non-SUBMITTED report")
        void shouldThrowExceptionWhenApprovingNonSubmittedReport() {
            // Given - Create a DRAFT report (not submitted)
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getUserId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            ReportVO draft = reportService.createReport(createRequest);

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(draft.getReportId());
            request.setApproverId(testApprover.getUserId());
            request.setAction(ApprovalAction.APPROVE);

            // When/Then
            assertThatThrownBy(() -> approvalService.processApproval(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("SUBMITTED");
        }
    }

    @Nested
    @DisplayName("processApproval - REJECT Tests")
    class ProcessApprovalRejectTests {

        @Test
        @DisplayName("Should reject submitted report with comment")
        void shouldRejectSubmittedReportWithComment() {
            // Given
            ReportVO submitted = createAndSubmitReport();

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(submitted.getReportId());
            request.setApproverId(testApprover.getUserId());
            request.setAction(ApprovalAction.REJECT);
            request.setComment("Rejected - incomplete data");

            // When
            ReportVO result = approvalService.processApproval(request);

            // Then
            assertThat(result.getStatus()).isEqualTo(ReportStatus.REJECTED);
        }

        @Test
        @DisplayName("Should throw exception when rejecting without comment")
        void shouldThrowExceptionWhenRejectingWithoutComment() {
            // Given
            ReportVO submitted = createAndSubmitReport();

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(submitted.getReportId());
            request.setApproverId(testApprover.getUserId());
            request.setAction(ApprovalAction.REJECT);
            request.setComment(null);

            // When/Then
            assertThatThrownBy(() -> approvalService.processApproval(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Comment is required");
        }

        @Test
        @DisplayName("Should throw exception when rejecting with empty comment")
        void shouldThrowExceptionWhenRejectingWithEmptyComment() {
            // Given
            ReportVO submitted = createAndSubmitReport();

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(submitted.getReportId());
            request.setApproverId(testApprover.getUserId());
            request.setAction(ApprovalAction.REJECT);
            request.setComment("   ");

            // When/Then
            assertThatThrownBy(() -> approvalService.processApproval(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Comment is required");
        }
    }

    @Nested
    @DisplayName("processApproval - RETURN Tests")
    class ProcessApprovalReturnTests {

        @Test
        @DisplayName("Should return submitted report with comment")
        void shouldReturnSubmittedReportWithComment() {
            // Given
            ReportVO submitted = createAndSubmitReport();

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(submitted.getReportId());
            request.setApproverId(testApprover.getUserId());
            request.setAction(ApprovalAction.RETURN);
            request.setComment("Please revise the narrative");

            // When
            ReportVO result = approvalService.processApproval(request);

            // Then
            assertThat(result.getStatus()).isEqualTo(ReportStatus.RETURNED);
        }

        @Test
        @DisplayName("Should throw exception when returning without comment")
        void shouldThrowExceptionWhenReturningWithoutComment() {
            // Given
            ReportVO submitted = createAndSubmitReport();

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(submitted.getReportId());
            request.setApproverId(testApprover.getUserId());
            request.setAction(ApprovalAction.RETURN);
            request.setComment(null);

            // When/Then
            assertThatThrownBy(() -> approvalService.processApproval(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Comment is required");
        }
    }

    @Nested
    @DisplayName("getApprovalRecordsByReportId Tests")
    class GetApprovalRecordsByReportIdTests {

        @Test
        @DisplayName("Should return approval records for report")
        void shouldReturnApprovalRecordsForReport() {
            // Given - Create, submit, and approve a report
            ReportVO submitted = createAndSubmitReport();

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(submitted.getReportId());
            request.setApproverId(testApprover.getUserId());
            request.setAction(ApprovalAction.APPROVE);
            request.setComment("Approved");

            approvalService.processApproval(request);

            // When
            List<ApprovalRecordVO> result = approvalService.getApprovalRecordsByReportId(submitted.getReportId());

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).anyMatch(r -> r.getAction() == ApprovalAction.APPROVE);
        }

        @Test
        @DisplayName("Should return empty list for report with no approvals")
        void shouldReturnEmptyListForReportWithNoApprovals() {
            // Given - Create a report but don't approve it
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getUserId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            ReportVO created = reportService.createReport(createRequest);

            // When
            List<ApprovalRecordVO> result = approvalService.getApprovalRecordsByReportId(created.getReportId());

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Returned Report Resubmission Tests")
    class ReturnedReportResubmissionTests {

        @Test
        @DisplayName("Should allow resubmission of returned report")
        void shouldAllowResubmissionOfReturnedReport() {
            // Given - Create, submit, and return a report
            ReportVO submitted = createAndSubmitReport();

            ApprovalRequest returnRequest = new ApprovalRequest();
            returnRequest.setReportId(submitted.getReportId());
            returnRequest.setApproverId(testApprover.getUserId());
            returnRequest.setAction(ApprovalAction.RETURN);
            returnRequest.setComment("Please revise");

            approvalService.processApproval(returnRequest);

            // When - Resubmit the report
            ReportVO resubmitted = reportService.submitReport(submitted.getReportId());

            // Then
            assertThat(resubmitted.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        }

        @Test
        @DisplayName("Should allow update of returned report")
        void shouldAllowUpdateOfReturnedReport() {
            // Given - Create, submit, and return a report
            ReportVO submitted = createAndSubmitReport();

            ApprovalRequest returnRequest = new ApprovalRequest();
            returnRequest.setReportId(submitted.getReportId());
            returnRequest.setApproverId(testApprover.getUserId());
            returnRequest.setAction(ApprovalAction.RETURN);
            returnRequest.setComment("Please revise");

            approvalService.processApproval(returnRequest);

            // When - Update the returned report
            com.sism.dto.ReportUpdateRequest updateRequest = new com.sism.dto.ReportUpdateRequest();
            updateRequest.setNarrative("Revised narrative");

            ReportVO updated = reportService.updateReport(submitted.getReportId(), updateRequest);

            // Then
            assertThat(updated.getNarrative()).isEqualTo("Revised narrative");
            assertThat(updated.getStatus()).isEqualTo(ReportStatus.RETURNED);
        }
    }
}
