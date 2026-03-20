package com.sism.workflow.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverCandidateResponse {
    private Long userId;
    private String username;
    private String realName;
    private Long orgId;
}
