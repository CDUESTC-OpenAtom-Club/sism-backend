package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.report.WorkflowAuditSyncGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class JdbcWorkflowAuditSyncGateway implements WorkflowAuditSyncGateway {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void markApproved(Long auditInstanceId) {
        updateTerminalStatus(auditInstanceId, "APPROVED");
    }

    @Override
    public void markRejected(Long auditInstanceId) {
        updateTerminalStatus(auditInstanceId, "REJECTED");
    }

    private void updateTerminalStatus(Long auditInstanceId, String status) {
        if (auditInstanceId == null || auditInstanceId <= 0) {
            return;
        }

        jdbcTemplate.update(
                """
                UPDATE public.audit_instance
                SET status = ?, completed_at = ?
                WHERE id = ?
                """,
                status,
                LocalDateTime.now(),
                auditInstanceId
        );
    }
}
