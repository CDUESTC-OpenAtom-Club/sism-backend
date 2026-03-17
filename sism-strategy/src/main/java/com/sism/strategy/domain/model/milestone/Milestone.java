package com.sism.strategy.domain.model.milestone;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "IndicatorMilestone")
@Table(name = "indicator_milestone")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @Column(name = "milestone_name", nullable = false)
    private String milestoneName;

    @Column(name = "milestone_desc", columnDefinition = "text")
    private String description;

    @Column(name = "due_date")
    private LocalDateTime targetDate;

    @Column(name = "target_progress")
    private Integer progress;

    @Column(name = "status")
    private String status;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_paired")
    private Boolean isPaired;

    // 注意：数据库中不存在此字段，标记为@Transient避免JPA读取
    @Transient
    private Long inheritedFrom;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
