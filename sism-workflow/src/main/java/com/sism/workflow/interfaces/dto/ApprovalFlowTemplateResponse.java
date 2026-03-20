package com.sism.workflow.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowTemplateResponse {

    private Long id;
    private String flowCode;
    private String flowName;
    private String description;
    private String entityType;
    private boolean isActive;
    private Integer version;
    private List<ApprovalFlowStepResponse> steps;
    private Integer stepCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
