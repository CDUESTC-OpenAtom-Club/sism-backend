package com.sism.execution.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RejectPlanReportRequest - 驳回计划报告请求DTO
 * 用于接收驳回报告的请求参数
 */
@Data
public class RejectPlanReportRequest {

    @NotBlank(message = "驳回理由不能为空")
    private String reason;
}
