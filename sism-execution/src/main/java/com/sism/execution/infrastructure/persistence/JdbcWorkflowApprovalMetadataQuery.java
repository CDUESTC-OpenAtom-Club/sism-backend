package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.report.WorkflowApprovalMetadata;
import com.sism.execution.domain.report.WorkflowApprovalMetadataQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JdbcWorkflowApprovalMetadataQuery implements WorkflowApprovalMetadataQuery {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<Long, WorkflowApprovalMetadata> findByAuditInstanceIds(Iterable<Long> auditInstanceIds) {
        List<Long> ids = normalizeIds(auditInstanceIds);
        if (ids.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Long, WorkflowApprovalMetadata> metadataByAuditId = new HashMap<>();
        List<Map<String, Object>> approvalRows = jdbcTemplate.queryForList(
                """
                SELECT DISTINCT ON (ai.id)
                    ai.id AS audit_id,
                    ai.requester_id,
                    ai.status,
                    asi.approver_id,
                    COALESCE(asi.approved_at, ai.completed_at) AS approved_at,
                    asi.step_no AS step_no,
                    asi.id AS step_instance_id
                FROM public.audit_instance ai
                LEFT JOIN public.audit_step_instance asi
                  ON asi.instance_id = ai.id
                WHERE ai.id IN (%s)
                ORDER BY ai.id,
                         COALESCE(asi.approved_at, ai.completed_at) DESC,
                         asi.step_no DESC,
                         asi.id DESC
                """.formatted(placeholders),
                ids.toArray()
        );
        for (Map<String, Object> row : approvalRows) {
            Object auditId = row.get("audit_id");
            if (!(auditId instanceof Number auditIdNumber)) {
                continue;
            }
            Long requesterId = null;
            Object requesterIdValue = row.get("requester_id");
            if (requesterIdValue instanceof Number requesterIdNumber) {
                requesterId = requesterIdNumber.longValue();
            }
            Object statusValue = row.get("status");
            String status = statusValue != null ? statusValue.toString() : null;
            boolean approved = "APPROVED".equals(status);
            Object approverId = row.get("approver_id");
            Object approvedAt = row.get("approved_at");
            Long auditIdValue = auditIdNumber.longValue();
            metadataByAuditId.put(
                    auditIdValue,
                    new WorkflowApprovalMetadata(
                            requesterId,
                            approved && approverId instanceof Number approverIdNumber
                                    ? approverIdNumber.longValue()
                                    : null,
                            approved ? toLocalDateTime(approvedAt) : null
                    )
            );
        }
        return metadataByAuditId;
    }

    private List<Long> normalizeIds(Iterable<Long> auditInstanceIds) {
        if (auditInstanceIds == null) {
            return List.of();
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long auditInstanceId : auditInstanceIds) {
            if (auditInstanceId != null && auditInstanceId > 0) {
                normalized.add(auditInstanceId);
            }
        }
        return new ArrayList<>(normalized);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return new java.sql.Timestamp(date.getTime()).toLocalDateTime();
        }
        return null;
    }
}
