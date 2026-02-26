package com.sism.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * PlanReportIndicator Entity
 * 
 * Represents the relationship between plan reports and indicators,
 * tracking progress and comments for each indicator in a report.
 */
@Entity
@Table(name = "plan_report_indicator", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanReportIndicator {

    @Id
    @SequenceGenerator(
        name = "PlanReportIndicator_IdSeq",
        sequenceName = "public.plan_report_indicator_id_seq",
        allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PlanReportIndicator_IdSeq")
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Report ID is required")
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @NotNull(message = "Indicator ID is required")
    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @NotNull(message = "Progress is required")
    @Min(value = 0, message = "Progress must be at least 0")
    @Max(value = 100, message = "Progress must not exceed 100")
    @Column(name = "progress", nullable = false)
    private Integer progress;

    @Column(name = "milestone_note", columnDefinition = "TEXT")
    private String milestoneNote;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @NotNull(message = "Created at timestamp is required")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (progress == null) {
            progress = 0;
        }
    }
}
