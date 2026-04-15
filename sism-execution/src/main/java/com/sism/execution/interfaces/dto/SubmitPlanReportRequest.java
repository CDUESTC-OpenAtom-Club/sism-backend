package com.sism.execution.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * SubmitPlanReportRequest - 提交计划报告请求DTO
 * 用于接收提交报告的请求参数
 */
@Data
public class SubmitPlanReportRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
