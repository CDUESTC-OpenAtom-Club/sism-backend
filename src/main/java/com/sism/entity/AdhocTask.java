package com.sism.entity;

import com.sism.enums.AdhocScopeType;
import com.sism.enums.AdhocTaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Adhoc task entity
 * Temporary statistical tasks outside regular assessment cycles
 */
@Getter
@Setter
@Entity
@Table(name = "adhoc_task")
public class AdhocTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adhoc_task_id")
    private Long adhocTaskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AssessmentCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_org_id", nullable = false)
    private Org creatorOrg;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private AdhocScopeType scopeType = AdhocScopeType.ALL_ORGS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicator_id")
    private Indicator indicator;

    @Column(name = "task_title", nullable = false, length = 200)
    private String taskTitle;

    @Column(name = "task_desc", columnDefinition = "TEXT")
    private String taskDesc;

    @Column(name = "open_at")
    private LocalDate openAt;

    @Column(name = "due_at")
    private LocalDate dueAt;

    @Column(name = "include_in_alert", nullable = false)
    private Boolean includeInAlert = false;

    @Column(name = "require_indicator_report", nullable = false)
    private Boolean requireIndicatorReport = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdhocTaskStatus status = AdhocTaskStatus.DRAFT;
}
