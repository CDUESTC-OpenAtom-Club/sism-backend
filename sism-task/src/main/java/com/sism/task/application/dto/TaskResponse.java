package com.sism.task.application.dto;

import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TaskResponse - 任务响应DTO
 */
@Data
public class TaskResponse {

    private Long id;
    private String taskName;
    private String taskDesc;
    private TaskType taskType;
    private Long planId;
    private Long cycleId;
    private Long orgId;
    private Long createdByOrgId;
    private Integer sortOrder;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskResponse fromEntity(StrategicTask task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTaskName(task.getTaskName());
        response.setTaskDesc(task.getTaskDesc());
        response.setTaskType(task.getTaskType());
        response.setPlanId(task.getPlanId());
        response.setCycleId(task.getCycleId());
        response.setOrgId(task.getOrg() != null ? task.getOrg().getId() : null);
        response.setCreatedByOrgId(task.getCreatedByOrg() != null ? task.getCreatedByOrg().getId() : null);
        response.setSortOrder(task.getSortOrder());
        response.setStatus(task.getStatus());
        response.setRemark(task.getRemark());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }
}
