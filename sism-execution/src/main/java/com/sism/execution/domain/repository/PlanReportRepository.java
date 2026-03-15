package com.sism.execution.domain.repository;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT pr FROM PlanReport pr WHERE pr.isDeleted = false")
    List<PlanReport> findAllActive();

    @Query("SELECT pr FROM PlanReport pr WHERE pr.isDeleted = false")
    Page<PlanReport> findAllActive(Pageable pageable);

    @Query("SELECT pr FROM PlanReport pr WHERE " +
           "(:reportMonth IS NULL OR pr.reportMonth = :reportMonth) AND " +
           "(:reportOrgId IS NULL OR pr.reportOrgId = :reportOrgId) AND " +
           "(:reportOrgType IS NULL OR pr.reportOrgType = :reportOrgType) AND " +
           "(:planId IS NULL OR pr.planId = :planId) AND " +
           "(:status IS NULL OR pr.status = :status) AND " +
           "(:title IS NULL OR pr.title LIKE %:title%) AND " +
           "(:minProgress IS NULL OR pr.progress >= :minProgress) AND " +
           "(:maxProgress IS NULL OR pr.progress <= :maxProgress) AND " +
           "pr.isDeleted = false")
    Page<PlanReport> findByConditions(
            @Param("reportMonth") String reportMonth,
            @Param("reportOrgId") Long reportOrgId,
            @Param("reportOrgType") ReportOrgType reportOrgType,
            @Param("planId") Long planId,
            @Param("status") String status,
            @Param("title") String title,
            @Param("minProgress") Integer minProgress,
            @Param("maxProgress") Integer maxProgress,
            Pageable pageable);

    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportOrgId = :orgId AND pr.isDeleted = false")
    Page<PlanReport> findByReportOrgId(@Param("orgId") Long orgId, Pageable pageable);

    @Query("SELECT pr FROM PlanReport pr WHERE pr.status = :status AND pr.isDeleted = false")
    Page<PlanReport> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT pr FROM PlanReport pr WHERE pr.planId = :planId AND pr.isDeleted = false")
    List<PlanReport> findByPlanId(@Param("planId") Long planId);

    @Query("SELECT COUNT(pr) FROM PlanReport pr WHERE pr.status = :status AND pr.isDeleted = false")
    long countByStatus(@Param("status") String status);

    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportMonth = :month AND pr.reportOrgId = :orgId AND pr.isDeleted = false")
    List<PlanReport> findByMonthAndOrgId(
            @Param("month") String month,
            @Param("orgId") Long orgId);
}
