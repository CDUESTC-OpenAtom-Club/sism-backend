package com.sism.execution.application;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ReportApplicationService - 报告应用服务
 * 处理计划报告的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class ReportApplicationService {

    private final PlanReportRepository planReportRepository;

    /**
     * 创建报告（草稿）
     */
    @Transactional
    public PlanReport createReport(String reportMonth, Long reportOrgId, String reportOrgName,
                                   ReportOrgType reportOrgType, Long planId) {
        PlanReport report = PlanReport.createDraft(
                reportMonth, reportOrgId, reportOrgName, reportOrgType, planId);
        report.validate();
        return planReportRepository.save(report);
    }

    /**
     * 更新报告内容
     */
    @Transactional
    public PlanReport updateReport(Long reportId, String content, String summary, Integer progress,
                                   String issues, String nextPlan) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.updateContent(content, summary, progress, issues, nextPlan);
        return planReportRepository.save(report);
    }

    /**
     * 提交报告
     */
    @Transactional
    public PlanReport submitReport(Long reportId, Long userId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.submit(userId);
        return planReportRepository.save(report);
    }

    /**
     * 审批通过报告
     */
    @Transactional
    public PlanReport approveReport(Long reportId, Long userId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.approve(userId);
        return planReportRepository.save(report);
    }

    /**
     * 驳回报告
     */
    @Transactional
    public PlanReport rejectReport(Long reportId, Long userId, String reason) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.reject(userId, reason);
        return planReportRepository.save(report);
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
}
