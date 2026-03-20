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

    private Long id;
    private String taskName;
    private String taskDesc;
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

    public static TaskResponse fromEntity(StrategicTask task, String planStatus) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTaskName(task.getTaskName());
        response.setTaskDesc(task.getTaskDesc());
        response.setTaskCategory(task.getTaskCategory());
        response.setTaskType(task.getTaskType());
        response.setPlanId(task.getPlanId());
        response.setCycleId(task.getCycleId());
        response.setOrgId(task.getOrg() != null ? task.getOrg().getId() : null);
        response.setCreatedByOrgId(task.getCreatedByOrg() != null ? task.getCreatedByOrg().getId() : null);
        response.setSortOrder(task.getSortOrder());
        response.setPlanStatus(planStatus);
        response.setTaskStatus(task.getStatus());
        response.setRemark(task.getRemark());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    public static TaskResponse fromView(TaskFlatView task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTaskName(task.getTaskName());
        response.setTaskDesc(task.getTaskDesc());
        response.setTaskCategory(TaskCategory.STRATEGIC);
        response.setTaskType(parseTaskType(task.getTaskType()));
        response.setPlanId(task.getPlanId());
        response.setCycleId(task.getCycleId());
        response.setOrgId(task.getOrgId());
        response.setCreatedByOrgId(task.getCreatedByOrgId());
        response.setSortOrder(task.getSortOrder());
        response.setPlanStatus(task.getPlanStatus());
        response.setTaskStatus(task.getTaskStatus());
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
            case "REGULAR", "常规", "常规性" -> TaskType.REGULAR;
            case "KEY", "重点" -> TaskType.KEY;
            case "SPECIAL", "专项" -> TaskType.SPECIAL;
            case "QUANTITATIVE", "量化" -> TaskType.QUANTITATIVE;
            case "DEVELOPMENT", "发展", "发展性" -> TaskType.DEVELOPMENT;
            default -> null;
        };
    }
}
