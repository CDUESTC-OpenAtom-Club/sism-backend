package com.sism.workflow.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectedApproverRequest {

    @NotNull(message = "Step definition ID is required")
    private Long stepDefId;

    @NotNull(message = "Approver ID is required")
    private Long approverId;
}
