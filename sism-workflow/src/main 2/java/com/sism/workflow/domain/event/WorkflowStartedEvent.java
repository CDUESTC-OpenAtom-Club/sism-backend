package com.sism.workflow.domain.event;

import lombok.Getter;

/**
 * 工作流启动事件
 */
@Getter
public class WorkflowStartedEvent extends WorkflowDomainEvent {

    private final String initiatorId;
    private final String businessKey;

    public WorkflowStartedEvent(String workflowId, String initiatorId, String businessKey) {
        super(workflowId);
        this.initiatorId = initiatorId;
        this.businessKey = businessKey;
    }

    @Override
    public String getEventType() {
        return "WORKFLOW_STARTED";
    }
}
