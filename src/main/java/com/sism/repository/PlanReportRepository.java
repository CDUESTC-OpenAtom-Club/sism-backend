package com.sism.repository;

import com.sism.entity.PlanReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanReportRepository extends JpaRepository<PlanReport, Long> {
    Optional<PlanReport> findByIdAndIsDeletedFalse(Long id);
    List<PlanReport> findByPlanIdAndIsDeletedFalseOrderByReportMonthDescIdDesc(Long planId);
}
