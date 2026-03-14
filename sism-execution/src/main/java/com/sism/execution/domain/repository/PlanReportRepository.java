package com.sism.execution.domain.repository;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PlanReportRepository - 计划报告仓储接口
 */
@Repository
public interface PlanReportRepository extends JpaRepository<PlanReport, Long> {

    List<PlanReport> findByReportOrgId(Long reportOrgId);

    List<PlanReport> findByReportOrgType(ReportOrgType reportOrgType);

    List<PlanReport> findByReportMonth(String reportMonth);

    List<PlanReport> findByStatus(String status);

    List<PlanReport> findByReportOrgIdAndStatus(Long reportOrgId, String status);

    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportOrgId = :orgId AND pr.reportMonth BETWEEN :startMonth AND :endMonth")
    List<PlanReport> findByOrgIdAndMonthRange(
            @Param("orgId") Long orgId,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth);

    @Query("SELECT pr FROM PlanReport pr WHERE pr.status = :status AND pr.reportOrgType = :orgType")
    List<PlanReport> findByStatusAndOrgType(
            @Param("status") String status,
            @Param("orgType") ReportOrgType orgType);
}
