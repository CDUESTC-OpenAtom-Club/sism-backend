package com.sism.execution.interfaces.dto;

import com.sism.execution.domain.report.ReportOrgType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;
    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 10;
}
