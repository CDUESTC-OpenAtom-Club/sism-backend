package com.sism.task.application.dto;

import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskCategory;
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
    private TaskCategory taskCategory;
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
        response.setTaskCategory(task.getTaskCategory() != null ? task.getTaskCategory() : TaskCategory.STRATEGIC);
        response.setTaskType(task.getTaskType());
        response.setPlanId(task.getPlanId());
        response.setCycleId(task.getCycleId());
        response.setOrgId(task.getOrg() != null ? task.getOrg().getId() : null);
        response.setCreatedByOrgId(task.getCreatedByOrg() != null ? task.getCreatedByOrg().getId() : null);
        response.setSortOrder(task.getSortOrder());
        response.setPlanStatus(DEFAULT_PLAN_STATUS);
        response.setTaskStatus(defaultStatus(task.getStatus()));
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
        response.setTaskCategory(parseTaskCategory(task.getTaskCategory()));
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

    static TaskCategory parseTaskCategory(String rawTaskCategory) {
        if (rawTaskCategory == null || rawTaskCategory.isBlank()) {
            return TaskCategory.STRATEGIC;
        }
        try {
            return TaskCategory.valueOf(rawTaskCategory.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return TaskCategory.STRATEGIC;
        }
    }

    static TaskType parseTaskType(String rawTaskType) {
        if (rawTaskType == null || rawTaskType.isBlank()) {
            return null;
        }

        String normalized = rawTaskType.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "PLAN", "计划" -> TaskType.PLAN;
            case "BASIC", "基础", "基础性" -> TaskType.BASIC;
            case "REGULAR", "常规", "常规性" -> TaskType.REGULAR;
            case "KEY", "重点" -> TaskType.KEY;
            case "SPECIAL", "专项" -> TaskType.SPECIAL;
            case "QUANTITATIVE", "量化" -> TaskType.QUANTITATIVE;
            case "DEVELOPMENT", "发展", "发展性" -> TaskType.DEVELOPMENT;
            default -> null;
        };
    }

    private static String defaultStatus(String rawStatus) {
        return rawStatus == null || rawStatus.isBlank() ? DEFAULT_STATUS : rawStatus;
    }

    private static String defaultPlanStatus(String rawPlanStatus) {
        return rawPlanStatus == null || rawPlanStatus.isBlank() ? DEFAULT_PLAN_STATUS : rawPlanStatus;
    }
}
