package com.sism.execution.domain.model.report;

import com.sism.execution.domain.model.report.event.PlanReportSubmittedEvent;
import com.sism.execution.domain.model.report.event.PlanReportApprovedEvent;
import com.sism.execution.domain.model.report.event.PlanReportRejectedEvent;
import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PlanReport - 月度进展报告聚合根
 * 表示指标执行过程中的月度报告
 */
@Getter
@Setter
@Entity
@Table(name = "plan_report")
@Access(AccessType.FIELD)
public class PlanReport extends AggregateRoot<Long> {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SUBMITTED = "IN_REVIEW";  // Database uses IN_REVIEW, not SUBMITTED
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @SequenceGenerator(name = "PlanReport_IdSeq", sequenceName = "plan_report_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PlanReport_IdSeq")
    @Column(name = "id")
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "report_month", nullable = false)
    private String reportMonth;

    @Column(name = "report_org_id", nullable = false)
    private Long reportOrgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_org_type", nullable = false)
    private ReportOrgType reportOrgType;

    @Column(name = "status", length = 20, nullable = false)
    private String status = STATUS_DRAFT;

    // Fields not in database - marked as transient for future use or removed if not needed
    @Transient
    private String title;

    @Transient
    private String content;

    @Transient
    private String summary;

    @Transient
    private Integer progress;

    @Transient
    private String issues;

    @Transient
    private String nextPlan;

    @Transient
    private Long submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Transient
    private Long approvedBy;

    @Transient
    private LocalDateTime approvedAt;

    @Transient
    private String rejectionReason;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 创建月度报告（草稿状态）
     * @param reportMonth 报告月份 (格式: yyyy-MM)
     * @param reportOrgId 报告组织ID
     * @param reportOrgType 报告组织类型 (FUNC_DEPT 或 COLLEGE)
     * @param planId 计划ID (可选，可为null)
     */
    public static PlanReport createDraft(String reportMonth, Long reportOrgId,
                                          ReportOrgType reportOrgType, Long planId) {
        if (reportMonth == null || reportMonth.trim().isEmpty()) {
            throw new IllegalArgumentException("Report month cannot be null or empty");
        }
        if (reportOrgId == null) {
            throw new IllegalArgumentException("Report organization ID cannot be null");
        }
        if (reportOrgType == null) {
            throw new IllegalArgumentException("Report organization type cannot be null");
        }

        PlanReport report = new PlanReport();
        report.reportMonth = reportMonth;
        report.reportOrgId = reportOrgId;
        report.reportOrgType = reportOrgType;
        report.planId = planId != null ? planId : 1L;  // Default to 1 if null
        report.status = STATUS_DRAFT;
        report.isDeleted = false;
        return report;
    }

    /**
     * 提交报告
     */
    public void submit(Long userId) {
        if (!STATUS_DRAFT.equals(this.status)) {
            throw new IllegalStateException("Cannot submit report: not in DRAFT status");
        }
        this.status = STATUS_SUBMITTED;
        // submittedBy is transient - not storing in DB
        this.submittedAt = LocalDateTime.now();
        setUpdatedAt(LocalDateTime.now());
        addEvent(new PlanReportSubmittedEvent(this.id, this.reportMonth, this.reportOrgId));
    }

    /**
     * 审批通过
     */
    public void approve(Long userId) {
        if (!STATUS_SUBMITTED.equals(this.status)) {
            throw new IllegalStateException("Cannot approve report: not in SUBMITTED status");
        }
        this.status = STATUS_APPROVED;
        // approvedBy and approvedAt are transient - not storing in DB
        setUpdatedAt(LocalDateTime.now());
        addEvent(new PlanReportApprovedEvent(this.id, this.reportMonth, this.reportOrgId));
    }

    /**
     * 审批驳回
     */
    public void reject(Long userId, String reason) {
        if (!STATUS_SUBMITTED.equals(this.status)) {
            throw new IllegalStateException("Cannot reject report: not in SUBMITTED status");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason cannot be null or empty");
        }
        this.status = STATUS_REJECTED;
        // rejectionReason is transient - not storing in DB
        setUpdatedAt(LocalDateTime.now());
        addEvent(new PlanReportRejectedEvent(this.id, this.reportMonth, this.reportOrgId, reason));
    }

    /**
     * 更新报告内容
     * Note: content, summary, progress, issues, next_plan are not stored in DB
     * This method only updates the timestamp for now
     */
    public void updateContent(String content, String summary, Integer progress,
                              String issues, String nextPlan) {
        if (!STATUS_DRAFT.equals(this.status)) {
            throw new IllegalStateException("Cannot update report: not in DRAFT status");
        }
        // These fields are transient - store in remark field instead if needed
        setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 判断是否已提交
     */
    public boolean isSubmitted() {
        return STATUS_SUBMITTED.equals(this.status);
    }

    /**
     * 判断是否已审批通过
     */
    public boolean isApproved() {
        return STATUS_APPROVED.equals(this.status);
    }

    /**
     * 判断是否被驳回
     */
    public boolean isRejected() {
        return STATUS_REJECTED.equals(this.status);
    }

    @Override
    public void validate() {
        if (reportMonth == null || reportMonth.trim().isEmpty()) {
            throw new IllegalArgumentException("Report month is required");
        }
        if (reportOrgId == null) {
            throw new IllegalArgumentException("Report organization ID is required");
        }
        if (reportOrgType == null) {
            throw new IllegalArgumentException("Report organization type is required");
        }
        // planId is nullable - removed check
    }

    @Override
    public boolean canPublish() {
        return STATUS_APPROVED.equals(status);
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = STATUS_DRAFT;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (getCreatedAt() == null) {
            setCreatedAt(LocalDateTime.now());
        }
        if (getUpdatedAt() == null) {
            setUpdatedAt(LocalDateTime.now());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdatedAt(LocalDateTime.now());
    }
}
