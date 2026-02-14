package com.sism.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Assessment cycle entity
 * Defines the time range for annual assessments
 */
@Data
@Entity
@Table(name = "cycle")
public class AssessmentCycle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long cycleId;

    @Column(name = "cycle_name", nullable = false, length = 100)
    private String cycleName;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String description;
}
