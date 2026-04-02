package com.sism.execution.application;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportIndicatorRepository;
import com.sism.execution.domain.repository.PlanReportRepository;
import com.sism.execution.interfaces.dto.PlanReportQueryRequest;
import com.sism.execution.interfaces.dto.UpdatePlanReportIndicatorDetailRequest;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ReportApplicationService - 计划报告应用服务
 * 处理计划报告的业务逻辑
 */
@Service("executionReportApplicationService")
@RequiredArgsConstructor
@Slf4j
public class ReportApplicationService {

    private final PlanReportRepository planReportRepository;
    private final PlanReportIndicatorRepository planReportIndicatorRepository;
    private final IndicatorRepository indicatorRepository;
    private final DomainEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建报告（草稿）
     */
    @Transactional
    public PlanReport createReport(String reportMonth, Long reportOrgId,
                                   ReportOrgType reportOrgType, Long planId) {
        return createReport(reportMonth, reportOrgId, reportOrgType, planId, null);
    }

    /**
     * 创建报告（草稿）
     */
    @Transactional
    public PlanReport createReport(String reportMonth, Long reportOrgId,
                                   ReportOrgType reportOrgType, Long planId, Long createdBy) {
        Optional<PlanReport> existingReport = planReportRepository.findLatestByMonthlyScope(
                planId, reportMonth, reportOrgType, reportOrgId);
        PlanReport previousRound = null;

        if (existingReport.isPresent()) {
            PlanReport report = existingReport.get();

            if (Boolean.TRUE.equals(report.getIsDeleted())) {
                report.setIsDeleted(false);
                report.setStatus(PlanReport.STATUS_DRAFT);
                report.setSubmittedAt(null);
                report.markCreatedByIfAbsent(createdBy);
                report.setUpdatedAt(LocalDateTime.now());
                report.validate();
                return enrichReportMetadata(planReportRepository.save(report));
            }

            if (PlanReport.STATUS_DRAFT.equals(report.getStatus())) {
                report.markCreatedByIfAbsent(createdBy);
                report.setUpdatedAt(LocalDateTime.now());
                report.validate();
                return enrichReportMetadata(planReportRepository.save(report));
            }

            if (PlanReport.STATUS_REJECTED.equals(report.getStatus())) {
                report.setStatus(PlanReport.STATUS_DRAFT);
                report.setSubmittedAt(null);
                report.markCreatedByIfAbsent(createdBy);
                report.setUpdatedAt(LocalDateTime.now());
                report.validate();
                return enrichReportMetadata(planReportRepository.save(report));
            }

            if (PlanReport.STATUS_SUBMITTED.equals(report.getStatus())) {
                throw new IllegalStateException("当前月份已有报告正在审批中，请等待审批完成或先撤回");
            }

            previousRound = report;
        }

        PlanReport report = PlanReport.createDraft(
                reportMonth, reportOrgId, reportOrgType, planId, createdBy);
        report.validate();
        PlanReport savedReport = enrichReportMetadata(planReportRepository.save(report));

        if (previousRound != null) {
            String previousStatus = String.valueOf(previousRound.getStatus()).trim().toUpperCase();
            if ("APPROVED".equals(previousStatus) || "REJECTED".equals(previousStatus)) {
                log.info(
                        "Created new monthly report round from historical report: previousReportId={}, previousStatus={}, newReportId={}, planId={}, reportMonth={}, reportOrgId={}, reportOrgType={}, createdBy={}",
                        previousRound.getId(),
                        previousStatus,
                        savedReport.getId(),
                        planId,
                        reportMonth,
                        reportOrgId,
                        reportOrgType,
                        createdBy
                );
            }
        }

        return savedReport;
    }

    /**
     * 更新报告内容（包含标题）
     */
    @Transactional
    public PlanReport updateReport(Long reportId, String title, Long indicatorId, String content, String summary, Integer progress,
                                   String issues, String nextPlan, String milestoneNote, Long operatorUserId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        validatePendingProgress(indicatorId, progress);
        report.markCreatedByIfAbsent(operatorUserId);
        report.updateContent(content, summary, progress, issues, nextPlan);
        if (title != null) {
            report.setTitle(title);
        }
        PlanReport savedReport = planReportRepository.save(report);
        if (indicatorId != null) {
            Long planReportIndicatorId = planReportIndicatorRepository.upsertDraftIndicator(
                    savedReport.getId(),
                    indicatorId,
                    progress,
                    content,
                    milestoneNote
            );
            // Single-indicator legacy update path currently has no attachment payload.
            planReportIndicatorRepository.attachFiles(planReportIndicatorId, List.of(), operatorUserId);
        }
        return enrichReportMetadata(savedReport);
    }

    @Transactional
    public PlanReport updateReportBatch(Long reportId,
                                        String title,
                                        String content,
                                        String summary,
                                        Integer progress,
                                        String issues,
                                        String nextPlan,
                                        Long operatorUserId,
                                        List<UpdatePlanReportIndicatorDetailRequest> indicatorDetails) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.markCreatedByIfAbsent(operatorUserId);
        report.updateContent(content, summary, progress, issues, nextPlan);
        if (title != null) {
            report.setTitle(title);
        }
        PlanReport savedReport = planReportRepository.save(report);

        List<UpdatePlanReportIndicatorDetailRequest> normalizedDetails = indicatorDetails == null
                ? List.of()
                : indicatorDetails.stream()
                .filter(Objects::nonNull)
                .toList();

        for (UpdatePlanReportIndicatorDetailRequest detail : normalizedDetails) {
            validatePendingProgress(detail.getIndicatorId(), detail.getProgress());
            Long planReportIndicatorId = planReportIndicatorRepository.upsertDraftIndicator(
                    savedReport.getId(),
                    detail.getIndicatorId(),
                    detail.getProgress(),
                    detail.getContent(),
                    detail.getMilestoneNote()
            );
            planReportIndicatorRepository.attachFiles(
                    planReportIndicatorId,
                    detail.getAttachmentIds(),
                    operatorUserId
            );
        }

        return enrichReportMetadata(savedReport);
    }

    private void validatePendingProgress(Long indicatorId, Integer progress) {
        if (indicatorId == null || progress == null) {
            return;
        }

        Indicator indicator = indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + indicatorId));
        int currentProgress = indicator.getProgress() == null ? 0 : indicator.getProgress();
        if (progress <= currentProgress) {
            throw new IllegalArgumentException(
                    "填报进度必须大于真实进度，当前真实进度为 " + currentProgress + "%"
            );
        }
    }

    /**
     * 更新报告内容（旧方法，保持向后兼容）
     */
    @Transactional
    public PlanReport updateReport(Long reportId, String content, String summary, Integer progress,
                                   String issues, String nextPlan) {
        return updateReport(reportId, null, null, content, summary, progress, issues, nextPlan, null, null);
    }

    /**
     * 提交报告
     */
    @Transactional
    public PlanReport submitReport(Long reportId, Long userId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.submit(userId);
        report = planReportRepository.save(report);
        publishAndSaveEvents(report);
        return enrichReportMetadata(report);
    }

    @Transactional
    public PlanReport attachAuditInstance(Long reportId, Long auditInstanceId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        report.setAuditInstanceId(auditInstanceId);
        report.setUpdatedAt(LocalDateTime.now());
        return enrichReportMetadata(planReportRepository.save(report));
    }

    /**
     * 审批通过报告
     */
    @Transactional
    public PlanReport approveReport(Long reportId, Long userId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.approve(userId);
        report = planReportRepository.save(report);
        syncApprovedIndicatorProgress(report.getId());
        publishAndSaveEvents(report);
        return enrichReportMetadata(report);
    }

    /**
     * 驳回报告
     */
    @Transactional
    public PlanReport rejectReport(Long reportId, Long userId, String reason) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.reject(userId, reason);
        report = planReportRepository.save(report);
        publishAndSaveEvents(report);
        return enrichReportMetadata(report);
    }

    @Transactional
    public PlanReport markWorkflowApproved(Long reportId, Long approverId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        if (!PlanReport.STATUS_APPROVED.equals(report.getStatus())) {
            report.setStatus(PlanReport.STATUS_APPROVED);
            report.setUpdatedAt(LocalDateTime.now());
            report = planReportRepository.save(report);

            // 同步更新关联的 audit_instance 的完成时间和状态
            if (report.getAuditInstanceId() != null) {
                try {
                    jdbcTemplate.update(
                            """
                            UPDATE public.audit_instance
                            SET status = 'APPROVED', completed_at = ?
                            WHERE id = ?
                            """,
                            LocalDateTime.now(),
                            report.getAuditInstanceId()
                    );
                } catch (Exception e) {
                    // 记录日志但不影响主流程
                    System.err.println("[ReportApplicationService] Failed to update audit_instance: " + e.getMessage());
                }
            }
        }
        syncApprovedIndicatorProgress(report.getId());
        createNextMonthlyDraftAfterTerminalApproval(report);
        return enrichReportMetadata(report);
    }

    @Transactional
    public PlanReport markWorkflowRejected(Long reportId, Long approverId, String reason) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        if (PlanReport.STATUS_REJECTED.equals(report.getStatus())
                && Objects.equals(reason, report.getRejectionReason())) {
            return report;
        }
        report.setStatus(PlanReport.STATUS_REJECTED);
        report.setRejectionReason(reason);
        report.setUpdatedAt(LocalDateTime.now());
        report = planReportRepository.save(report);

        // 同步更新关联的 audit_instance 的完成时间和状态
        if (report.getAuditInstanceId() != null) {
            try {
                jdbcTemplate.update(
                        """
                        UPDATE public.audit_instance
                        SET status = 'REJECTED', completed_at = ?
                        WHERE id = ?
                        """,
                        LocalDateTime.now(),
                        report.getAuditInstanceId()
                );
            } catch (Exception e) {
                // 记录日志但不影响主流程
                System.err.println("[ReportApplicationService] Failed to update audit_instance: " + e.getMessage());
            }
        }
        return enrichReportMetadata(report);
    }

    @Transactional
    public PlanReport markWorkflowWithdrawn(Long reportId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        report.setStatus(PlanReport.STATUS_DRAFT);
        report.setSubmittedAt(null);
        report.setAuditInstanceId(null);
        report.setUpdatedAt(LocalDateTime.now());
        PlanReport saved = enrichReportMetadata(planReportRepository.save(report));
        syncPlanStatusToDraft(saved.getPlanId());
        return saved;
    }

    @Transactional
    public PlanReport markWorkflowReturnedForResubmission(Long reportId, Long auditInstanceId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        report.setStatus(PlanReport.STATUS_DRAFT);
        report.setSubmittedAt(null);
        if (auditInstanceId != null && auditInstanceId > 0) {
            report.setAuditInstanceId(auditInstanceId);
        }
        report.setUpdatedAt(LocalDateTime.now());
        PlanReport saved = enrichReportMetadata(planReportRepository.save(report));
        syncPlanStatusToDraft(saved.getPlanId());
        return saved;
    }

    private void syncPlanStatusToDraft(Long planId) {
        if (planId == null || planId <= 0) {
            return;
        }

        try {
            jdbcTemplate.update(
                    """
                    UPDATE public.plan
                    SET status = 'DRAFT',
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """,
                    planId
            );
        } catch (Exception e) {
            log.warn("[ReportApplicationService] Failed to sync plan status back to DRAFT for planId={}: {}",
                    planId, e.getMessage());
        }
    }

    @Transactional
    public PlanReport markWorkflowReturnedForResubmission(Long reportId, Long auditInstanceId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        report.setStatus(PlanReport.STATUS_DRAFT);
        report.setSubmittedAt(null);
        if (auditInstanceId != null && auditInstanceId > 0) {
            report.setAuditInstanceId(auditInstanceId);
        }
        report.setUpdatedAt(LocalDateTime.now());
        return enrichReportMetadata(planReportRepository.save(report));
    }

    private void createNextMonthlyDraftAfterTerminalApproval(PlanReport approvedReport) {
        if (approvedReport == null || !PlanReport.STATUS_APPROVED.equals(approvedReport.getStatus())) {
            return;
        }

        Optional<PlanReport> latestReport = planReportRepository.findLatestByMonthlyScope(
                approvedReport.getPlanId(),
                approvedReport.getReportMonth(),
                approvedReport.getReportOrgType(),
                approvedReport.getReportOrgId()
        );

        if (latestReport.isPresent()
                && PlanReport.STATUS_DRAFT.equalsIgnoreCase(String.valueOf(latestReport.get().getStatus()))) {
            return;
        }

        createReport(
                approvedReport.getReportMonth(),
                approvedReport.getReportOrgId(),
                approvedReport.getReportOrgType(),
                approvedReport.getPlanId(),
                approvedReport.getCreatedBy()
        );
    }

    /**
     * 根据ID查询报告
     */
    public Optional<PlanReport> findReportById(Long reportId) {
        return planReportRepository.findById(reportId).map(this::enrichReportMetadata);
    }

    /**
     * 根据组织ID查询报告
     */
    public List<PlanReport> findReportsByOrgId(Long reportOrgId) {
        return planReportRepository.findByReportOrgId(reportOrgId).stream()
                .map(this::enrichReportMetadata)
                .toList();
    }

    /**
     * 根据月份查询报告
     */
    public List<PlanReport> findReportsByMonth(String reportMonth) {
        return planReportRepository.findByReportMonth(reportMonth).stream()
                .map(this::enrichReportMetadata)
                .toList();
    }

    /**
     * 根据状态查询报告
     */
    public List<PlanReport> findReportsByStatus(String status) {
        return planReportRepository.findByStatus(status).stream()
                .map(this::enrichReportMetadata)
                .toList();
    }

    /**
     * 根据组织和月份范围查询报告
     */
    public List<PlanReport> findReportsByOrgAndMonthRange(Long orgId, String startMonth, String endMonth) {
        return planReportRepository.findByOrgIdAndMonthRange(orgId, startMonth, endMonth).stream()
                .map(this::enrichReportMetadata)
                .toList();
    }

    /**
     * 查询待审批的报告
     */
    public List<PlanReport> findPendingReports() {
        return planReportRepository.findByStatus(PlanReport.STATUS_SUBMITTED).stream()
                .map(this::enrichReportMetadata)
                .toList();
    }

    /**
     * 根据组织类型查询报告
     */
    public List<PlanReport> findReportsByOrgType(ReportOrgType orgType) {
        return planReportRepository.findByReportOrgType(orgType).stream()
                .map(this::enrichReportMetadata)
                .toList();
    }

    // ==================== 新增查询方法 ====================

    /**
     * 查询所有有效的报告（未删除）
     */
    public List<PlanReport> findAllActiveReports() {
        return planReportRepository.findAllActive().stream()
                .map(this::enrichReportMetadata)
                .toList();
    }

    /**
     * 分页查询所有有效的报告
     */
    public Page<PlanReport> findAllActiveReports(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return planReportRepository.findAllActive(pageable).map(this::enrichReportMetadata);
    }

    /**
     * 根据条件分页查询报告
     * Note: title, minProgress, maxProgress parameters are not used in query
     * as these fields are transient (not stored in database)
     */
    public Page<PlanReport> findReportsByConditions(PlanReportQueryRequest queryRequest) {
        Pageable pageable = PageRequest.of(
                queryRequest.getPage() - 1,
                queryRequest.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return planReportRepository.findByConditions(
                queryRequest.getReportMonth(),
                queryRequest.getReportOrgId(),
                queryRequest.getReportOrgType(),
                queryRequest.getPlanId(),
                queryRequest.getStatus(),
                pageable
        ).map(this::enrichReportMetadata);
    }

    private void syncApprovedIndicatorProgress(Long reportId) {
        for (var detail : planReportIndicatorRepository.findByReportId(reportId)) {
            Indicator indicator = indicatorRepository.findById(detail.indicatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + detail.indicatorId()));
            indicator.setProgress(detail.progress());
            indicatorRepository.save(indicator);
        }
    }

    /**
     * 分页查询组织的报告
     */
    public Page<PlanReport> findReportsByOrgId(Long reportOrgId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return planReportRepository.findByReportOrgId(reportOrgId, pageable).map(this::enrichReportMetadata);
    }

    /**
     * 分页查询指定状态的报告
     */
    public Page<PlanReport> findReportsByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return planReportRepository.findByStatus(status, pageable).map(this::enrichReportMetadata);
    }

    /**
     * 根据计划ID查询报告
     */
    public List<PlanReport> findReportsByPlanId(Long planId) {
        return planReportRepository.findByPlanId(planId).stream()
                .map(this::enrichReportMetadata)
                .toList();
    }

    /**
     * 统计指定状态的报告数量
     */
    public long countReportsByStatus(String status) {
        return planReportRepository.countByStatus(status);
    }

    /**
     * 根据月份和组织ID查询报告
     */
    public List<PlanReport> findReportsByMonthAndOrgId(String month, Long orgId) {
        return planReportRepository.findByMonthAndOrgId(month, orgId).stream()
                .map(this::enrichReportMetadata)
                .toList();
    }

    private PlanReport enrichReportMetadata(PlanReport report) {
        if (report == null) {
            return null;
        }

        report.setIndicatorDetails(report.getId() == null
                ? List.of()
                : planReportIndicatorRepository.findByReportId(report.getId()));
        report.setSubmittedBy(resolveSubmittedBy(report));

        ApprovalSnapshot approvalSnapshot = resolveApprovalSnapshot(report.getAuditInstanceId());
        report.setApprovedBy(approvalSnapshot.approvedBy());
        report.setApprovedAt(approvalSnapshot.approvedAt());
        return report;
    }

    private Long resolveSubmittedBy(PlanReport report) {
        Long fallbackUserId = report.getCreatedBy();
        if (report.getAuditInstanceId() == null) {
            return fallbackUserId;
        }

        try {
            Long requesterId = jdbcTemplate.query(
                    """
                    SELECT requester_id
                    FROM public.audit_instance
                    WHERE id = ?
                    """,
                    rs -> rs.next() ? rs.getLong("requester_id") : null,
                    report.getAuditInstanceId()
            );
            return requesterId != null && requesterId > 0 ? requesterId : fallbackUserId;
        } catch (Exception ignored) {
            return fallbackUserId;
        }
    }

    private ApprovalSnapshot resolveApprovalSnapshot(Long auditInstanceId) {
        if (auditInstanceId == null || auditInstanceId <= 0) {
            return ApprovalSnapshot.empty();
        }

        try {
            List<ApprovalSnapshot> snapshots = jdbcTemplate.query(
                    """
                    SELECT
                        asi.approver_id AS approved_by,
                        COALESCE(asi.approved_at, ai.completed_at) AS approved_at
                    FROM public.audit_instance ai
                    LEFT JOIN public.audit_step_instance asi
                        ON asi.instance_id = ai.id
                       AND asi.status = 'APPROVED'
                    WHERE ai.id = ?
                      AND ai.status = 'APPROVED'
                    ORDER BY
                        asi.approved_at DESC NULLS LAST,
                        asi.step_no DESC NULLS LAST,
                        asi.id DESC NULLS LAST
                    LIMIT 1
                    """,
                    (rs, rowNum) -> mapApprovalSnapshot(rs),
                    auditInstanceId
            );
            return snapshots.isEmpty() ? ApprovalSnapshot.empty() : snapshots.get(0);
        } catch (Exception ignored) {
            return ApprovalSnapshot.empty();
        }
    }

    private ApprovalSnapshot mapApprovalSnapshot(ResultSet resultSet) throws java.sql.SQLException {
        Long approvedBy = resultSet.getObject("approved_by", Long.class);
        LocalDateTime approvedAt = resultSet.getObject("approved_at", LocalDateTime.class);
        return new ApprovalSnapshot(approvedBy, approvedAt);
    }

    private record ApprovalSnapshot(Long approvedBy, LocalDateTime approvedAt) {
        private static ApprovalSnapshot empty() {
            return new ApprovalSnapshot(null, null);
        }
    }

    /**
     * 发布并保存领域事件
     * 从聚合根中提取所有领域事件，保存到事件存储，并发布到事件系统
     */
    private void publishAndSaveEvents(com.sism.shared.domain.model.base.AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();
        eventPublisher.publishAll(events);
        aggregate.clearEvents();
    }
}
