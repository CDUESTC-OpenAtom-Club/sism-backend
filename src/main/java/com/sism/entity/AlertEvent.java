package com.sism.entity;

import com.sism.enums.AlertSeverity;
import com.sism.enums.AlertStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Alert event entity
 * Generated when progress gaps exceed thresholds
 */
@Getter
@Setter
@Entity
@Table(name = "alert_event")
public class AlertEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicator_id", nullable = false)
    private Indicator indicator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "window_id", nullable = false)
    private AlertWindow window;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AlertRule rule;

    @Column(name = "expected_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal expectedPercent;

    @Column(name = "actual_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal actualPercent;

    @Column(name = "gap_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal gapPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private AppUser handledBy;

    @Column(name = "handled_note", columnDefinition = "TEXT")
    private String handledNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json", columnDefinition = "jsonb")
    private Map<String, Object> detailJson;
}
