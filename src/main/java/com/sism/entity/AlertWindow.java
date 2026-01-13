package com.sism.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Alert window entity
 * Defines cutoff dates for alert calculations
 */
@Getter
@Setter
@Entity
@Table(name = "alert_window")
public class AlertWindow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "window_id")
    private Long windowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AssessmentCycle cycle;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "cutoff_date", nullable = false)
    private LocalDate cutoffDate;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
