package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JpaPlanReportRepository - PlanReport 的 JPA 仓储实现
 * 同时继承 JpaRepository 和领域层 PlanReportRepository 接口
 * 位于 infrastructure.persistence 包下，可被 @EnableJpaRepositories 扫描到
 */
@Repository
public interface JpaPlanReportRepository extends JpaRepository<PlanReport, Long>, PlanReportRepository {

    @Override
    List<PlanReport> findByReportOrgId(Long reportOrgId);

    @Override
    List<PlanReport> findByReportOrgType(ReportOrgType reportOrgType);

    @Override
    List<PlanReport> findByReportMonth(String reportMonth);

    @Override
    List<PlanReport> findByStatus(String status);

    @Override
    List<PlanReport> findByReportOrgIdAndStatus(Long reportOrgId, String status);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportOrgId = :orgId AND pr.reportMonth BETWEEN :startMonth AND :endMonth")
    List<PlanReport> findByOrgIdAndMonthRange(
            @Param("orgId") Long orgId,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.status = :status AND pr.reportOrgType = :orgType")
    List<PlanReport> findByStatusAndOrgType(
            @Param("status") String status,
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

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportOrgId = :orgId AND pr.isDeleted = false")
    Page<PlanReport> findByReportOrgId(@Param("orgId") Long orgId, Pageable pageable);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.status = :status AND pr.isDeleted = false")
    Page<PlanReport> findByStatus(@Param("status") String status, Pageable pageable);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.planId = :planId AND pr.isDeleted = false")
    List<PlanReport> findByPlanId(@Param("planId") Long planId);

    @Override
    @Query("SELECT COUNT(pr) FROM PlanReport pr WHERE pr.status = :status AND pr.isDeleted = false")
    long countByStatus(@Param("status") String status);

    @Override
    @Query("SELECT pr FROM PlanReport pr WHERE pr.reportMonth = :month AND pr.reportOrgId = :orgId AND pr.isDeleted = false")
    List<PlanReport> findByMonthAndOrgId(
            @Param("month") String month,
            @Param("orgId") Long orgId);
}
