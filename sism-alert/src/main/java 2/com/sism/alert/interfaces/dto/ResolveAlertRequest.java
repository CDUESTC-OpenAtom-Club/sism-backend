package com.sism.alert.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ResolveAlertRequest - 解决预警请求DTO
 */
@Data
@Schema(description = "解决预警请求")
public class ResolveAlertRequest {

    @Schema(description = "解决人ID", example = "1")
    private Long resolvedBy;

    @Schema(description = "解决方案", example = "已完成任务处理")
    @NotBlank(message = "解决方案不能为空")
    @Size(max = 500, message = "解决方案长度不能超过500个字符")
    private String resolution;
}
