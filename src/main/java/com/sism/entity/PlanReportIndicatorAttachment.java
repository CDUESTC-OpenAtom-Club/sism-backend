package com.sism.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * PlanReportIndicatorAttachment Entity
 * 
 * Represents the relationship between plan report indicators and attachments,
 * allowing multiple attachments to be associated with each indicator report.
 */
@Entity
@Table(
    name = "plan_report_indicator_attachment",
    schema = "public",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "plan_report_indicator_attachm_plan_report_indicator_id_atta_key",
            columnNames = {"plan_report_indicator_id", "attachment_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanReportIndicatorAttachment {

    @Id
    @SequenceGenerator(
        name = "PlanReportIndicatorAttachment_IdSeq",
        sequenceName = "public.plan_report_indicator_attachment_id_seq",
        allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PlanReportIndicatorAttachment_IdSeq")
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Plan report indicator ID is required")
    @Column(name = "plan_report_indicator_id", nullable = false)
    private Long planReportIndicatorId;

    @NotNull(message = "Attachment ID is required")
    @Column(name = "attachment_id", nullable = false)
    private Long attachmentId;

    @NotNull(message = "Sort order is required")
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @NotNull(message = "Created by is required")
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @NotNull(message = "Created at timestamp is required")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
