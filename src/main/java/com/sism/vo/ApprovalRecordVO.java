package com.sism.vo;

import com.sism.enums.ApprovalAction;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Value Object for approval record response
 */
@Data
public class ApprovalRecordVO {

    private Long approvalId;
    private Long reportId;
    private Long approverId;
    private String approverName;
    private Long approverOrgId;
    private String approverOrgName;
    private ApprovalAction action;
    private String comment;
    private LocalDateTime actedAt;
}
