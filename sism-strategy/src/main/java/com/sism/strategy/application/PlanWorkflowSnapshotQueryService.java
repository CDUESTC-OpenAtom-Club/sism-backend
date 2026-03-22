package com.sism.strategy.application;

import com.sism.iam.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlanWorkflowSnapshotQueryService {

    private static final String PLAN_ENTITY_TYPE = "PLAN";

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    public PlanWorkflowSnapshotQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public WorkflowSnapshot getWorkflowSnapshotByPlanId(Long planId) {
        return getWorkflowSnapshot(PLAN_ENTITY_TYPE, planId);
    }

    public Map<Long, WorkflowSnapshot> getWorkflowSnapshotsByPlanIds(Collection<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Map.of();
        }

        List<Long> normalizedPlanIds = planIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedPlanIds.isEmpty()) {
            return Map.of();
        }

        List<?> instanceRows = entityManager.createNativeQuery("""
                SELECT DISTINCT ON (ai.entity_id)
                    ai.entity_id,
                    ai.id,
                    ai.status,
                    ai.requester_id,
                    ai.started_at,
                    ai.completed_at
                FROM audit_instance ai
                WHERE ai.entity_type = :entityType
                  AND ai.entity_id IN :entityIds
                ORDER BY ai.entity_id, ai.started_at DESC NULLS LAST, ai.id DESC
                """)
                .setParameter("entityType", PLAN_ENTITY_TYPE)
                .setParameter("entityIds", normalizedPlanIds)
                .getResultList();

        Map<Long, AuditInstanceRow> latestInstanceByPlanId = new LinkedHashMap<>();
        for (Object row : instanceRows) {
            Object[] columns = (Object[]) row;
            Long planId = asLong(columns[0]);
            String status = asString(columns[2]);
            if (planId == null || "WITHDRAWN".equalsIgnoreCase(status)) {
                continue;
            }
            latestInstanceByPlanId.put(
                    planId,
                    new AuditInstanceRow(
                            asLong(columns[1]),
                            status,
                            asLong(columns[3]),
                            asLocalDateTime(columns[4]),
                            asLocalDateTime(columns[5])
                    )
            );
        }

        if (latestInstanceByPlanId.isEmpty()) {
            return Map.of();
        }

        Map<Long, WorkflowSnapshot> snapshotsByPlanId = new LinkedHashMap<>();
        latestInstanceByPlanId.forEach((planId, instance) -> snapshotsByPlanId.put(
                planId,
                WorkflowSnapshot.builder()
                        .workflowInstanceId(instance.getInstanceId())
                        .workflowStatus(instance.getStatus())
                        .starterId(instance.getRequesterId())
                        .starterName(resolveUserName(instance.getRequesterId()))
                        .startedAt(instance.getStartedAt())
                        .completedAt(instance.getCompletedAt())
                        .build()
        ));

        Map<Long, Long> planIdByInstanceId = latestInstanceByPlanId.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getValue().getInstanceId(), Map.Entry::getKey));
        List<Long> instanceIds = latestInstanceByPlanId.values().stream()
                .map(AuditInstanceRow::getInstanceId)
                .toList();

        hydrateCurrentPendingSteps(instanceIds, planIdByInstanceId, snapshotsByPlanId);
        hydrateWithdrawFlags(instanceIds, planIdByInstanceId, snapshotsByPlanId);
        hydrateLastRejectReasons(instanceIds, planIdByInstanceId, snapshotsByPlanId);

        return snapshotsByPlanId;
    }

    public WorkflowSnapshot getWorkflowSnapshot(String entityType, Long entityId) {
        AuditInstanceRow instance = findLatestInstance(entityType, entityId);
        if (instance == null) {
            return null;
        }

        WorkflowSnapshot snapshot = WorkflowSnapshot.builder()
                .workflowInstanceId(instance.getInstanceId())
                .workflowStatus(instance.getStatus())
                .starterId(instance.getRequesterId())
                .starterName(resolveUserName(instance.getRequesterId()))
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .build();

        List<?> currentSteps = entityManager.createNativeQuery("""
                SELECT asi.step_name, asi.approver_id, asi.status
                FROM audit_step_instance asi
                WHERE asi.instance_id = :instanceId
                  AND asi.status = 'PENDING'
                ORDER BY asi.step_no DESC, asi.id DESC
                LIMIT 1
                """)
                .setParameter("instanceId", instance.getInstanceId())
                .getResultList();

        if (!currentSteps.isEmpty()) {
            Object[] stepRow = (Object[]) currentSteps.get(0);
            Long approverId = asLong(stepRow[1]);
            snapshot.setCurrentStepName(asString(stepRow[0]));
            snapshot.setCurrentApproverId(approverId);
            snapshot.setCurrentApproverName(resolveUserName(approverId));
        }

        List<?> handledSteps = entityManager.createNativeQuery("""
                SELECT COUNT(1)
                FROM audit_step_instance asi
                WHERE asi.instance_id = :instanceId
                  AND asi.status IN ('APPROVED', 'REJECTED')
                """)
                .setParameter("instanceId", instance.getInstanceId())
                .getResultList();
        long handledCount = handledSteps.isEmpty() ? 0L : ((Number) handledSteps.get(0)).longValue();
        boolean canWithdraw = "IN_REVIEW".equalsIgnoreCase(snapshot.getWorkflowStatus()) && handledCount <= 1L;
        snapshot.setCanWithdraw(canWithdraw);
        snapshot.setLastRejectReason(findLastRejectReason(instance.getInstanceId()));
        return snapshot;
    }

    private void hydrateCurrentPendingSteps(List<Long> instanceIds,
                                            Map<Long, Long> planIdByInstanceId,
                                            Map<Long, WorkflowSnapshot> snapshotsByPlanId) {
        if (instanceIds.isEmpty()) {
            return;
        }

        List<?> currentSteps = entityManager.createNativeQuery("""
                SELECT DISTINCT ON (asi.instance_id)
                    asi.instance_id,
                    asi.step_name,
                    asi.approver_id
                FROM audit_step_instance asi
                WHERE asi.instance_id IN :instanceIds
                  AND asi.status = 'PENDING'
                ORDER BY asi.instance_id, asi.step_no DESC, asi.id DESC
                """)
                .setParameter("instanceIds", instanceIds)
                .getResultList();

        Set<Long> approverIds = currentSteps.stream()
                .map(Object[].class::cast)
                .map(row -> asLong(row[2]))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userNamesById = resolveUserNames(approverIds);

        for (Object row : currentSteps) {
            Object[] columns = (Object[]) row;
            Long instanceId = asLong(columns[0]);
            Long planId = planIdByInstanceId.get(instanceId);
            WorkflowSnapshot snapshot = snapshotsByPlanId.get(planId);
            if (snapshot == null) {
                continue;
            }
            Long approverId = asLong(columns[2]);
            snapshot.setCurrentStepName(asString(columns[1]));
            snapshot.setCurrentApproverId(approverId);
            snapshot.setCurrentApproverName(userNamesById.getOrDefault(approverId, approverId == null ? null : "User#" + approverId));
        }
    }

    private void hydrateWithdrawFlags(List<Long> instanceIds,
                                      Map<Long, Long> planIdByInstanceId,
                                      Map<Long, WorkflowSnapshot> snapshotsByPlanId) {
        if (instanceIds.isEmpty()) {
            return;
        }

        List<?> handledCounts = entityManager.createNativeQuery("""
                SELECT asi.instance_id, COUNT(1)
                FROM audit_step_instance asi
                WHERE asi.instance_id IN :instanceIds
                  AND asi.status IN ('APPROVED', 'REJECTED')
                GROUP BY asi.instance_id
                """)
                .setParameter("instanceIds", instanceIds)
                .getResultList();

        Map<Long, Long> handledCountByInstanceId = new HashMap<>();
        for (Object row : handledCounts) {
            Object[] columns = (Object[]) row;
            handledCountByInstanceId.put(asLong(columns[0]), ((Number) columns[1]).longValue());
        }

        planIdByInstanceId.forEach((instanceId, planId) -> {
            WorkflowSnapshot snapshot = snapshotsByPlanId.get(planId);
            if (snapshot == null) {
                return;
            }
            long handledCount = handledCountByInstanceId.getOrDefault(instanceId, 0L);
            boolean canWithdraw = "IN_REVIEW".equalsIgnoreCase(snapshot.getWorkflowStatus()) && handledCount <= 1L;
            snapshot.setCanWithdraw(canWithdraw);
        });
    }

    private void hydrateLastRejectReasons(List<Long> instanceIds,
                                          Map<Long, Long> planIdByInstanceId,
                                          Map<Long, WorkflowSnapshot> snapshotsByPlanId) {
        if (instanceIds.isEmpty()) {
            return;
        }

        List<?> rejectRows = entityManager.createNativeQuery("""
                SELECT DISTINCT ON (asi.instance_id)
                    asi.instance_id,
                    asi.comment
                FROM audit_step_instance asi
                WHERE asi.instance_id IN :instanceIds
                  AND asi.status = 'REJECTED'
                ORDER BY asi.instance_id, asi.approved_at DESC NULLS LAST, asi.created_at DESC NULLS LAST, asi.id DESC
                """)
                .setParameter("instanceIds", instanceIds)
                .getResultList();

        for (Object row : rejectRows) {
            Object[] columns = (Object[]) row;
            Long instanceId = asLong(columns[0]);
            Long planId = planIdByInstanceId.get(instanceId);
            WorkflowSnapshot snapshot = snapshotsByPlanId.get(planId);
            if (snapshot != null) {
                snapshot.setLastRejectReason(asString(columns[1]));
            }
        }
    }

    public List<WorkflowHistoryItem> getWorkflowHistoryByPlanId(Long planId) {
        AuditInstanceRow instance = findLatestInstance(PLAN_ENTITY_TYPE, planId);
        if (instance == null) {
            return List.of();
        }
        return getWorkflowHistory(instance.getInstanceId());
    }

    public List<WorkflowHistoryItem> getWorkflowHistory(Long workflowInstanceId) {
        if (workflowInstanceId == null) {
            return List.of();
        }

        List<WorkflowHistoryItem> history = new ArrayList<>();
        List<?> rows = entityManager.createNativeQuery("""
                SELECT asi.id, asi.step_name, asi.approver_id, asi.status, asi.comment, asi.approved_at, asi.created_at
                FROM audit_step_instance asi
                WHERE asi.instance_id = :instanceId
                  AND asi.status IN ('APPROVED', 'REJECTED')
                ORDER BY asi.step_no ASC, asi.approved_at ASC, asi.created_at ASC
                """)
                .setParameter("instanceId", workflowInstanceId)
                .getResultList();

        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            Long approverId = asLong(columns[2]);
            history.add(WorkflowHistoryItem.builder()
                    .taskId(asLong(columns[0]))
                    .stepName(asString(columns[1]))
                    .operatorId(approverId)
                    .operatorName(resolveUserName(approverId))
                    .action("REJECTED".equalsIgnoreCase(asString(columns[3])) ? "REJECT" : "APPROVE")
                    .comment(asString(columns[4]))
                    .operateTime(asLocalDateTime(columns[5]) != null ? asLocalDateTime(columns[5]) : asLocalDateTime(columns[6]))
                    .build());
        }
        return history;
    }

    private AuditInstanceRow findLatestInstance(String entityType, Long entityId) {
        if (entityId == null || entityType == null || entityType.isBlank()) {
            return null;
        }

        List<?> instances = entityManager.createNativeQuery("""
                SELECT ai.id, ai.status, ai.requester_id, ai.started_at, ai.completed_at
                FROM audit_instance ai
                WHERE ai.entity_type = :entityType
                  AND ai.entity_id = :entityId
                ORDER BY ai.started_at DESC NULLS LAST, ai.id DESC
                LIMIT 1
                """)
                .setParameter("entityType", entityType)
                .setParameter("entityId", entityId)
                .getResultList();
        if (instances.isEmpty()) {
            return null;
        }

        Object[] row = (Object[]) instances.get(0);
        if ("WITHDRAWN".equalsIgnoreCase(asString(row[1]))) {
            return null;
        }
        return new AuditInstanceRow(
                asLong(row[0]),
                asString(row[1]),
                asLong(row[2]),
                asLocalDateTime(row[3]),
                asLocalDateTime(row[4]));
    }

    private String findLastRejectReason(Long workflowInstanceId) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT asi.comment
                FROM audit_step_instance asi
                WHERE asi.instance_id = :instanceId
                  AND asi.status = 'REJECTED'
                ORDER BY asi.approved_at DESC NULLS LAST, asi.created_at DESC NULLS LAST, asi.id DESC
                LIMIT 1
                """)
                .setParameter("instanceId", workflowInstanceId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        return asString(rows.get(0));
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(user -> user.getRealName() != null && !user.getRealName().isBlank() ? user.getRealName() : user.getUsername())
                .orElse("User#" + userId);
    }

    private Map<Long, String> resolveUserNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> namesById = new HashMap<>();
        userIds.stream()
                .filter(Objects::nonNull)
                .forEach(userId -> userRepository.findById(userId).ifPresent(user -> namesById.put(
                        user.getId(),
                        user.getRealName() != null && !user.getRealName().isBlank() ? user.getRealName() : user.getUsername()
                )));
        return namesById;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowSnapshot {
        private Long workflowInstanceId;
        private String workflowStatus;
        private Long starterId;
        private String starterName;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String currentStepName;
        private Long currentApproverId;
        private String currentApproverName;
        private Boolean canWithdraw;
        private String lastRejectReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowHistoryItem {
        private Long taskId;
        private String stepName;
        private Long operatorId;
        private String operatorName;
        private String action;
        private String comment;
        private LocalDateTime operateTime;
    }

    @Data
    @AllArgsConstructor
    private static class AuditInstanceRow {
        private Long instanceId;
        private String status;
        private Long requesterId;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }
}
