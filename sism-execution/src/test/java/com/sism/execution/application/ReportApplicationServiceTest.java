package com.sism.execution.application;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportIndicatorRepository;
import com.sism.execution.domain.repository.PlanReportIndicatorSnapshot;
import com.sism.execution.domain.repository.PlanReportRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportApplicationServiceTest {

    @Mock
    private PlanReportRepository planReportRepository;

    @Mock
    private PlanReportIndicatorRepository planReportIndicatorRepository;

    @Mock
    private IndicatorRepository indicatorRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private ReportApplicationService reportApplicationService;

    @BeforeEach
    void setUp() {
        reportApplicationService = new ReportApplicationService(
                planReportRepository,
                planReportIndicatorRepository,
                indicatorRepository,
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

    @Test
    void attachAuditInstance_shouldPersistRuntimeLink() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(8L);

        when(planReportRepository.findById(8L)).thenReturn(Optional.of(report));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport updated = reportApplicationService.attachAuditInstance(8L, 901L);

        assertThat(updated.getAuditInstanceId()).isEqualTo(901L);
        verify(planReportRepository).save(report);
    }

    @Test
    void markWorkflowRejected_shouldUpdateStatusWithoutPublishingEvents() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(9L);
        report.setStatus(PlanReport.STATUS_SUBMITTED);

        when(planReportRepository.findById(9L)).thenReturn(Optional.of(report));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport updated = reportApplicationService.markWorkflowRejected(9L, 33L, "退回修改");

        assertThat(updated.getStatus()).isEqualTo(PlanReport.STATUS_REJECTED);
        assertThat(updated.getRejectionReason()).isEqualTo("退回修改");
        verify(planReportRepository).save(report);
    }

    @Test
    void updateReport_shouldPersistIndicatorDraftDetailWhenIndicatorIdProvided() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(12L);

        when(planReportRepository.findById(12L)).thenReturn(Optional.of(report));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport updated = reportApplicationService.updateReport(
                12L,
                "指标 A",
                2001L,
                "本月推进完成",
                "本月推进完成",
                45,
                "本月推进完成",
                "本月推进完成",
                null
        );

        assertThat(updated.getId()).isEqualTo(12L);
        verify(planReportIndicatorRepository)
                .upsertDraftIndicator(12L, 2001L, 45, "本月推进完成", null);
    }

    @Test
    void markWorkflowApproved_shouldSyncIndicatorProgressFromReportDetails() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(13L);
        report.setStatus(PlanReport.STATUS_SUBMITTED);

        Indicator indicator = new Indicator();
        indicator.setId(2001L);
        indicator.setProgress(0);

        when(planReportRepository.findById(13L)).thenReturn(Optional.of(report));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(planReportIndicatorRepository.findByReportId(13L))
                .thenReturn(List.of(new PlanReportIndicatorSnapshot(2001L, 67, "审批通过备注", null)));
        when(indicatorRepository.findById(2001L)).thenReturn(Optional.of(indicator));
        when(indicatorRepository.save(any(Indicator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport updated = reportApplicationService.markWorkflowApproved(13L, 33L);

        assertThat(updated.getStatus()).isEqualTo(PlanReport.STATUS_APPROVED);
        assertThat(indicator.getProgress()).isEqualTo(67);
        verify(indicatorRepository).save(indicator);
    }

    @Test
    void approveReport_shouldSyncIndicatorProgressFromReportDetails() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(15L);
        report.setStatus(PlanReport.STATUS_SUBMITTED);

        Indicator indicator = new Indicator();
        indicator.setId(2002L);
        indicator.setProgress(0);

        when(planReportRepository.findById(15L)).thenReturn(Optional.of(report));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(planReportIndicatorRepository.findByReportId(15L))
                .thenReturn(List.of(new PlanReportIndicatorSnapshot(2002L, 20, "审批完成", null)));
        when(indicatorRepository.findById(2002L)).thenReturn(Optional.of(indicator));
        when(indicatorRepository.save(any(Indicator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport updated = reportApplicationService.approveReport(15L, 33L);

        assertThat(updated.getStatus()).isEqualTo(PlanReport.STATUS_APPROVED);
        assertThat(indicator.getProgress()).isEqualTo(20);
        verify(indicatorRepository).save(indicator);
    }

    @Test
    void updateReport_shouldNotPersistIndicatorDetailWhenIndicatorIdMissing() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(14L);

        when(planReportRepository.findById(14L)).thenReturn(Optional.of(report));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportApplicationService.updateReport(14L, "纯报表", null, 20, "问题", "计划");

        verify(planReportIndicatorRepository, never())
                .upsertDraftIndicator(any(), any(), any(), any(), any());
    }
}
