package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 审批请求 DTO
 */
@Data
public class ApprovalRequest {

    @NotBlank(message = "Comment is required")
    private String comment; // 批注

    private Map<String, Object> variables;
}
