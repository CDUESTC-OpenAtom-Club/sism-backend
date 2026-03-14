package com.sism.execution.domain.model.report.event;

import com.sism.shared.domain.model.domainevent.DomainEvent;
import lombok.Getter;

/**
 * PlanReportApprovedEvent - 计划报告审批通过事件
 */
@Getter
public class PlanReportApprovedEvent extends DomainEvent {

    private final Long reportId;
    private final String reportMonth;
    private final Long reportOrgId;

    public PlanReportApprovedEvent(Long reportId, String reportMonth, Long reportOrgId) {
        super("PlanReportApprovedEvent");
        this.reportId = reportId;
        this.reportMonth = reportMonth;
        this.reportOrgId = reportOrgId;
    }
}
