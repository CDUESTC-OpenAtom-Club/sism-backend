package com.sism.execution.domain.milestone;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "indicator_milestone")
public class Milestone {

    @Id
    @SequenceGenerator(name="Milestone_IdSeq", sequenceName="public.indicator_milestone_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="Milestone_IdSeq")
    @Column(name = "id")
    private Long id;

    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @Column(name = "milestone_name", nullable = false)
    private String milestoneName;

    @Column(name = "milestone_desc", columnDefinition = "text")
    private String description;

    @Column(name = "due_date")
    private LocalDateTime targetDate;

    @Column(name = "target_progress")
    private Integer progress;

    @Column(name = "status")
    private String status;

    public void setStatus(String status) {
        this.status = MilestoneStatus.normalize(status);
    }

    public void setStatus(MilestoneStatus status) {
        this.status = status == null ? null : status.name();
    }

    @Transient
    public MilestoneStatus getStatusEnum() {
        return MilestoneStatus.from(status);
    }

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_paired")
    private Boolean isPaired;

    @Transient
    private Long inheritedFrom;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
