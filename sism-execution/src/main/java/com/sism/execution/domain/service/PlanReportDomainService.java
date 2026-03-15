package com.sism.execution.domain.service;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportRepository;
import com.sism.execution.domain.model.report.event.PlanReportSubmittedEvent;
import com.sism.execution.domain.model.report.event.PlanReportApprovedEvent;
import com.sism.execution.domain.model.report.event.PlanReportRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PlanReportDomainService - 计划报告领域服务
 * 处理报告提交、审批等复杂业务逻辑
 */
@Service
@RequiredArgsConstructor
public class PlanReportDomainService {

    private final PlanReportRepository planReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 提交报告
     */
    @Transactional
    public PlanReport submit(Long reportId, Long userId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.submit(userId);
        PlanReport saved = planReportRepository.save(report);
        eventPublisher.publishEvent(new PlanReportSubmittedEvent(saved.getId(), saved.getReportMonth(), saved.getReportOrgId()));
        return saved;
    }

    /**
     * 审批通过报告
     */
    @Transactional
    public PlanReport approve(Long reportId, Long approverId) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.approve(approverId);
        PlanReport saved = planReportRepository.save(report);
        eventPublisher.publishEvent(new PlanReportApprovedEvent(saved.getId(), saved.getReportMonth(), saved.getReportOrgId()));
        return saved;
    }

    /**
     * 驳回报告
     */
    @Transactional
    public PlanReport reject(Long reportId, Long approverId, String reason) {
        PlanReport report = planReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.reject(approverId, reason);
        PlanReport saved = planReportRepository.save(report);
        eventPublisher.publishEvent(new PlanReportRejectedEvent(saved.getId(), saved.getReportMonth(), saved.getReportOrgId(), reason));
        return saved;
    }

    /**
     * 查询某组织的待审批报告
     */
    public List<PlanReport> findPendingReports(Long orgId) {
        return planReportRepository.findByReportOrgIdAndStatus(orgId, "PENDING");
    }

    /**
     * 查询某类型组织的报告
     */
    public List<PlanReport> findByOrgType(ReportOrgType orgType) {
        return planReportRepository.findByReportOrgType(orgType);
    }

    /**
     * 查询某月的所有报告
     */
    public List<PlanReport> findByMonth(String reportMonth) {
        return planReportRepository.findByReportMonth(reportMonth);
    }

    /**
     * 查询某时间段内的报告
     */
    public List<PlanReport> findByMonthRange(Long orgId, String startMonth, String endMonth) {
        return planReportRepository.findByOrgIdAndMonthRange(orgId, startMonth, endMonth);
    }
}
