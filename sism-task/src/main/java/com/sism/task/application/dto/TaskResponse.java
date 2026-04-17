package com.sism.task.application.dto;

import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import com.sism.task.infrastructure.persistence.TaskFlatView;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * TaskResponse - 任务响应DTO
 */
@Data
public class TaskResponse {

    private static final String DEFAULT_STATUS = "DRAFT";
    private static final String DEFAULT_PLAN_STATUS = "DRAFT";

    private Long id;
    private String name;
    private String desc;
    private TaskType taskType;
    private Long planId;
    private Long cycleId;
    private Long orgId;
    private Long createdByOrgId;
    private Integer sortOrder;
    private String planStatus;
    private String taskStatus;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskResponse fromEntity(StrategicTask task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setName(task.getName());
        response.setDesc(task.getDesc());
        response.setTaskType(task.getTaskType());
        response.setPlanId(task.getPlanId());
        response.setCycleId(task.getCycleId());
        response.setOrgId(task.getOrg() != null ? task.getOrg().getId() : null);
        response.setCreatedByOrgId(task.getCreatedByOrg() != null ? task.getCreatedByOrg().getId() : null);
        response.setSortOrder(task.getSortOrder());
        response.setPlanStatus(DEFAULT_PLAN_STATUS);
        response.setTaskStatus(DEFAULT_PLAN_STATUS);
        response.setRemark(task.getRemark());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    public static TaskResponse fromView(TaskFlatView task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setName(task.getName());
        response.setDesc(task.getDesc());
        response.setTaskType(parseTaskType(task.getTaskType()));
        response.setPlanId(task.getPlanId());
        response.setCycleId(task.getCycleId());
        response.setOrgId(task.getOrgId());
        response.setCreatedByOrgId(task.getCreatedByOrgId());
        response.setSortOrder(task.getSortOrder());
        response.setPlanStatus(defaultPlanStatus(task.getPlanStatus()));
        response.setTaskStatus(defaultStatus(task.getTaskStatus()));
        response.setRemark(task.getRemark());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    static TaskType parseTaskType(String rawTaskType) {
        if (rawTaskType == null || rawTaskType.isBlank()) {
            return null;
        }

        String normalized = rawTaskType.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "BASIC", "基础", "基础性" -> TaskType.BASIC;
            case "DEVELOPMENT", "发展", "发展性" -> TaskType.DEVELOPMENT;
            default -> throw new IllegalStateException("不支持的任务类型: " + rawTaskType);
        };
    }

    private static String defaultStatus(String rawStatus) {
        return rawStatus == null || rawStatus.isBlank() ? DEFAULT_STATUS : rawStatus;
    }

    private static String defaultPlanStatus(String rawPlanStatus) {
        return rawPlanStatus == null || rawPlanStatus.isBlank() ? DEFAULT_PLAN_STATUS : rawPlanStatus;
    }
}
