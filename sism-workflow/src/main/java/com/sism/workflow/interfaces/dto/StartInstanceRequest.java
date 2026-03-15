package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 启动工作流实例请求 DTO
 */
@Data
public class StartInstanceRequest {

    @NotNull(message = "Business entity ID is required")
    private Long businessEntityId;

    private Map<String, Object> variables;
}
