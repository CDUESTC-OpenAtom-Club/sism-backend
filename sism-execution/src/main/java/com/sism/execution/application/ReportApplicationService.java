package com.sism.execution.application;

import com.sism.exception.ConflictException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportIndicatorRepository;
import com.sism.execution.domain.repository.PlanReportIndicatorSnapshot;
import com.sism.execution.domain.repository.PlanReportRepository;
import com.sism.execution.domain.repository.WorkflowApprovalMetadata;
import com.sism.execution.domain.repository.WorkflowApprovalMetadataQuery;
import com.sism.execution.domain.repository.PlanStatusSyncGateway;
import com.sism.execution.domain.repository.WorkflowAuditSyncGateway;
import com.sism.execution.interfaces.dto.PlanReportQueryRequest;
import com.sism.execution.interfaces.dto.UpdatePlanReportIndicatorDetailRequest;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.shared.domain.exception.TechnicalException;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ReportApplicationService - 计划报告应用服务
 * 处理计划报告的业务逻辑
 */
@Service("executionReportApplicationService")
@RequiredArgsConstructor
@Slf4j
public class ReportApplicationService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final PlanReportRepository planReportRepository;
    private final PlanReportIndicatorRepository planReportIndicatorRepository;
    private final IndicatorRepository indicatorRepository;
    private final DomainEventPublisher eventPublisher;
    private final WorkflowApprovalMetadataQuery workflowApprovalMetadataQuery;
    private final WorkflowAuditSyncGateway workflowAuditSyncGateway;
    private final PlanStatusSyncGateway planStatusSyncGateway;

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
                throw new ConflictException("当前月份已有报告正在审批中，请等待审批完成或先撤回");
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
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        validatePendingProgress(indicatorId, progress);
        report.markCreatedByIfAbsent(operatorUserId);
        report.updateContent(content, summary, progress, issues, nextPlan);
        if (title != null) {
            report.setTitle(title);
        }
        PlanReport savedReport = planReportRepository.save(report);
        upsertIndicatorDetail(savedReport.getId(), indicatorId, progress, content, milestoneNote, List.of(), operatorUserId);
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
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

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
            upsertIndicatorDetail(
                    savedReport.getId(),
                    detail.getIndicatorId(),
                    detail.getProgress(),
                    detail.getContent(),
                    detail.getMilestoneNote(),
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
                .orElseThrow(() -> new ResourceNotFoundException("Indicator", indicatorId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        report.submit(userId);
        report = planReportRepository.save(report);
        publishAndSaveEvents(report);
        return enrichReportMetadata(report);
    }

    @Transactional
    public PlanReport submitReport(Long reportId, Long userId, CurrentUser currentUser) {
        requireAnyRole(currentUser, "ROLE_ADMIN", "ROLE_STRATEGY_DEPT", "ROLE_FUNC_DEPT", "ROLE_APPROVER");
        Long actorId = resolveUserId(userId, currentUser);
        return submitReport(reportId, actorId);
    }

    @Transactional
    public PlanReport attachAuditInstance(Long reportId, Long auditInstanceId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        report.approve(userId);
        report = planReportRepository.save(report);
        syncApprovedIndicatorProgress(report.getId());
        publishAndSaveEvents(report);
        return enrichReportMetadata(report);
    }

    @Transactional
    public PlanReport approveReport(Long reportId, Long userId, CurrentUser currentUser) {
        requireAnyRole(currentUser, "ROLE_ADMIN", "ROLE_APPROVER");
        Long actorId = resolveUserId(userId, currentUser);
        return approveReport(reportId, actorId);
    }

    /**
     * 驳回报告
     */
    @Transactional
    public PlanReport rejectReport(Long reportId, Long userId, String reason) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        report.reject(userId, reason);
        report = planReportRepository.save(report);
        publishAndSaveEvents(report);
        return enrichReportMetadata(report);
    }

    @Transactional
    public PlanReport rejectReport(Long reportId, Long userId, String reason, CurrentUser currentUser) {
        requireAnyRole(currentUser, "ROLE_ADMIN", "ROLE_APPROVER");
        Long actorId = resolveUserId(userId, currentUser);
        return rejectReport(reportId, actorId, reason);
    }

    @Transactional
    public PlanReport markWorkflowApproved(Long reportId, Long approverId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
        if (!PlanReport.STATUS_APPROVED.equals(report.getStatus())) {
            report.setStatus(PlanReport.STATUS_APPROVED);
            report.setUpdatedAt(LocalDateTime.now());
            report = planReportRepository.save(report);
        }
        Long auditInstanceId = report.getAuditInstanceId();
        syncWorkflowAuditState(reportId, auditInstanceId,
                () -> workflowAuditSyncGateway.markApproved(auditInstanceId));
        syncApprovedIndicatorProgress(report.getId());
        createNextMonthlyDraftAfterTerminalApproval(report);
        return enrichReportMetadata(report);
    }

    @Transactional
    public PlanReport markWorkflowRejected(Long reportId, Long approverId, String reason) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
        if (PlanReport.STATUS_REJECTED.equals(report.getStatus())
                && Objects.equals(reason, report.getRejectionReason())) {
            return report;
        }
        report.setStatus(PlanReport.STATUS_REJECTED);
        report.setRejectionReason(reason);
        report.setUpdatedAt(LocalDateTime.now());
        report = planReportRepository.save(report);
        Long auditInstanceId = report.getAuditInstanceId();
        syncWorkflowAuditState(reportId, auditInstanceId,
                () -> workflowAuditSyncGateway.markRejected(auditInstanceId));
        return enrichReportMetadata(report);
    }

    @Transactional
    public PlanReport markWorkflowWithdrawn(Long reportId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
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

        bestEffortSync(
                "sync plan status back to DRAFT",
                null,
                planId,
                () -> planStatusSyncGateway.syncBackToDraft(planId)
        );
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
        return enrichReportList(planReportRepository.findByReportOrgId(reportOrgId));
    }

    /**
     * 根据月份查询报告
     */
    public List<PlanReport> findReportsByMonth(String reportMonth) {
        return enrichReportList(planReportRepository.findByReportMonth(reportMonth));
    }

    /**
     * 根据状态查询报告
     */
    public List<PlanReport> findReportsByStatus(String status) {
        return enrichReportList(planReportRepository.findByStatus(status));
    }

    /**
     * 根据组织和月份范围查询报告
     */
    public List<PlanReport> findReportsByOrgAndMonthRange(Long orgId, String startMonth, String endMonth) {
        return enrichReportList(planReportRepository.findByOrgIdAndMonthRange(orgId, startMonth, endMonth));
    }

    /**
     * 查询待审批的报告
     */
    public List<PlanReport> findPendingReports() {
        return enrichReportList(planReportRepository.findByStatus(PlanReport.STATUS_SUBMITTED));
    }

    /**
     * 根据组织类型查询报告
     */
    public List<PlanReport> findReportsByOrgType(ReportOrgType orgType) {
        return enrichReportList(planReportRepository.findByReportOrgType(orgType));
    }

    // ==================== 新增查询方法 ====================

    /**
     * 查询所有有效的报告（未删除）
     */
    public List<PlanReport> findAllActiveReports() {
        return enrichReportList(planReportRepository.findAllActive());
    }

    /**
     * 分页查询所有有效的报告
     */
    public Page<PlanReport> findAllActiveReports(int page, int size) {
        Pageable pageable = createPageable(page, size);
        return enrichReportPage(planReportRepository.findAllActive(pageable));
    }

    /**
     * 根据条件分页查询报告
     * Note: title, minProgress, maxProgress parameters are not used in query
     * as these fields are transient (not stored in database)
     */
    public Page<PlanReport> findReportsByConditions(PlanReportQueryRequest queryRequest) {
        Pageable pageable = createPageable(queryRequest.getPage(), queryRequest.getSize());

        return enrichReportPage(planReportRepository.findByConditions(
                queryRequest.getReportMonth(),
                queryRequest.getReportOrgId(),
                queryRequest.getReportOrgType(),
                queryRequest.getPlanId(),
                queryRequest.getStatus(),
                pageable
        ));
    }

    private void syncApprovedIndicatorProgress(Long reportId) {
        List<PlanReportIndicatorSnapshot> details = planReportIndicatorRepository.findByReportId(reportId);
        if (details.isEmpty()) {
            return;
        }
        for (PlanReportIndicatorSnapshot detail : details) {
            Indicator indicator = indicatorRepository.findById(detail.indicatorId()).orElse(null);
            if (indicator == null) {
                continue;
            }
            indicator.setProgress(detail.progress());
            indicatorRepository.save(indicator);
        }
    }

    private PlanReport enrichReportMetadata(PlanReport report) {
        if (report == null) {
            return null;
        }
        assignMetadata(List.of(report));
        return report;
    }

    private List<PlanReport> enrichReportList(List<PlanReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return reports;
        }
        assignMetadata(reports);
        return reports;
    }

    private Page<PlanReport> enrichReportPage(Page<PlanReport> page) {
        if (page == null || page.isEmpty()) {
            return page;
        }
        List<PlanReport> content = new ArrayList<>(page.getContent());
        assignMetadata(content);
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    private void assignMetadata(List<PlanReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return;
        }

        List<PlanReport> nonNullReports = reports.stream()
                .filter(Objects::nonNull)
                .toList();
        List<Long> reportIds = nonNullReports.stream()
                .map(PlanReport::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> auditIds = nonNullReports.stream()
                .map(PlanReport::getAuditInstanceId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        Map<Long, List<PlanReportIndicatorSnapshot>> indicatorDetailsByReportId = reportIds.isEmpty()
                ? Map.of()
                : planReportIndicatorRepository.findByReportIds(reportIds);
        Map<Long, WorkflowApprovalMetadata> workflowMetadata = auditIds.isEmpty()
                ? Map.of()
                : workflowApprovalMetadataQuery.findByAuditInstanceIds(auditIds);

        for (PlanReport report : nonNullReports) {
            Long reportId = report.getId();
            report.setIndicatorDetails(reportId == null
                    ? List.of()
                    : indicatorDetailsByReportId.getOrDefault(reportId, List.of()));

            Long auditInstanceId = report.getAuditInstanceId();
            WorkflowApprovalMetadata metadata = auditInstanceId == null
                    ? WorkflowApprovalMetadata.empty()
                    : workflowMetadata.getOrDefault(auditInstanceId, WorkflowApprovalMetadata.empty());
            Long submittedBy = metadata.submittedBy();
            if (submittedBy == null) {
                submittedBy = report.getCreatedBy();
            }
            report.setSubmittedBy(submittedBy);

            report.setApprovedBy(metadata.approvedBy());
            report.setApprovedAt(metadata.approvedAt());
        }
    }

    /**
     * 分页查询组织的报告
     */
    public Page<PlanReport> findReportsByOrgId(Long reportOrgId, int page, int size) {
        Pageable pageable = createPageable(page, size);
        return enrichReportPage(planReportRepository.findByReportOrgId(reportOrgId, pageable));
    }

    /**
     * 分页查询指定状态的报告
     */
    public Page<PlanReport> findReportsByStatus(String status, int page, int size) {
        Pageable pageable = createPageable(page, size);
        return enrichReportPage(planReportRepository.findByStatus(status, pageable));
    }

    /**
     * 根据计划ID查询报告
     */
    public List<PlanReport> findReportsByPlanId(Long planId) {
        return enrichReportList(planReportRepository.findByPlanId(planId));
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
        return enrichReportList(planReportRepository.findByMonthAndOrgId(month, orgId));
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

    @FunctionalInterface
    private interface SyncAction {
        void run();
    }

    private void syncWorkflowAuditState(Long reportId, Long auditInstanceId, SyncAction action) {
        if (auditInstanceId == null) {
            return;
        }

        bestEffortSync("sync audit_instance state", reportId, auditInstanceId, action);
    }

    private void bestEffortSync(String operation, Long reportId, Long relatedId, SyncAction action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("[ReportApplicationService] Best-effort sync failed. operation={}, reportId={}, relatedId={}",
                    operation, reportId, relatedId, e);
            throw new TechnicalException(
                    "EXECUTION_SYNC_FAILED",
                    "Failed to " + operation + " for reportId=" + reportId + ", relatedId=" + relatedId,
                    e
            );
        }
    }

    private void upsertIndicatorDetail(Long reportId,
                                       Long indicatorId,
                                       Integer progress,
                                       String comment,
                                       String milestoneNote,
                                       List<Long> attachmentIds,
                                       Long operatorUserId) {
        if (reportId == null || indicatorId == null) {
            return;
        }

        validatePendingProgress(indicatorId, progress);
        Long planReportIndicatorId = planReportIndicatorRepository.upsertDraftIndicator(
                reportId,
                indicatorId,
                progress,
                comment,
                milestoneNote
        );
        // The draft indicator and its attachments must be kept in sync as a unit.
        planReportIndicatorRepository.attachFiles(
                planReportIndicatorId,
                attachmentIds == null ? List.of() : attachmentIds,
                operatorUserId
        );
    }

    private Pageable createPageable(int page, int size) {
        return PageRequest.of(normalizePageNumber(page), normalizePageSize(size), DEFAULT_SORT);
    }

    private int normalizePageNumber(int page) {
        return Math.max(page, 1) - 1;
    }

    private int normalizePageSize(int size) {
        return Math.max(size, 1);
    }

    private Long resolveUserId(Long explicitUserId, CurrentUser currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalStateException("当前用户未登录或缺少 ID");
        }
        if (explicitUserId == null) {
            return currentUser.getId();
        }
        if (!currentUser.getId().equals(explicitUserId)) {
            throw new IllegalArgumentException("请求中传入的 userId 与当前用户不一致");
        }
        return explicitUserId;
    }

    private void requireAnyRole(CurrentUser currentUser, String... roleNames) {
        if (!hasAnyRole(currentUser, roleNames)) {
            throw new AccessDeniedException("当前用户没有执行此操作的权限");
        }
    }

    private boolean hasAnyRole(CurrentUser currentUser, String... roleNames) {
        if (currentUser == null || currentUser.getAuthorities() == null) {
            return false;
        }
        for (String roleName : roleNames) {
            boolean matched = currentUser.getAuthorities().stream()
                    .anyMatch(authority -> roleName.equals(authority.getAuthority()));
            if (matched) {
                return true;
            }
        }
        return false;
    }
}
