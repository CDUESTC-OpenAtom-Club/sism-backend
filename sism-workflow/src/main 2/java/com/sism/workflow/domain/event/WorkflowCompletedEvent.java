package com.sism.workflow.domain.event;

import lombok.Getter;

/**
 * 工作流完成事件
 */
@Getter
public class WorkflowCompletedEvent extends WorkflowDomainEvent {

    private final String finalStatus;
    private final String completionRemark;

    public WorkflowCompletedEvent(String workflowId, String finalStatus, String completionRemark) {
        super(workflowId);
        this.finalStatus = finalStatus;
        this.completionRemark = completionRemark;
    }

    @Override
    public String getEventType() {
        return "WORKFLOW_COMPLETED";
    }
}
