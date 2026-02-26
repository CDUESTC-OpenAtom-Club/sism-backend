package com.sism.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "plan_report", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanReport {

    @Id
    @SequenceGenerator(name="PlanReport_IdSeq", sequenceName="public.plan_report_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="PlanReport_IdSeq")
    @Column(name="id")
    private Long id;

    @Column(name="plan_id", nullable=false)
    private Long planId;

    @Column(name="report_month", nullable=false)
    private String reportMonth;

    @Column(name="created_by", nullable=false)
    private Long createdBy;

    @Column(name="report_org_type", nullable=false)
    private String reportOrgType;

    @Column(name="report_org_id", nullable=false)
    private Long reportOrgId;

    @Column(name="status", nullable=false)
    private String status;

    @Column(name="submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name="remark")
    private String remark;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;

    @Column(name="is_deleted", nullable=false)
    private Boolean isDeleted;

}
