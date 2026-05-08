package com.sism.execution.interfaces.dto;

import com.sism.execution.domain.report.ReportOrgType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CreatePlanReportRequest - 创建计划报告请求DTO
 * 用于接收创建月度报告（草稿）的请求参数
 */
@Data
public class CreatePlanReportRequest {

    @NotBlank(message = "报告月份不能为空")
    private String reportMonth;

    @NotNull(message = "报告组织ID不能为空")
    private Long reportOrgId;

    @NotNull(message = "报告组织类型不能为空")
    private ReportOrgType reportOrgType;

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    private Long createdBy;
}
