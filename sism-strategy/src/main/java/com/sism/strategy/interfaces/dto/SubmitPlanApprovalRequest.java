package com.sism.strategy.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitPlanApprovalRequest {

    @NotBlank(message = "Workflow code is required")
    private String workflowCode;

    @Valid
    @NotEmpty(message = "Selected approvers are required")
    private List<SelectedApproverRequest> selectedApprovers;

    @Data
    public static class SelectedApproverRequest {
        @NotNull(message = "Step definition ID is required")
        private Long stepDefId;

        @NotNull(message = "Approver ID is required")
        private Long approverId;
    }
}
