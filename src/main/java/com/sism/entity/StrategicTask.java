package com.sism.entity;

import com.sism.enums.TaskType;
import com.sism.vo.TaskVO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Task entity
 * Maps to strategic_task table in database
 */
@Entity
@Table(name = "sys_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategicTask {

    @Id
    @SequenceGenerator(name="Task_IdSeq", sequenceName="public.sys_task_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="Task_IdSeq")
    @Column(name="task_id")
    private Long taskId;

    @Column(name="plan_id", nullable=false)
    private Long planId;

    @Column(name="cycle_id", nullable=false)
    private Long cycleId;

    @Column(name="task_name", nullable=false)
    private String taskName;

    @Column(name="task_desc")
    private String taskDesc;

    @Enumerated(EnumType.STRING)
    @Column(name="task_type", nullable=false)
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

    // ==================== toDTO Method ====================

    /**
     * Convert this StrategicTask entity to TaskVO
     *
     * **Validates: Requirements 4.2**
     *
     * @return TaskVO with all field mappings
     */
    public TaskVO toDTO() {
        return new TaskVO(
            this.taskId,
            this.planId,
            this.cycleId,
            null, // cycleName - should be set by caller
            null, // year - should be set by caller
            this.taskName,
            this.taskDesc,
            this.taskType,
            this.sortOrder,
            this.remark,
            this.createdAt,
            this.updatedAt
        );
    }

    /**
     * Convert this StrategicTask entity to TaskVO with cycle info
     *
     * **Validates: Requirements 4.2**
     *
     * @param cycleName Cycle name to include
     * @param year      Year to include
     * @return TaskVO with all field mappings
     */
    public TaskVO toDTO(String cycleName, Integer year) {
        return new TaskVO(
            this.taskId,
            this.planId,
            this.cycleId,
            cycleName,
            year,
            this.taskName,
            this.taskDesc,
            this.taskType,
            this.sortOrder,
            this.remark,
            this.createdAt,
            this.updatedAt
        );
    }
}
