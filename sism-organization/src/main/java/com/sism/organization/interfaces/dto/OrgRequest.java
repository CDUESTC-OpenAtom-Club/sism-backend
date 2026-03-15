package com.sism.organization.interfaces.dto;

import com.sism.organization.domain.OrgType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for organization creation and update requests
 */
@Schema(description = "Organization request DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgRequest {

    @Schema(description = "Organization name", requiredMode = Schema.RequiredMode.REQUIRED, example = "Computer Science Department")
    @NotBlank(message = "Organization name is required")
    private String name;

    @Schema(description = "Organization type", requiredMode = Schema.RequiredMode.REQUIRED, example = "FUNCTIONAL_DEPT")
    @NotNull(message = "Organization type is required")
    private OrgType type;

    @Schema(description = "Parent organization ID", example = "1")
    private Long parentOrgId;

    @Schema(description = "Sort order", example = "0")
    private Integer sortOrder;
}
