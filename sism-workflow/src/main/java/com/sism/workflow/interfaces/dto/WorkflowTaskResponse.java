package com.sism.workflow.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流任务响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTaskResponse {

    private String taskId;
    private String taskName;
    private String taskKey;
    private String status; // PENDING, COMPLETED, REJECTED
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime createdTime;
    private LocalDateTime dueDate;
    private String claimUrl; // 前端可以直接打开的链接
    private Map<String, Object> variables;
}
