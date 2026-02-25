package com.sism.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for approval action (approve/reject)
 */
@Data
public class ApprovalActionRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private String comment;

    private String reason;
}
