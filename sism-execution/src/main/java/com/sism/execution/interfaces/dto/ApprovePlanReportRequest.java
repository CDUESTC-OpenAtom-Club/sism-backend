package com.sism.execution.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ApprovePlanReportRequest - 审批通过计划报告请求DTO
 * 用于接收审批通过报告的请求参数
 */
@Data
public class ApprovePlanReportRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
