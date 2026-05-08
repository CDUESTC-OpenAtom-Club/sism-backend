package com.sism.execution.domain.report.event;

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
    private final Long submitterId;

    public PlanReportSubmittedEvent(Long reportId, String reportMonth, Long reportOrgId, Long submitterId) {
        this.reportId = reportId;
        this.reportMonth = reportMonth;
        this.reportOrgId = reportOrgId;
        this.submitterId = submitterId;
    }

    @Override
    public String getEventType() {
        return "PlanReportSubmittedEvent";
    }
}
