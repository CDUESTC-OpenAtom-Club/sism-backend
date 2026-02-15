package com.sism.repository;

import com.sism.entity.PlanReportIndicatorAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PlanReportIndicatorAttachment entity
 */
@Repository
public interface PlanReportIndicatorAttachmentRepository extends JpaRepository<PlanReportIndicatorAttachment, Long> {

    /**
     * Find all attachments for a specific plan report indicator
     *
     * @param planReportIndicatorId the plan report indicator ID
     * @return list of attachments ordered by sort order
     */
    List<PlanReportIndicatorAttachment> findByPlanReportIndicatorIdOrderBySortOrderAsc(Long planReportIndicatorId);

    /**
     * Find all plan report indicators that use a specific attachment
     *
     * @param attachmentId the attachment ID
     * @return list of plan report indicator attachments
     */
    List<PlanReportIndicatorAttachment> findByAttachmentId(Long attachmentId);

    /**
     * Find a specific attachment for a plan report indicator
     *
     * @param planReportIndicatorId the plan report indicator ID
     * @param attachmentId the attachment ID
     * @return optional plan report indicator attachment
     */
    Optional<PlanReportIndicatorAttachment> findByPlanReportIndicatorIdAndAttachmentId(
        Long planReportIndicatorId,
        Long attachmentId
    );

    /**
     * Check if an attachment is already associated with a plan report indicator
     *
     * @param planReportIndicatorId the plan report indicator ID
     * @param attachmentId the attachment ID
     * @return true if exists, false otherwise
     */
    boolean existsByPlanReportIndicatorIdAndAttachmentId(Long planReportIndicatorId, Long attachmentId);

    /**
     * Delete all attachments for a specific plan report indicator
     *
     * @param planReportIndicatorId the plan report indicator ID
     */
    void deleteByPlanReportIndicatorId(Long planReportIndicatorId);

    /**
     * Count attachments for a specific plan report indicator
     *
     * @param planReportIndicatorId the plan report indicator ID
     * @return count of attachments
     */
    long countByPlanReportIndicatorId(Long planReportIndicatorId);
}
