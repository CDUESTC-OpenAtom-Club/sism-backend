package com.sism.entity;

import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Indicator entity with self-referential relationship
 * Supports hierarchical indicator structure
 */
@Getter
@Setter
@Entity
@Table(name = "indicator")
public class Indicator extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_id")
    private Long indicatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private StrategicTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_indicator_id")
    private Indicator parentIndicator;

    @OneToMany(mappedBy = "parentIndicator")
    private List<Indicator> childIndicators = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndicatorLevel level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_org_id", nullable = false)
    private Org ownerOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_org_id", nullable = false)
    private Org targetOrg;

    @Column(name = "indicator_desc", nullable = false, columnDefinition = "TEXT")
    private String indicatorDesc;

    @Column(name = "weight_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal weightPercent = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndicatorStatus status = IndicatorStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @OneToMany(mappedBy = "indicator", cascade = CascadeType.ALL)
    private List<Milestone> milestones = new ArrayList<>();
}
