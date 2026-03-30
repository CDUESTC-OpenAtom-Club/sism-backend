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
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ReportApplicationService reportApplicationService;

    @BeforeEach
    void setUp() {
        reportApplicationService = new ReportApplicationService(
                planReportRepository,
                planReportIndicatorRepository,
                indicatorRepository,
                eventPublisher,
                jdbcTemplate
        );
    }

    @Test
    void createReport_restoresLatestDeletedReportInMonthlyScope() {
        PlanReport deletedReport = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        deletedReport.setId(6L);
        deletedReport.setIsDeleted(true);
        deletedReport.setStatus(PlanReport.STATUS_REJECTED);

        when(planReportRepository.findLatestByMonthlyScope(111L, "202603", ReportOrgType.FUNC_DEPT, 39L))
                .thenReturn(Optional.of(deletedReport));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport restored = reportApplicationService.createReport("202603", 39L, ReportOrgType.FUNC_DEPT, 111L, 501L);

        assertThat(restored.getId()).isEqualTo(6L);
        assertThat(restored.getIsDeleted()).isFalse();
        assertThat(restored.getStatus()).isEqualTo(PlanReport.STATUS_DRAFT);
        assertThat(restored.getSubmittedAt()).isNull();
        assertThat(restored.getCreatedBy()).isEqualTo(501L);
        verify(planReportRepository).save(deletedReport);
    }

    @Test
    void createReport_reusesLatestDraftReportInMonthlyScope() {
        PlanReport activeReport = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        activeReport.setId(7L);
        activeReport.setIsDeleted(false);

        when(planReportRepository.findLatestByMonthlyScope(111L, "202603", ReportOrgType.FUNC_DEPT, 39L))
                .thenReturn(Optional.of(activeReport));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport reused = reportApplicationService.createReport("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);

        assertThat(reused.getId()).isEqualTo(7L);
        verify(planReportRepository).save(activeReport);
    }

    @Test
    void createReport_shouldPersistFirstFiller() {
        when(planReportRepository.findLatestByMonthlyScope(111L, "202603", ReportOrgType.FUNC_DEPT, 39L))
                .thenReturn(Optional.empty());
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport created = reportApplicationService.createReport("202603", 39L, ReportOrgType.FUNC_DEPT, 111L, 7001L);

        assertThat(created.getCreatedBy()).isEqualTo(7001L);
    }

    @Test
    void createReport_createsNewRoundWhenLatestMonthlyReportIsApproved() {
        PlanReport approvedReport = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        approvedReport.setId(17L);
        approvedReport.setStatus(PlanReport.STATUS_APPROVED);

        when(planReportRepository.findLatestByMonthlyScope(111L, "202603", ReportOrgType.FUNC_DEPT, 39L))
                .thenReturn(Optional.of(approvedReport));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> {
            PlanReport report = invocation.getArgument(0);
            report.setId(27L);
            return report;
        });

        PlanReport created = reportApplicationService.createReport("202603", 39L, ReportOrgType.FUNC_DEPT, 111L, 7002L);

        assertThat(created.getId()).isEqualTo(27L);
        assertThat(created.getStatus()).isEqualTo(PlanReport.STATUS_DRAFT);
        assertThat(created.getCreatedBy()).isEqualTo(7002L);
        assertThat(created).isNotSameAs(approvedReport);
        verify(planReportRepository).save(any(PlanReport.class));
    }

    @Test
    void createReport_createsNewRoundWhenLatestMonthlyReportIsRejected() {
        PlanReport rejectedReport = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        rejectedReport.setId(19L);
        rejectedReport.setStatus(PlanReport.STATUS_REJECTED);

        when(planReportRepository.findLatestByMonthlyScope(111L, "202603", ReportOrgType.FUNC_DEPT, 39L))
                .thenReturn(Optional.of(rejectedReport));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> {
            PlanReport report = invocation.getArgument(0);
            report.setId(29L);
            return report;
        });

        PlanReport created = reportApplicationService.createReport("202603", 39L, ReportOrgType.FUNC_DEPT, 111L, 7003L);

        assertThat(created.getId()).isEqualTo(29L);
        assertThat(created.getStatus()).isEqualTo(PlanReport.STATUS_DRAFT);
        assertThat(created.getCreatedBy()).isEqualTo(7003L);
        assertThat(created).isNotSameAs(rejectedReport);
        verify(planReportRepository).save(any(PlanReport.class));
    }

    @Test
    void createReport_rejectsNewRoundWhenLatestMonthlyReportIsInReview() {
        PlanReport inReviewReport = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        inReviewReport.setId(18L);
        inReviewReport.setStatus(PlanReport.STATUS_SUBMITTED);

        when(planReportRepository.findLatestByMonthlyScope(111L, "202603", ReportOrgType.FUNC_DEPT, 39L))
                .thenReturn(Optional.of(inReviewReport));

        assertThatThrownBy(() -> reportApplicationService.createReport("202603", 39L, ReportOrgType.FUNC_DEPT, 111L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("当前月份已有报告正在审批中，请等待审批完成或先撤回");
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
    void markWorkflowWithdrawn_shouldResetDraftStateAndClearAuditLink() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(19L);
        report.setStatus(PlanReport.STATUS_SUBMITTED);
        report.setAuditInstanceId(901L);
        report.setSubmittedAt(java.time.LocalDateTime.now());

        when(planReportRepository.findById(19L)).thenReturn(Optional.of(report));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanReport updated = reportApplicationService.markWorkflowWithdrawn(19L);

        assertThat(updated.getStatus()).isEqualTo(PlanReport.STATUS_DRAFT);
        assertThat(updated.getAuditInstanceId()).isNull();
        assertThat(updated.getSubmittedAt()).isNull();
    }

    @Test
    void updateReport_shouldPersistIndicatorDraftDetailWhenIndicatorIdProvided() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(12L);
        Indicator indicator = new Indicator();
        indicator.setId(2001L);
        indicator.setProgress(20);

        when(planReportRepository.findById(12L)).thenReturn(Optional.of(report));
        when(planReportRepository.save(any(PlanReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(indicatorRepository.findById(2001L)).thenReturn(Optional.of(indicator));

        PlanReport updated = reportApplicationService.updateReport(
                12L,
                "指标 A",
                2001L,
                "本月推进完成",
                "本月推进完成",
                45,
                "本月推进完成",
                "本月推进完成",
                null,
                8001L
        );

        assertThat(updated.getId()).isEqualTo(12L);
        assertThat(updated.getCreatedBy()).isEqualTo(8001L);
        verify(planReportIndicatorRepository)
                .upsertDraftIndicator(12L, 2001L, 45, "本月推进完成", null);
    }

    @Test
    void updateReport_shouldRejectProgressNotGreaterThanActualIndicatorProgress() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(18L);
        Indicator indicator = new Indicator();
        indicator.setId(2008L);
        indicator.setProgress(30);

        when(planReportRepository.findById(18L)).thenReturn(Optional.of(report));
        when(indicatorRepository.findById(2008L)).thenReturn(Optional.of(indicator));

        assertThatThrownBy(() -> reportApplicationService.updateReport(
                18L,
                "指标 B",
                2008L,
                "进度未增长",
                "进度未增长",
                30,
                "进度未增长",
                "进度未增长",
                null,
                9001L
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("填报进度必须大于真实进度，当前真实进度为 30%");

        verify(planReportRepository, never()).save(any(PlanReport.class));
        verify(planReportIndicatorRepository, never())
                .upsertDraftIndicator(any(), any(), any(), any(), any());
    }

    @Test
    void findReportById_shouldHydrateIndicatorDetailsFromDraftTable() {
        PlanReport report = PlanReport.createDraft("202603", 39L, ReportOrgType.FUNC_DEPT, 111L);
        report.setId(16L);

        when(planReportRepository.findById(16L)).thenReturn(Optional.of(report));
        when(planReportIndicatorRepository.findByReportId(16L))
                .thenReturn(List.of(new PlanReportIndicatorSnapshot(2003L, 15, "已填报草稿", "里程碑一")));

        PlanReport hydrated = reportApplicationService.findReportById(16L).orElseThrow();

        assertThat(hydrated.getIndicatorDetails()).hasSize(1);
        assertThat(hydrated.getIndicatorDetails().get(0).indicatorId()).isEqualTo(2003L);
        assertThat(hydrated.getIndicatorDetails().get(0).progress()).isEqualTo(15);
        assertThat(hydrated.getIndicatorDetails().get(0).comment()).isEqualTo("已填报草稿");
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
