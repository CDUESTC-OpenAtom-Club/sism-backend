package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.repository.PlanReportIndicatorRepository;
import com.sism.execution.domain.repository.PlanReportAttachmentSnapshot;
import com.sism.execution.domain.repository.PlanReportIndicatorSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JdbcPlanReportIndicatorRepository implements PlanReportIndicatorRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Long upsertDraftIndicator(Long reportId, Long indicatorId, Integer progress, String comment, String milestoneNote) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO public.plan_report_indicator (report_id, indicator_id, progress, milestone_note, comment, created_at)
                VALUES (?, ?, ?, ?, ?, now())
                ON CONFLICT (report_id, indicator_id) DO UPDATE SET
                    progress = EXCLUDED.progress,
                    milestone_note = EXCLUDED.milestone_note,
                    comment = EXCLUDED.comment,
                    created_at = now()
                RETURNING id
                """,
                Long.class,
                reportId,
                indicatorId,
                progress == null ? 0 : progress,
                milestoneNote,
                comment
        );
    }

    @Override
    public void attachFiles(Long planReportIndicatorId, List<Long> attachmentIds, Long createdBy) {
        if (planReportIndicatorId == null) {
            return;
        }

        List<Long> normalizedAttachmentIds = attachmentIds == null
                ? List.of()
                : attachmentIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (normalizedAttachmentIds.isEmpty()) {
            jdbcTemplate.update(
                    "DELETE FROM public.plan_report_indicator_attachment WHERE plan_report_indicator_id = ?",
                    planReportIndicatorId
            );
            return;
        }

        String placeholders = normalizedAttachmentIds.stream()
                .map(ignored -> "?")
                .reduce((left, right) -> left + "," + right)
                .orElse("?");

        List<Object> deleteParams = new ArrayList<>();
        deleteParams.add(planReportIndicatorId);
        deleteParams.addAll(normalizedAttachmentIds);
        jdbcTemplate.update(
                """
                DELETE FROM public.plan_report_indicator_attachment
                WHERE plan_report_indicator_id = ?
                  AND attachment_id NOT IN (%s)
                """.formatted(placeholders),
                deleteParams.toArray()
        );

        int sortOrder = 0;
        for (Long attachmentId : normalizedAttachmentIds) {
            jdbcTemplate.update(
                    """
                    INSERT INTO public.plan_report_indicator_attachment (
                        plan_report_indicator_id,
                        attachment_id,
                        sort_order,
                        created_by,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, now())
                    ON CONFLICT (plan_report_indicator_id, attachment_id) DO NOTHING
                    """,
                    planReportIndicatorId,
                    attachmentId,
                    sortOrder++,
                    createdBy == null ? 0L : createdBy
            );
        }
    }

    @Override
    public List<PlanReportIndicatorSnapshot> findByReportId(Long reportId) {
        List<IndexedSnapshot> indexedSnapshots = jdbcTemplate.query(
                """
                SELECT id, indicator_id, progress, comment, milestone_note
                FROM public.plan_report_indicator
                WHERE report_id = ?
                ORDER BY id ASC
                """,
                (rs, rowNum) -> new IndexedSnapshot(
                        rs.getLong("id"),
                        new PlanReportIndicatorSnapshot(
                                rs.getLong("indicator_id"),
                                rs.getInt("progress"),
                                rs.getString("comment"),
                                rs.getString("milestone_note"),
                                List.of()
                        )
                ),
                reportId
        );

        if (indexedSnapshots.isEmpty()) {
            return List.of();
        }

        Map<Long, List<PlanReportAttachmentSnapshot>> attachmentsByPriId = new HashMap<>();
        String placeholders = indexedSnapshots.stream()
                .map(ignored -> "?")
                .reduce((left, right) -> left + "," + right)
                .orElse("?");

        Object[] params = indexedSnapshots.stream()
                .map(IndexedSnapshot::id)
                .toArray();
        jdbcTemplate.query(
                """
                SELECT pria.plan_report_indicator_id,
                       a.id,
                       a.original_name,
                       a.size_bytes,
                       a.content_type,
                       COALESCE(NULLIF(a.public_url, ''), CONCAT('/api/v1/attachments/', a.id, '/download')) AS url,
                       a.uploaded_by,
                       a.uploaded_at
                FROM public.plan_report_indicator_attachment pria
                JOIN public.attachment a ON a.id = pria.attachment_id
                WHERE pria.plan_report_indicator_id IN (%s)
                  AND COALESCE(a.is_deleted, false) = false
                ORDER BY pria.plan_report_indicator_id ASC, pria.sort_order ASC, pria.id ASC
                """.formatted(placeholders),
                rs -> {
                    Long planReportIndicatorId = rs.getLong("plan_report_indicator_id");
                    OffsetDateTime uploadedAt = rs.getObject("uploaded_at", OffsetDateTime.class);
                    attachmentsByPriId.computeIfAbsent(planReportIndicatorId, ignored -> new ArrayList<>())
                            .add(new PlanReportAttachmentSnapshot(
                                    rs.getLong("id"),
                                    rs.getString("original_name"),
                                    rs.getLong("size_bytes"),
                                    rs.getString("content_type"),
                                    rs.getString("url"),
                                    rs.getLong("uploaded_by"),
                                    uploadedAt == null ? null : uploadedAt.toString()
                            ));
                },
                params
        );

        return indexedSnapshots.stream()
                .map(indexed -> new PlanReportIndicatorSnapshot(
                        indexed.snapshot().indicatorId(),
                        indexed.snapshot().progress(),
                        indexed.snapshot().comment(),
                        indexed.snapshot().milestoneNote(),
                        attachmentsByPriId.getOrDefault(indexed.id(), List.of())
                ))
                .toList();
    }

    private record IndexedSnapshot(Long id, PlanReportIndicatorSnapshot snapshot) {
    }
}
