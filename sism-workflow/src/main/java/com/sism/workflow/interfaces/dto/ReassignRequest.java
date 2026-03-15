package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转办请求 DTO
 */
@Data
public class ReassignRequest {

    @NotNull(message = "Target user ID is required")
    private Long targetUserId; // 目标用户 ID

    private String reason; // 转办原因
}
