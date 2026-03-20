package com.sism.execution.application;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportRepository;
import com.sism.execution.interfaces.dto.PlanReportQueryRequest;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ReportApplicationService - 计划报告应用服务
 * 处理计划报告的业务逻辑
 */
@Service("executionReportApplicationService")
@RequiredArgsConstructor
public class ReportApplicationService {

    private final PlanReportRepository planReportRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * 创建报告（草稿）
     */
    @Transactional
    public PlanReport createReport(String reportMonth, Long reportOrgId,
                                   ReportOrgType reportOrgType, Long planId) {
        Optional<PlanReport> existingReport = planReportRepository.findByUniqueKey(
                planId, reportMonth, reportOrgType, reportOrgId);

        if (existingReport.isPresent()) {
            PlanReport report = existingReport.get();

            if (Boolean.TRUE.equals(report.getIsDeleted())) {
                report.setIsDeleted(false);
                report.setStatus(PlanReport.STATUS_DRAFT);
                report.setSubmittedAt(null);
                report.setUpdatedAt(LocalDateTime.now());
                report.validate();
                return planReportRepository.save(report);
            }

            throw new IllegalStateException("当前月份已存在报告，请勿重复创建");
        }

        PlanReport report = PlanReport.createDraft(
                reportMonth, reportOrgId, reportOrgType, planId);
        report.validate();
        return planReportRepository.save(report);
    }

    /**
     * 更新报告内容（包含标题）
     */
    @Transactional
    public PlanReport updateReport(Long reportId, String title, String content, String summary, Integer progress,
                                   String issues, String nextPlan) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.updateContent(content, summary, progress, issues, nextPlan);
        if (title != null) {
            report.setTitle(title);
        }
        return planReportRepository.save(report);
    }

    /**
     * 更新报告内容（旧方法，保持向后兼容）
     */
    @Transactional
    public PlanReport updateReport(Long reportId, String content, String summary, Integer progress,
                                   String issues, String nextPlan) {
        return updateReport(reportId, null, content, summary, progress, issues, nextPlan);
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
        return report;
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
        publishAndSaveEvents(report);
        return report;
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
        return report;
    }

    /**
     * 删除报告（逻辑删除）
     */
    @Transactional
    public void deleteReport(Long reportId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.setIsDeleted(true);
        planReportRepository.save(report);
    }

    /**
     * 根据ID查询报告
     */
    public Optional<PlanReport> findReportById(Long reportId) {
        return planReportRepository.findById(reportId);
    }

    /**
     * 根据组织ID查询报告
     */
    public List<PlanReport> findReportsByOrgId(Long reportOrgId) {
        return planReportRepository.findByReportOrgId(reportOrgId);
    }

    /**
     * 根据月份查询报告
     */
    public List<PlanReport> findReportsByMonth(String reportMonth) {
        return planReportRepository.findByReportMonth(reportMonth);
    }

    /**
     * 根据状态查询报告
     */
    public List<PlanReport> findReportsByStatus(String status) {
        return planReportRepository.findByStatus(status);
    }

    /**
     * 根据组织和月份范围查询报告
     */
    public List<PlanReport> findReportsByOrgAndMonthRange(Long orgId, String startMonth, String endMonth) {
        return planReportRepository.findByOrgIdAndMonthRange(orgId, startMonth, endMonth);
    }

    /**
     * 查询待审批的报告
     */
    public List<PlanReport> findPendingReports() {
        return planReportRepository.findByStatus(PlanReport.STATUS_SUBMITTED);
    }

    /**
     * 根据组织类型查询报告
     */
    public List<PlanReport> findReportsByOrgType(ReportOrgType orgType) {
        return planReportRepository.findByReportOrgType(orgType);
    }

    // ==================== 新增查询方法 ====================

    /**
     * 查询所有有效的报告（未删除）
     */
    public List<PlanReport> findAllActiveReports() {
        return planReportRepository.findAllActive();
    }

    /**
     * 分页查询所有有效的报告
     */
    public Page<PlanReport> findAllActiveReports(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return planReportRepository.findAllActive(pageable);
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
        );
    }

    /**
     * 分页查询组织的报告
     */
    public Page<PlanReport> findReportsByOrgId(Long reportOrgId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return planReportRepository.findByReportOrgId(reportOrgId, pageable);
    }

    /**
     * 分页查询指定状态的报告
     */
    public Page<PlanReport> findReportsByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return planReportRepository.findByStatus(status, pageable);
    }

    /**
     * 根据计划ID查询报告
     */
    public List<PlanReport> findReportsByPlanId(Long planId) {
        return planReportRepository.findByPlanId(planId);
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
        return planReportRepository.findByMonthAndOrgId(month, orgId);
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
