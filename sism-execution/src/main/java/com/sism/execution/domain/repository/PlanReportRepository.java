package com.sism.execution.domain.repository;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * PlanReportRepository - 计划报告仓储接口（领域层）
 * 定义领域层所需的仓储方法
 * 实际 JPA 实现位于 infrastructure.persistence.JpaPlanReportRepository
 */
public interface PlanReportRepository {

    Optional<PlanReport> findById(Long id);

    PlanReport save(PlanReport report);

    List<PlanReport> findByReportOrgId(Long reportOrgId);

    List<PlanReport> findByReportOrgType(ReportOrgType reportOrgType);

    List<PlanReport> findByReportMonth(String reportMonth);

    List<PlanReport> findByStatus(String status);

    List<PlanReport> findByReportOrgIdAndStatus(Long reportOrgId, String status);

    List<PlanReport> findByOrgIdAndMonthRange(Long orgId, String startMonth, String endMonth);

    List<PlanReport> findByStatusAndOrgType(String status, ReportOrgType orgType);

    List<PlanReport> findAllActive();

    Page<PlanReport> findAllActive(Pageable pageable);

    Page<PlanReport> findByConditions(
            String reportMonth,
            Long reportOrgId,
            ReportOrgType reportOrgType,
            Long planId,
            String status,
            Pageable pageable);

    Page<PlanReport> findByReportOrgId(Long orgId, Pageable pageable);

    Page<PlanReport> findByStatus(String status, Pageable pageable);

    List<PlanReport> findByPlanId(Long planId);

    long countByStatus(String status);

    List<PlanReport> findByMonthAndOrgId(String month, Long orgId);

    Optional<PlanReport> findByUniqueKey(Long planId, String reportMonth, ReportOrgType reportOrgType, Long reportOrgId);
}
