package com.sism.execution.interfaces.dto;

import com.sism.execution.domain.model.report.ReportOrgType;
import lombok.Data;

/**
 * PlanReportQueryRequest - 计划报告查询请求DTO
 * 用于接收查询计划报告的请求参数（支持分页和多条件查询）
 */
@Data
public class PlanReportQueryRequest {

    private String reportMonth;
    private Long reportOrgId;
    private ReportOrgType reportOrgType;
    private Long planId;
    private String status;
    private String title;
    private Integer minProgress;
    private Integer maxProgress;
    private Integer page = 1;
    private Integer size = 10;
}
