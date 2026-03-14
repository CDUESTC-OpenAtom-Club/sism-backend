package com.sism.shared.domain.model.workflow;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WorkflowTask aggregate root - manages workflow task state
 */
@Getter
@Setter
@Entity
@Table(name = "workflow_task")
public class WorkflowTask extends AggregateRoot<Long> {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "workflow_type", nullable = false)
    private String workflowType;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "task_type")
    private String taskType;

    @Column(nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "next_step")
    private String nextStep;

    @Column(name = "initiator_id")
    private Long initiatorId;

    @Column(name = "initiator_org_id")
    private Long initiatorOrgId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "assignee_org_id")
    private Long assigneeOrgId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @ElementCollection
    @CollectionTable(name = "workflow_task_history", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "history")
    private List<String> history = new ArrayList<>();

    @Override
    public boolean canPublish() {
        return STATUS_COMPLETED.equals(status) || STATUS_FAILED.equals(status);
    }

    @Override
    public void validate() {
        if (workflowId == null || workflowId.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow ID is required");
        }
        if (taskName == null || taskName.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }
    }

    public void start(Long operatorId, Long operatorOrgId) {
        this.status = STATUS_RUNNING;
        this.startedAt = LocalDateTime.now();
        this.assigneeId = operatorId;
        this.assigneeOrgId = operatorOrgId;
        addHistory("Task started by operator: " + operatorId);
    }

    public void complete(String result) {
        this.status = STATUS_COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.result = result;
        addHistory("Task completed with result: " + result);
    }

    public void fail(String errorMessage) {
        this.status = STATUS_FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        addHistory("Task failed: " + errorMessage);
    }

    public void cancel() {
        this.status = STATUS_CANCELLED;
        this.completedAt = LocalDateTime.now();
        addHistory("Task cancelled");
    }

    public void moveToNextStep(String nextStep) {
        this.currentStep = this.nextStep;
        this.nextStep = nextStep;
        addHistory("Moved to step: " + nextStep);
    }

    public void approve(Long approverId, String comment) {
        if (!STATUS_RUNNING.equals(status)) {
            throw new IllegalStateException("Cannot approve: task is not running");
        }
        this.result = "Approved by " + approverId + (comment != null ? ": " + comment : "");
        this.status = STATUS_COMPLETED;
        this.completedAt = LocalDateTime.now();
        addHistory("Approved by user: " + approverId + (comment != null ? " - " + comment : ""));
    }

    public void reject(Long approverId, String comment) {
        if (!STATUS_RUNNING.equals(status)) {
            throw new IllegalStateException("Cannot reject: task is not running");
        }
        this.result = "Rejected by " + approverId + (comment != null ? ": " + comment : "");
        this.status = STATUS_FAILED;
        this.completedAt = LocalDateTime.now();
        addHistory("Rejected by user: " + approverId + (comment != null ? " - " + comment : ""));
    }

    private void addHistory(String entry) {
        String timestamp = LocalDateTime.now().toString();
        history.add("[" + timestamp + "] " + entry);
    }

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
