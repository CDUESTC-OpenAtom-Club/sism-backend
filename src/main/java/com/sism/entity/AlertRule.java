package com.sism.entity;

import com.sism.enums.AlertSeverity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Alert rule entity
 * Defines thresholds for generating alert events
 */
@Getter
@Setter
@Entity
@Table(name = "alert_rule")
public class AlertRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AssessmentCycle cycle;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity = AlertSeverity.WARNING;

    @Column(name = "gap_threshold", precision = 5, scale = 2, nullable = false)
    private BigDecimal gapThreshold;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;
}
