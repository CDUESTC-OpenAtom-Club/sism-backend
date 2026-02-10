package com.sism.service;

import com.sism.dto.ReportCreateRequest;
import com.sism.dto.ReportUpdateRequest;
import com.sism.entity.SysUser;
import com.sism.entity.Indicator;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.ReportStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.ReportRepository;
import com.sism.repository.UserRepository;
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
 * Unit tests for ReportService
 * Tests progress report CRUD operations and status workflow
 * 
 * Requirements: 4.2 - Service layer unit test coverage
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private UserRepository userRepository;

    private Indicator testIndicator;
    private SysUser testReporter;

    @BeforeEach
    void setUp() {
        // Get existing test data from database
        testIndicator = indicatorRepository.findAll().stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active indicators found in test database"));

        testReporter = userRepository.findAll().stream()
                .filter(SysUser::getIsActive)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active users found in test database"));
    }

    @Nested
    @DisplayName("getReportById Tests")
    class GetReportByIdTests {

        @Test
        @DisplayName("Should return report when exists")
        void shouldReturnReportWhenExists() {
            // Given - Create a report first
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("50"));
            createRequest.setNarrative("Test report");

            ReportVO created = reportService.createReport(createRequest);

            // When
            ReportVO result = reportService.getReportById(created.getReportId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getReportId()).isEqualTo(created.getReportId());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when report not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            Long nonExistentId = 999999L;

            // When/Then
            assertThatThrownBy(() -> reportService.getReportById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ProgressReport");
        }
    }

    @Nested
    @DisplayName("createReport Tests")
    class CreateReportTests {

        @Test
        @DisplayName("Should create report in DRAFT status")
        void shouldCreateReportInDraftStatus() {
            // Given
            ReportCreateRequest request = new ReportCreateRequest();
            request.setIndicatorId(testIndicator.getIndicatorId());
            request.setReporterId(testReporter.getId());
            request.setPercentComplete(new BigDecimal("25"));
            request.setNarrative("Test report narrative");
            request.setAchievedMilestone(false);

            // When
            ReportVO result = reportService.createReport(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getReportId()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ReportStatus.DRAFT);
            assertThat(result.getIsFinal()).isFalse();
            assertThat(result.getVersionNo()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw exception when indicator not found")
        void shouldThrowExceptionWhenIndicatorNotFound() {
            // Given
            ReportCreateRequest request = new ReportCreateRequest();
            request.setIndicatorId(999999L);
            request.setReporterId(testReporter.getId());
            request.setPercentComplete(new BigDecimal("50"));

            // When/Then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Indicator");
        }

        @Test
        @DisplayName("Should throw exception when reporter not found")
        void shouldThrowExceptionWhenReporterNotFound() {
            // Given
            ReportCreateRequest request = new ReportCreateRequest();
            request.setIndicatorId(testIndicator.getIndicatorId());
            request.setReporterId(999999L);
            request.setPercentComplete(new BigDecimal("50"));

            // When/Then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }

        @Test
        @DisplayName("Should throw exception when both milestone and adhocTask are set")
        void shouldThrowExceptionWhenBothMilestoneAndAdhocTaskSet() {
            // Given
            ReportCreateRequest request = new ReportCreateRequest();
            request.setIndicatorId(testIndicator.getIndicatorId());
            request.setReporterId(testReporter.getId());
            request.setPercentComplete(new BigDecimal("50"));
            request.setMilestoneId(1L);
            request.setAdhocTaskId(1L);

            // When/Then
            assertThatThrownBy(() -> reportService.createReport(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("simultaneously");
        }
    }

    @Nested
    @DisplayName("updateReport Tests")
    class UpdateReportTests {

        @Test
        @DisplayName("Should update report in DRAFT status")
        void shouldUpdateReportInDraftStatus() {
            // Given - Create a report first
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("25"));
            createRequest.setNarrative("Original narrative");

            ReportVO created = reportService.createReport(createRequest);

            ReportUpdateRequest updateRequest = new ReportUpdateRequest();
            updateRequest.setPercentComplete(new BigDecimal("75"));
            updateRequest.setNarrative("Updated narrative");

            // When
            ReportVO result = reportService.updateReport(created.getReportId(), updateRequest);

            // Then
            assertThat(result.getPercentComplete()).isEqualByComparingTo(new BigDecimal("75"));
            assertThat(result.getNarrative()).isEqualTo("Updated narrative");
        }

        @Test
        @DisplayName("Should throw exception when updating non-DRAFT report")
        void shouldThrowExceptionWhenUpdatingNonDraftReport() {
            // Given - Create and submit a report
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            ReportVO created = reportService.createReport(createRequest);
            reportService.submitReport(created.getReportId());

            ReportUpdateRequest updateRequest = new ReportUpdateRequest();
            updateRequest.setNarrative("Should fail");

            // When/Then
            assertThatThrownBy(() -> reportService.updateReport(created.getReportId(), updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DRAFT");
        }
    }

    @Nested
    @DisplayName("submitReport Tests")
    class SubmitReportTests {

        @Test
        @DisplayName("Should submit DRAFT report")
        void shouldSubmitDraftReport() {
            // Given
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            ReportVO created = reportService.createReport(createRequest);

            // When
            ReportVO result = reportService.submitReport(created.getReportId());

            // Then
            assertThat(result.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
            assertThat(result.getReportedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when submitting non-DRAFT report")
        void shouldThrowExceptionWhenSubmittingNonDraftReport() {
            // Given - Create, submit, then try to submit again
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            ReportVO created = reportService.createReport(createRequest);
            reportService.submitReport(created.getReportId());

            // When/Then
            assertThatThrownBy(() -> reportService.submitReport(created.getReportId()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("withdrawReport Tests")
    class WithdrawReportTests {

        @Test
        @DisplayName("Should withdraw SUBMITTED report")
        void shouldWithdrawSubmittedReport() {
            // Given
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            ReportVO created = reportService.createReport(createRequest);
            reportService.submitReport(created.getReportId());

            // When
            ReportVO result = reportService.withdrawReport(created.getReportId());

            // Then
            assertThat(result.getStatus()).isEqualTo(ReportStatus.DRAFT);
            assertThat(result.getReportedAt()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when withdrawing non-SUBMITTED report")
        void shouldThrowExceptionWhenWithdrawingNonSubmittedReport() {
            // Given - Create a DRAFT report
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            ReportVO created = reportService.createReport(createRequest);

            // When/Then
            assertThatThrownBy(() -> reportService.withdrawReport(created.getReportId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("SUBMITTED");
        }
    }

    @Nested
    @DisplayName("getReportsByIndicatorId Tests")
    class GetReportsByIndicatorIdTests {

        @Test
        @DisplayName("Should return reports for indicator")
        void shouldReturnReportsForIndicator() {
            // Given - Create a report
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            reportService.createReport(createRequest);

            // When
            List<ReportVO> result = reportService.getReportsByIndicatorId(testIndicator.getIndicatorId());

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(r -> r.getIndicatorId().equals(testIndicator.getIndicatorId()));
        }
    }

    @Nested
    @DisplayName("getReportsByStatus Tests")
    class GetReportsByStatusTests {

        @Test
        @DisplayName("Should return reports with specified status")
        void shouldReturnReportsWithStatus() {
            // Given - Create a DRAFT report
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            reportService.createReport(createRequest);

            // When
            List<ReportVO> result = reportService.getReportsByStatus(ReportStatus.DRAFT);

            // Then
            assertThat(result).allMatch(r -> r.getStatus() == ReportStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("getReportsByReporterId Tests")
    class GetReportsByReporterIdTests {

        @Test
        @DisplayName("Should return reports by reporter")
        void shouldReturnReportsByReporter() {
            // Given - Create a report
            ReportCreateRequest createRequest = new ReportCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setReporterId(testReporter.getId());
            createRequest.setPercentComplete(new BigDecimal("50"));

            reportService.createReport(createRequest);

            // When
            List<ReportVO> result = reportService.getReportsByReporterId(testReporter.getId());

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(r -> r.getReporterId().equals(testReporter.getId()));
        }
    }

    @Nested
    @DisplayName("validateMutualExclusion Tests")
    class ValidateMutualExclusionTests {

        @Test
        @DisplayName("Should pass when only milestone is set")
        void shouldPassWhenOnlyMilestoneSet() {
            // When/Then - Should not throw
            assertThatCode(() -> reportService.validateMutualExclusion(1L, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass when only adhocTask is set")
        void shouldPassWhenOnlyAdhocTaskSet() {
            // When/Then - Should not throw
            assertThatCode(() -> reportService.validateMutualExclusion(null, 1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass when both are null")
        void shouldPassWhenBothNull() {
            // When/Then - Should not throw
            assertThatCode(() -> reportService.validateMutualExclusion(null, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when both are set")
        void shouldThrowWhenBothSet() {
            // When/Then
            assertThatThrownBy(() -> reportService.validateMutualExclusion(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("simultaneously");
        }
    }
}
