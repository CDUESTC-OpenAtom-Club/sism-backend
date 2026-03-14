package com.sism.execution.domain.model.report.event;

import com.sism.shared.domain.model.domainevent.DomainEvent;
import lombok.Getter;

/**
 * PlanReportRejectedEvent - 计划报告审批驳回事件
 */
@Getter
public class PlanReportRejectedEvent extends DomainEvent {

    private final Long reportId;
    private final String reportMonth;
    private final Long reportOrgId;
    private final String reason;

    public PlanReportRejectedEvent(Long reportId, String reportMonth, Long reportOrgId, String reason) {
        super("PlanReportRejectedEvent");
        this.reportId = reportId;
        this.reportMonth = reportMonth;
        this.reportOrgId = reportOrgId;
        this.reason = reason;
    }
}
