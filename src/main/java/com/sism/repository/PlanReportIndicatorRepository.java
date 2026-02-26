package com.sism.repository;

import com.sism.entity.PlanReportIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PlanReportIndicator entity
 */
@Repository
public interface PlanReportIndicatorRepository extends JpaRepository<PlanReportIndicator, Long> {

    /**
     * Find all indicators for a specific plan report
     *
     * @param reportId the plan report ID
     * @return list of plan report indicators
     */
    List<PlanReportIndicator> findByReportId(Long reportId);

    /**
     * Find all reports for a specific indicator
     *
     * @param indicatorId the indicator ID
     * @return list of plan report indicators
     */
    List<PlanReportIndicator> findByIndicatorId(Long indicatorId);

    /**
     * Find a specific indicator in a specific report
     *
     * @param reportId the plan report ID
     * @param indicatorId the indicator ID
     * @return optional plan report indicator
     */
    Optional<PlanReportIndicator> findByReportIdAndIndicatorId(Long reportId, Long indicatorId);

    /**
     * Check if an indicator exists in a report
     *
     * @param reportId the plan report ID
     * @param indicatorId the indicator ID
     * @return true if exists, false otherwise
     */
    boolean existsByReportIdAndIndicatorId(Long reportId, Long indicatorId);

    /**
     * Delete all indicators for a specific report
     *
     * @param reportId the plan report ID
     */
    void deleteByReportId(Long reportId);
}
