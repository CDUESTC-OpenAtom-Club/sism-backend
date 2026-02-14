package com.sism.entity;

import com.sism.enums.MilestoneStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Milestone entity
 * Associated with indicator
 */
@Getter
@Setter
@Entity
@Table(name = "indicator_milestone")
public class Milestone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long milestoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicator_id", nullable = false)
    private Indicator indicator;

    @Column(name = "milestone_name", nullable = false, length = 200)
    private String milestoneName;

    @Column(name = "milestone_desc", columnDefinition = "TEXT")
    private String milestoneDesc;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MilestoneStatus status = MilestoneStatus.NOT_STARTED;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inherited_from")
    private Milestone inheritedFrom;

    // ==================== 新增字段 (前端数据对齐) ====================

    /**
     * 目标进度百分比 (0-100)
     * 对应前端 targetProgress
     */
    @Column(name = "target_progress")
    private Integer targetProgress = 0;

    /**
     * 是否已配对（有审核通过的填报记录）
     * 对应前端 isPaired
     */
    @Column(name = "is_paired")
    private Boolean isPaired = false;
}
