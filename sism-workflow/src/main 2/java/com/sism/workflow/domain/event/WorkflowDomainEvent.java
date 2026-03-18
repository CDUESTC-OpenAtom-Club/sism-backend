package com.sism.workflow.domain.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工作流领域事件基类
 * 表示工作流相关的领域事件
 */
@Getter
public abstract class WorkflowDomainEvent implements DomainEvent {

    protected final String eventId;
    protected final LocalDateTime occurredOn;
    protected final String workflowId;

    public WorkflowDomainEvent(String workflowId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.workflowId = workflowId;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public abstract String getEventType();
}
