package com.sism.workflow.interfaces.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 工作流实例详情响应 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowInstanceDetailResponse extends WorkflowInstanceResponse {

    private List<WorkflowTaskResponse> tasks;
    private List<WorkflowHistoryResponse> history;

    public WorkflowInstanceDetailResponse() {
        super();
    }

    public WorkflowInstanceDetailResponse(
            String instanceId, String definitionId, String status, Long businessEntityId,
            Long starterId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
            List<WorkflowTaskResponse> tasks, List<WorkflowHistoryResponse> history) {
        super(instanceId, definitionId, status, businessEntityId, starterId, startTime, endTime);
        this.tasks = tasks;
        this.history = history;
    }
}
