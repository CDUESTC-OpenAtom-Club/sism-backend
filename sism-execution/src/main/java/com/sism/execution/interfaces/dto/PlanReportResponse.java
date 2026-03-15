package com.sism.execution.interfaces.dto;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PlanReportResponse - 计划报告响应DTO
 * 用于返回计划报告的完整信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanReportResponse {

    private Long id;
    private String reportMonth;
    private Long reportOrgId;
    private String reportOrgName;
    private ReportOrgType reportOrgType;
    private Long planId;
    private String title;
    private String content;
    private String summary;
    private Integer progress;
    private String issues;
    private String nextPlan;
    private String status;
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为响应DTO
     */
    public static PlanReportResponse fromEntity(PlanReport report) {
        return PlanReportResponse.builder()
                .id(report.getId())
                .reportMonth(report.getReportMonth())
                .reportOrgId(report.getReportOrgId())
                .reportOrgName(report.getReportOrgName())
                .reportOrgType(report.getReportOrgType())
                .planId(report.getPlanId())
                .title(report.getTitle())
                .content(report.getContent())
                .summary(report.getSummary())
                .progress(report.getProgress())
                .issues(report.getIssues())
                .nextPlan(report.getNextPlan())
                .status(report.getStatus())
                .submittedBy(report.getSubmittedBy())
                .submittedAt(report.getSubmittedAt())
                .approvedBy(report.getApprovedBy())
                .approvedAt(report.getApprovedAt())
                .rejectionReason(report.getRejectionReason())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
