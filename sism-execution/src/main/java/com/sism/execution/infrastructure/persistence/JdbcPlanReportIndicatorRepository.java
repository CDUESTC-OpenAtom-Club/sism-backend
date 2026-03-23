package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.repository.PlanReportIndicatorRepository;
import com.sism.execution.domain.repository.PlanReportIndicatorSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcPlanReportIndicatorRepository implements PlanReportIndicatorRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void upsertDraftIndicator(Long reportId, Long indicatorId, Integer progress, String comment, String milestoneNote) {
        jdbcTemplate.update(
                """
                INSERT INTO public.plan_report_indicator (report_id, indicator_id, progress, milestone_note, comment, created_at)
                VALUES (?, ?, ?, ?, ?, now())
                ON CONFLICT (report_id, indicator_id) DO UPDATE SET
                    progress = EXCLUDED.progress,
                    milestone_note = EXCLUDED.milestone_note,
                    comment = EXCLUDED.comment,
                    created_at = now()
                """,
                reportId,
                indicatorId,
                progress == null ? 0 : progress,
                milestoneNote,
                comment
        );
    }

    @Override
    public List<PlanReportIndicatorSnapshot> findByReportId(Long reportId) {
        return jdbcTemplate.query(
                """
                SELECT indicator_id, progress, comment, milestone_note
                FROM public.plan_report_indicator
                WHERE report_id = ?
                ORDER BY id ASC
                """,
                (rs, rowNum) -> new PlanReportIndicatorSnapshot(
                        rs.getLong("indicator_id"),
                        rs.getInt("progress"),
                        rs.getString("comment"),
                        rs.getString("milestone_note")
                ),
                reportId
        );
    }
}
