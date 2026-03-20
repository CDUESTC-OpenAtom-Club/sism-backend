package com.sism.workflow.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作流定义响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionResponse {

    private String definitionId;
    private String definitionCode;
    private String definitionName;
    private String description;
    private String category;
    private String version;
    private boolean isActive;
    private LocalDateTime createTime;
}
