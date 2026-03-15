package com.sism.strategy.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * TerminateIndicatorRequest - 终止指标请求DTO
 */
@Data
@Schema(description = "终止指标请求")
public class TerminateIndicatorRequest {

    @Schema(description = "终止原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "终止原因不能为空")
    private String reason;
}
