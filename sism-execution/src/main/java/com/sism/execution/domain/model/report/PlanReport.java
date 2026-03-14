package com.sism.execution.domain.model.report;

import com.sism.execution.domain.model.report.event.PlanReportSubmittedEvent;
import com.sism.execution.domain.model.report.event.PlanReportApprovedEvent;
import com.sism.execution.domain.model.report.event.PlanReportRejectedEvent;
import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PlanReport - 月度进展报告聚合根
 * 表示指标执行过程中的月度报告
 */
@Getter
@Setter
@Entity
@Table(name = "plan_report")
public class PlanReport extends AggregateRoot<Long> {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @SequenceGenerator(name = "PlanReport_IdSeq", sequenceName = "plan_report_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PlanReport_IdSeq")
    @Column(name = "id")
    private Long id;

    @Column(name = "report_month", nullable = false)
    private String reportMonth;

    @Column(name = "report_org_id", nullable = false)
    private Long reportOrgId;

    @Column(name = "report_org_name")
    private String reportOrgName;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_org_type")
    private ReportOrgType reportOrgType;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "issues", columnDefinition = "TEXT")
    private String issues;

    @Column(name = "next_plan", columnDefinition = "TEXT")
    private String nextPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private String status = STATUS_DRAFT;

    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 创建月度报告（草稿状态）
     */
    public static PlanReport createDraft(String reportMonth, Long reportOrgId, String reportOrgName,
                                          ReportOrgType reportOrgType, Long planId) {
        if (reportMonth == null || reportMonth.trim().isEmpty()) {
            throw new IllegalArgumentException("Report month cannot be null or empty");
        }
        if (reportOrgId == null) {
            throw new IllegalArgumentException("Report organization ID cannot be null");
        }

        PlanReport report = new PlanReport();
        report.reportMonth = reportMonth;
        report.reportOrgId = reportOrgId;
        report.reportOrgName = reportOrgName;
        report.reportOrgType = reportOrgType;
        report.planId = planId;
        report.progress = 0;
        report.status = STATUS_DRAFT;
        report.isDeleted = false;
        report.createdAt = LocalDateTime.now();
        report.updatedAt = LocalDateTime.now();
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
        this.submittedBy = userId;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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
        this.approvedBy = userId;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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
        this.approvedBy = userId;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = reason;
        this.updatedAt = LocalDateTime.now();
        addEvent(new PlanReportRejectedEvent(this.id, this.reportMonth, this.reportOrgId, reason));
    }

    /**
     * 更新报告内容
     */
    public void updateContent(String content, String summary, Integer progress,
                              String issues, String nextPlan) {
        if (!STATUS_DRAFT.equals(this.status)) {
            throw new IllegalStateException("Cannot update report: not in DRAFT status");
        }
        this.content = content;
        this.summary = summary;
        this.progress = progress;
        this.issues = issues;
        this.nextPlan = nextPlan;
        this.updatedAt = LocalDateTime.now();
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
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = STATUS_DRAFT;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 加载时兼容旧的枚举值
     * 如果数据库中是FUNC_DEPT，转换为FUNCTIONAL
     */
    @PostLoad
    protected void onLoad() {
        if (reportOrgType != null && "FUNC_DEPT".equals(reportOrgType.name())) {
            reportOrgType = ReportOrgType.FUNCTIONAL;
        }
    }
}
