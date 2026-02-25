package com.sism.dto;

import com.sism.enums.AuditEntityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for starting an approval flow
 */
@Data
public class ApprovalStartRequest {

    @NotNull(message = "提交人ID不能为空")
    private Long submitterId;

    @NotNull(message = "实体类型不能为空")
    private AuditEntityType entityType;

    @NotNull(message = "实体ID不能为空")
    private Long entityId;
}
