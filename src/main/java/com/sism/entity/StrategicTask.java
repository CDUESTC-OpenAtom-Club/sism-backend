package com.sism.entity;

import com.sism.enums.TaskType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Strategic task entity
 * Associated with assessment cycle and organization
 */
@Getter
@Setter
@Entity
@Table(name = "strategic_task")
public class StrategicTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AssessmentCycle cycle;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "task_desc", columnDefinition = "TEXT")
    private String taskDesc;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType = TaskType.BASIC;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_org_id", nullable = false)
    private Org createdByOrg;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(columnDefinition = "TEXT")
    private String remark;
}
