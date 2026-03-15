package com.sism.execution.domain.model.report.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

/**
 * PlanReportRejectedEvent - 计划报告审批驳回事件
 */
@Getter
public class PlanReportRejectedEvent implements DomainEvent {

    private final Long reportId;
    private final String reportMonth;
    private final Long reportOrgId;
    private final String reason;

    public PlanReportRejectedEvent(Long reportId, String reportMonth, Long reportOrgId, String reason) {
        this.reportId = reportId;
        this.reportMonth = reportMonth;
        this.reportOrgId = reportOrgId;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "PlanReportRejectedEvent";
    }
}
