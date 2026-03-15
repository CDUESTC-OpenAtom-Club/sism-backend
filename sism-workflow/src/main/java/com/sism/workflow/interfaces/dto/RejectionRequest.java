package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 拒绝请求 DTO
 */
@Data
public class RejectionRequest {

    @NotBlank(message = "Reason is required")
    private String reason; // 拒绝原因

    private String returnToStep; // 返回至哪一步 (可选)

    private Map<String, Object> variables;
}
