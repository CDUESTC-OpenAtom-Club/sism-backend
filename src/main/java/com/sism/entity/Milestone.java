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
@Table(name = "milestone")
public class Milestone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "milestone_id")
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

    @Column(name = "weight_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal weightPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneStatus status = MilestoneStatus.NOT_STARTED;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inherited_from")
    private Milestone inheritedFrom;
}
