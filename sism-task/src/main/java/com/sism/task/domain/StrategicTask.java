package com.sism.task.domain;

import com.sism.organization.domain.SysOrg;
import com.sism.shared.domain.model.base.AggregateRoot;
import com.sism.task.domain.event.TaskCreatedEvent;
import com.sism.task.domain.event.TaskStatusChangedEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Current strategic-task implementation within the broader task center.
 */
@Getter
@Setter
@Entity
@Table(name = "sys_task")
@Access(AccessType.FIELD)
public class StrategicTask extends AggregateRoot<Long> {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

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

    @Column(name="desc")
    private String desc;

    @Enumerated(EnumType.STRING)
    @Column(name="task_type", nullable=false)
    private TaskType taskType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private SysOrg org;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @Transient
    private TaskCategory taskCategory = TaskCategory.STRATEGIC;

    // 当前战略任务的 taskStatus 仍为任务域内部状态，不映射数据库列。
    @Transient
    private String status = STATUS_DRAFT;

    public static StrategicTask create(String name, TaskType taskType, Long planId, Long cycleId,
                                        SysOrg org, SysOrg createdByOrg) {
        return create(TaskCategory.STRATEGIC, name, taskType, planId, cycleId, org, createdByOrg);
    }

    public static StrategicTask create(TaskCategory taskCategory, String name, TaskType taskType, Long planId, Long cycleId,
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
        task.taskCategory = taskCategory != null ? taskCategory : TaskCategory.STRATEGIC;
        task.sortOrder = 0;
        task.status = STATUS_DRAFT;
        task.createdAt = LocalDateTime.now();
        task.updatedAt = LocalDateTime.now();
        task.isDeleted = false;
        task.addEvent(new TaskCreatedEvent(task.id, name, org.getId()));
        return task;
    }

    public void activate() {
        if (!STATUS_DRAFT.equals(this.status)) {
            throw new IllegalStateException("Cannot activate task: not in DRAFT state");
        }
        String oldStatus = this.status;
        this.status = STATUS_ACTIVE;
        this.updatedAt = LocalDateTime.now();
        this.addEvent(new TaskStatusChangedEvent(this.id, oldStatus, STATUS_ACTIVE));
    }

    public void complete() {
        if (!STATUS_ACTIVE.equals(this.status)) {
            throw new IllegalStateException("Cannot complete task: not in ACTIVE state");
        }
        String oldStatus = this.status;
        this.status = STATUS_COMPLETED;
        this.updatedAt = LocalDateTime.now();
        this.addEvent(new TaskStatusChangedEvent(this.id, oldStatus, STATUS_COMPLETED));
    }

    public void cancel() {
        if (!STATUS_DRAFT.equals(this.status) && !STATUS_ACTIVE.equals(this.status)) {
            throw new IllegalStateException("Cannot cancel task: not in DRAFT or ACTIVE state");
        }
        String oldStatus = this.status;
        this.status = STATUS_CANCELLED;
        this.updatedAt = LocalDateTime.now();
        this.addEvent(new TaskStatusChangedEvent(this.id, oldStatus, STATUS_CANCELLED));
    }

    public void updateName(String name) {
        if (Objects.isNull(name) || name.trim().isEmpty()) {
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
        if (Objects.isNull(sortOrder) || sortOrder < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }
        this.sortOrder = sortOrder;
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
        if (taskCategory == null) {
            taskCategory = TaskCategory.STRATEGIC;
        }
        if (status == null) {
            status = STATUS_DRAFT;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PostLoad
    protected void onLoad() {
        if (taskCategory == null) {
            taskCategory = TaskCategory.STRATEGIC;
        }
        if (status == null) {
            status = STATUS_DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
    }
}
