package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.PlanReportStatus;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JpaPlanReportRepository - PlanReport 的 JPA 仓储实现
 * 同时继承 JpaRepository 和领域层 PlanReportRepository 接口
 * 位于 infrastructure.persistence 包下，可被 @EnableJpaRepositories 扫描到
 */
@Repository
public interface JpaPlanReportRepository extends JpaRepository<PlanReport, Long>, PlanReportRepository {

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportOrgId = :reportOrgId AND pr.isDeleted = false")
    List<PlanReport> findByReportOrgId(@Param("reportOrgId") Long reportOrgId);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportOrgType = :reportOrgType AND pr.isDeleted = false")
    List<PlanReport> findByReportOrgType(@Param("reportOrgType") ReportOrgType reportOrgType);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportMonth = :reportMonth AND pr.isDeleted = false")
    List<PlanReport> findByReportMonth(@Param("reportMonth") String reportMonth);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.status = :status AND pr.isDeleted = false")
    List<PlanReport> findByStatus(@Param("status") PlanReportStatus status);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportOrgId = :reportOrgId AND pr.status = :status AND pr.isDeleted = false")
    List<PlanReport> findByReportOrgIdAndStatus(@Param("reportOrgId") Long reportOrgId,
                                                @Param("status") PlanReportStatus status);

    @Override
    @Query(value = """
            SELECT *
            FROM plan_report pr
            WHERE pr.report_org_id = :orgId
              AND pr.is_deleted = false
              AND to_date(pr.report_month, 'YYYYMM') BETWEEN to_date(:startMonth, 'YYYYMM') AND to_date(:endMonth, 'YYYYMM')
            """, nativeQuery = true)
    List<PlanReport> findByOrgIdAndMonthRange(
            @Param("orgId") Long orgId,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.status = :status AND pr.reportOrgType = :orgType AND pr.isDeleted = false")
    List<PlanReport> findByStatusAndOrgType(
            @Param("status") PlanReportStatus status,
            @Param("orgType") ReportOrgType orgType);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.isDeleted = false")
    List<PlanReport> findAllActive();

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.isDeleted = false")
    Page<PlanReport> findAllActive(Pageable pageable);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE " +
           "(:reportMonth IS NULL OR pr.reportMonth = :reportMonth) AND " +
           "(:reportOrgId IS NULL OR pr.reportOrgId = :reportOrgId) AND " +
           "(:reportOrgType IS NULL OR pr.reportOrgType = :reportOrgType) AND " +
           "(:planId IS NULL OR pr.planId = :planId) AND " +
           "(:status IS NULL OR pr.status = :status) AND " +
           "pr.isDeleted = false")
    Page<PlanReport> findByConditions(
            @Param("reportMonth") String reportMonth,
            @Param("reportOrgId") Long reportOrgId,
            @Param("reportOrgType") ReportOrgType reportOrgType,
            @Param("planId") Long planId,
            @Param("status") PlanReportStatus status,
            Pageable pageable);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportOrgId = :orgId AND pr.isDeleted = false")
    Page<PlanReport> findByReportOrgId(@Param("orgId") Long orgId, Pageable pageable);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.status = :status AND pr.isDeleted = false")
    Page<PlanReport> findByStatus(@Param("status") PlanReportStatus status, Pageable pageable);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.planId = :planId AND pr.isDeleted = false")
    List<PlanReport> findByPlanId(@Param("planId") Long planId);

    @Override
    @Query("SELECT COUNT(pr) FROM PlanReport pr WHERE pr.status = :status AND pr.isDeleted = false")
    long countByStatus(@Param("status") PlanReportStatus status);

    @Query("SELECT COUNT(pr) FROM PlanReport pr WHERE pr.status = :status AND pr.reportOrgId = :reportOrgId AND pr.isDeleted = false")
    long countByStatusAndReportOrgId(@Param("status") PlanReportStatus status,
                                     @Param("reportOrgId") Long reportOrgId);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportMonth = :month AND pr.reportOrgId = :orgId AND pr.isDeleted = false")
    List<PlanReport> findByMonthAndOrgId(
            @Param("month") String month,
            @Param("orgId") Long orgId);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.planId = :planId AND pr.reportMonth = :reportMonth AND pr.reportOrgType = :reportOrgType AND pr.reportOrgId = :reportOrgId AND pr.isDeleted = false")
    Optional<PlanReport> findByUniqueKey(
            @Param("planId") Long planId,
            @Param("reportMonth") String reportMonth,
            @Param("reportOrgType") ReportOrgType reportOrgType,
            @Param("reportOrgId") Long reportOrgId);

    @Query("""
            SELECT pr
            FROM PlanReport pr
            WHERE pr.planId = :planId
              AND pr.reportMonth = :reportMonth
              AND pr.reportOrgType = :reportOrgType
              AND pr.reportOrgId = :reportOrgId
              AND pr.isDeleted = false
            ORDER BY pr.submittedAt DESC, pr.id DESC
            """)
    List<PlanReport> findLatestCandidatesByMonthlyScope(
            @Param("planId") Long planId,
            @Param("reportMonth") String reportMonth,
            @Param("reportOrgType") ReportOrgType reportOrgType,
            @Param("reportOrgId") Long reportOrgId);

    @Override
    default Optional<PlanReport> findLatestByMonthlyScope(
            Long planId,
            String reportMonth,
            ReportOrgType reportOrgType,
            Long reportOrgId
    ) {
        return findLatestCandidatesByMonthlyScope(planId, reportMonth, reportOrgType, reportOrgId)
                .stream()
                .findFirst();
    }
}
