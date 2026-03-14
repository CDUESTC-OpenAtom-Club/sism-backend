package com.sism.task.domain.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class TaskCreatedEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long taskId;
    private final String taskName;
    private final Long orgId;

    public TaskCreatedEvent(Long taskId, String taskName, Long orgId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.taskId = taskId;
        this.taskName = taskName;
        this.orgId = orgId;
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
    public String getEventType() {
        return "TASK_CREATED";
    }
}
