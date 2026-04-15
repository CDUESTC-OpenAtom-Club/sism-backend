package com.sism.task.domain;

import com.sism.organization.domain.SysOrg;
import com.sism.shared.domain.model.base.AggregateRoot;
import com.sism.task.domain.event.TaskCreatedEvent;
import com.sism.task.domain.event.TaskStatusChangedEvent;
import com.sism.task.domain.TaskType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;
/**
 * Current strategic-task implementation within the broader task center.
 */
@Getter
@Entity
@Table(name = "sys_task")
@Access(AccessType.FIELD)
public class StrategicTask extends AggregateRoot<Long> {

    public static final String STATUS_DRAFT = TaskStatus.DRAFT.value();
    public static final String STATUS_ACTIVE = TaskStatus.ACTIVE.value();
    public static final String STATUS_COMPLETED = TaskStatus.COMPLETED.value();
    public static final String STATUS_CANCELLED = TaskStatus.CANCELLED.value();

    @Id
    @SequenceGenerator(name="Task_IdSeq", sequenceName="strategic_task_task_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="Task_IdSeq")
    @Column(name="task_id")
    private Long id;

    @Column(name="plan_id", nullable=false)
    private Long planId;

    @Column(name="cycle_id", nullable=false)
    private Long cycleId;

    @Column(name="name", nullable=false)
    private String name;

    @Column(name="\"desc\"")
    private String desc;

    @Enumerated(EnumType.STRING)
    @Column(name="task_type", nullable=false)
    private TaskType taskType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private SysOrg org;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_org_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private SysOrg createdByOrg;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    @Column(name="is_deleted", nullable=false)
    private Boolean isDeleted = false;

    @Column(name = "status", nullable = false)
    private String status = STATUS_DRAFT;

    public String getPlanStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = TaskStatus.normalize(status);
    }

    public void setStatus(TaskStatus status) {
        this.status = status == null ? null : status.value();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public void setCycleId(Long cycleId) {
        this.cycleId = cycleId;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public void setOrg(SysOrg org) {
        this.org = org;
    }

    public void setCreatedByOrg(SysOrg createdByOrg) {
        this.createdByOrg = createdByOrg;
    }

    @Transient
    public TaskStatus getStatusEnum() {
        return TaskStatus.from(status);
    }

    public static StrategicTask create(String name, TaskType taskType, Long planId, Long cycleId,
                                        SysOrg org, SysOrg createdByOrg) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be null or empty");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("Task type cannot be null");
        }
        if (planId == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }
        if (cycleId == null) {
            throw new IllegalArgumentException("Cycle ID cannot be null");
        }
        if (org == null) {
            throw new IllegalArgumentException("Organization cannot be null");
        }
        if (createdByOrg == null) {
            throw new IllegalArgumentException("Created by org cannot be null");
        }

        StrategicTask task = new StrategicTask();
        task.name = name;
        task.taskType = taskType;
        task.planId = planId;
        task.cycleId = cycleId;
        task.org = org;
        task.createdByOrg = createdByOrg;
        task.sortOrder = 0;
        task.setStatus(TaskStatus.DRAFT);
        task.createdAt = LocalDateTime.now();
        task.updatedAt = LocalDateTime.now();
        task.isDeleted = false;
        return task;
    }

    public void activate() {
        if (getStatusEnum() != TaskStatus.DRAFT) {
            throw new IllegalStateException("Cannot activate task: not in DRAFT state");
        }
        String oldStatus = this.status;
        this.setStatus(TaskStatus.ACTIVE);
        this.updatedAt = LocalDateTime.now();
        this.addEvent(new TaskStatusChangedEvent(this.id, oldStatus, STATUS_ACTIVE));
    }

    public void complete() {
        if (getStatusEnum() != TaskStatus.ACTIVE) {
            throw new IllegalStateException("Cannot complete task: not in ACTIVE state");
        }
        String oldStatus = this.status;
        this.setStatus(TaskStatus.COMPLETED);
        this.updatedAt = LocalDateTime.now();
        this.addEvent(new TaskStatusChangedEvent(this.id, oldStatus, STATUS_COMPLETED));
    }

    public void cancel() {
        TaskStatus currentStatus = getStatusEnum();
        if (currentStatus != TaskStatus.DRAFT && currentStatus != TaskStatus.ACTIVE) {
            throw new IllegalStateException("Cannot cancel task: not in DRAFT or ACTIVE state");
        }
        String oldStatus = this.status;
        this.setStatus(TaskStatus.CANCELLED);
        this.updatedAt = LocalDateTime.now();
        this.addEvent(new TaskStatusChangedEvent(this.id, oldStatus, STATUS_CANCELLED));
    }

    public void updateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be empty");
        }
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDesc(String desc) {
        this.desc = desc;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRemark(String remark) {
        this.remark = remark;
        this.updatedAt = LocalDateTime.now();
    }

    public void markDeleted() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void reassign(TaskType taskType,
                         Long planId,
                         Long cycleId,
                         SysOrg org,
                         SysOrg createdByOrg) {
        if (taskType == null) {
            throw new IllegalArgumentException("Task type cannot be null");
        }
        if (planId == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }
        if (cycleId == null) {
            throw new IllegalArgumentException("Cycle ID cannot be null");
        }
        if (org == null) {
            throw new IllegalArgumentException("Organization cannot be null");
        }
        if (createdByOrg == null) {
            throw new IllegalArgumentException("Created by org cannot be null");
        }
        this.taskType = taskType;
        this.planId = planId;
        this.cycleId = cycleId;
        this.org = org;
        this.createdByOrg = createdByOrg;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("Task type is required");
        }
        if (planId == null) {
            throw new IllegalArgumentException("Plan ID is required");
        }
        if (cycleId == null) {
            throw new IllegalArgumentException("Cycle ID is required");
        }
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            setStatus(TaskStatus.DRAFT);
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PostPersist
    protected void onCreated() {
        if (id != null) {
            addEvent(new TaskCreatedEvent(id, name, org != null ? org.getId() : null));
        }
    }

    @PreUpdate
    protected void onUpdate() {
    }
}
