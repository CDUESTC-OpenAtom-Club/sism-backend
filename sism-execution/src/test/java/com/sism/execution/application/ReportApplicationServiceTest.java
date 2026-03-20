package com.sism.execution.application;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportApplicationServiceTest {

    @Mock
    private PlanReportRepository planReportRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private ReportApplicationService reportApplicationService;

    @BeforeEach
    void setUp() {
        reportApplicationService = new ReportApplicationService(
                planReportRepository,
                eventPublisher
        );
    }

    @Test
    void createReport_restoresDeletedReportWithSameUniqueKey() {
        PlanReport deletedReport = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        deletedReport.setId(6L);
        deletedReport.setIsDeleted(true);
        deletedReport.setStatus(PlanReport.STATUS_REJECTED);

        when(planReportRepository.findByUniqueKey(111L, "202603", ReportOrgType.FUNC_DEPT, 39L))
                .thenReturn(Optional.of(deletedReport));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport restored = reportApplicationService.createReport("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);

        assertThat(restored.getId()).isEqualTo(6L);
        assertThat(restored.getIsDeleted()).isFalse();
        assertThat(restored.getStatus()).isEqualTo(PlanReport.STATUS_DRAFT);
        assertThat(restored.getSubmittedAt()).isNull();
        verify(planReportRepository).save(deletedReport);
    }

    @Test
    void createReport_rejectsDuplicateActiveReport() {
        PlanReport activeReport = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        activeReport.setId(7L);
        activeReport.setIsDeleted(false);

        when(planReportRepository.findByUniqueKey(111L, "202603", ReportOrgType.FUNC_DEPT, 39L))
                .thenReturn(Optional.of(activeReport));

        assertThatThrownBy(() -> reportApplicationService.createReport("202603", 39L, ReportOrgType.FUNC_DEPT, 111L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("当前月份已存在报告，请勿重复创建");
    }
}
