package com.sism.analytics.application;

import com.sism.analytics.domain.Report;
import com.sism.analytics.infrastructure.repository.ReportRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportApplicationService Tests")
class ReportApplicationServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private EventStore eventStore;

    private ReportApplicationService reportApplicationService;

    @BeforeEach
    void setUp() {
        reportApplicationService = new ReportApplicationService(reportRepository, eventPublisher, eventStore);
    }

    @Test
    @DisplayName("updateReport should reject non-owner access")
    void updateReportShouldRejectNonOwnerAccess() {
        Report report = Report.create("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, null, null);
        report.setId(99L);
        when(reportRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.of(report));

        assertThrows(AccessDeniedException.class, () ->
                reportApplicationService.updateReport(99L, 2L, "新名称", Report.TYPE_EXECUTION, Report.FORMAT_EXCEL, "新描述")
        );
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    @DisplayName("findReportById should hide other users report")
    void findReportByIdShouldHideOtherUsersReport() {
        Report report = Report.create("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, null, null);
        report.setId(100L);
        when(reportRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(report));

        assertEquals(Optional.empty(), reportApplicationService.findReportById(100L, 2L));
    }

    @Test
    @DisplayName("findReportsByGeneratedBy should reject other users request")
    void findReportsByGeneratedByShouldRejectOtherUsersRequest() {
        assertThrows(AccessDeniedException.class, () ->
                reportApplicationService.findReportsByGeneratedBy(2L, 1L)
        );
        verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("countReportsByGeneratedBy should reject other users request")
    void countReportsByGeneratedByShouldRejectOtherUsersRequest() {
        assertThrows(AccessDeniedException.class, () ->
                reportApplicationService.countReportsByGeneratedBy(2L, 1L)
        );
        verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("findReportsByDateRange should reject invalid date range")
    void findReportsByDateRangeShouldRejectInvalidDateRange() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 6, 12, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 5, 12, 0);

        assertThrows(IllegalArgumentException.class, () ->
                reportApplicationService.findReportsByDateRange(start, end, 1L)
        );
        verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("searchReportsByName should escape SQL LIKE wildcards")
    void searchReportsByNameShouldEscapeLikeWildcards() {
        reportApplicationService.searchReportsByName("100%_done", 1L);

        verify(reportRepository).findByGeneratedByAndNameContainingAndNotDeleted(1L, "100\\%\\_done");
    }
}
