package com.sism.execution.domain.model.report.event;

import com.sism.shared.domain.model.base.DomainEvent;
import lombok.Getter;

/**
 * PlanReportSubmittedEvent - 计划报告提交事件
 */
@Getter
public class PlanReportSubmittedEvent implements DomainEvent {

    private final Long reportId;
    private final String reportMonth;
    private final Long reportOrgId;

    public PlanReportSubmittedEvent(Long reportId, String reportMonth, Long reportOrgId) {
        this.reportId = reportId;
        this.reportMonth = reportMonth;
        this.reportOrgId = reportOrgId;
    }

    @Override
    public String getEventType() {
        return "PlanReportSubmittedEvent";
    }
}
