package com.sism.execution.domain.report.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

/**
 * PlanReportApprovedEvent - 计划报告审批通过事件
 */
@Getter
public class PlanReportApprovedEvent implements DomainEvent {

    private final Long reportId;
    private final String reportMonth;
    private final Long reportOrgId;
    private final Long approverId;

    public PlanReportApprovedEvent(Long reportId, String reportMonth, Long reportOrgId, Long approverId) {
        this.reportId = reportId;
        this.reportMonth = reportMonth;
        this.reportOrgId = reportOrgId;
        this.approverId = approverId;
    }

    @Override
    public String getEventType() {
        return "PlanReportApprovedEvent";
    }
}
