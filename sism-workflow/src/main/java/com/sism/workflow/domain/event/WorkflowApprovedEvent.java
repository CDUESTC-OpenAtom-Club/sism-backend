package com.sism.workflow.domain.event;

import lombok.Getter;

/**
 * 工作流审批通过事件
 */
@Getter
public class WorkflowApprovedEvent extends WorkflowDomainEvent {

    private final String approverId;
    private final String stepName;
    private final String remark;

    public WorkflowApprovedEvent(String workflowId, String approverId, String stepName, String remark) {
        super(workflowId);
        this.approverId = approverId;
        this.stepName = stepName;
        this.remark = remark;
    }

    @Override
    public String getEventType() {
        return "WORKFLOW_APPROVED";
    }
}
