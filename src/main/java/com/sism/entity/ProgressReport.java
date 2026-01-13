package com.sism.entity;

import com.sism.enums.ReportStatus;
import com.sism.exception.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Progress report entity
 * Implements mutual exclusion constraint: milestone and adhocTask cannot both be non-null
 */
@Getter
@Setter
@Entity
@Table(name = "progress_report")
public class ProgressReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicator_id", nullable = false)
    private Indicator indicator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adhoc_task_id")
    private AdhocTask adhocTask;

    @Column(name = "percent_complete", precision = 5, scale = 2, nullable = false)
    private BigDecimal percentComplete = BigDecimal.ZERO;

    @Column(name = "achieved_milestone", nullable = false)
    private Boolean achievedMilestone = false;

    @Column(columnDefinition = "TEXT")
    private String narrative;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private AppUser reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 1;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    /**
     * Business rule validation: milestone and adhocTask cannot both be non-null
     * This implements the mutual exclusion constraint from Requirements 3.4
     */
    @PrePersist
    @PreUpdate
    private void validateMutualExclusion() {
        if (milestone != null && adhocTask != null) {
            throw new BusinessException("Milestone and adhoc task cannot be associated simultaneously");
        }
    }
}
