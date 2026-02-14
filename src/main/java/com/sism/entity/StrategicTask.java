package com.sism.entity;

import com.sism.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Task entity
 * Maps to task table in database
 */
@Entity
@Table(name = "task", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategicTask {

    @Id
    @SequenceGenerator(name="Task_IdSeq", sequenceName="public.task_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="Task_IdSeq")
    @Column(name="id")
    private Long taskId;

    @Column(name="plan_id", nullable=false)
    private Long planId;

    @Column(name="cycle_id", nullable=false)
    private Long cycleId;

    @Column(name="name", nullable=false)
    private String taskName;

    @Column(name="desc")
    private String taskDesc;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name="type", columnDefinition="task_type", nullable=false)
    private TaskType taskType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private SysOrg org;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_org_id", nullable = false)
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
    private Boolean isDeleted;
}
