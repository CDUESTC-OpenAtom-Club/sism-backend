package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 启动工作流请求 DTO
 */
@Data
public class StartWorkflowRequest {

    @NotBlank(message = "Workflow code is required")
    private String workflowCode; // "indicator_distribution", "report_approval" 等

    @NotNull(message = "Business entity ID is required")
    private Long businessEntityId; // 业务实体 ID (Indicator, Report 等)

    private String businessEntityType; // 业务实体类型

    private Map<String, Object> variables; // 工作流变量

    @Valid
    private List<SelectedApproverRequest> selectedApprovers;
}
