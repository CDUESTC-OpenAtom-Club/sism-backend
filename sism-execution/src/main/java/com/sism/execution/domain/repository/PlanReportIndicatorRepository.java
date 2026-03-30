package com.sism.execution.domain.repository;

import java.util.List;

/**
 * plan_report_indicator 运行时明细仓储。
 */
public interface PlanReportIndicatorRepository {

    Long upsertDraftIndicator(Long reportId, Long indicatorId, Integer progress, String comment, String milestoneNote);

    void attachFiles(Long planReportIndicatorId, java.util.List<Long> attachmentIds, Long createdBy);

    List<PlanReportIndicatorSnapshot> findByReportId(Long reportId);
}
