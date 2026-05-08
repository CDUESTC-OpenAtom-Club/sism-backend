package com.sism.organization.interfaces.dto;

import com.sism.enums.OrgType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for organization response
 * Includes nested children organizations for tree structure
 */
@Schema(description = "Organization response DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgResponse {

    @Schema(description = "Organization ID", example = "1")
    private Long id;

    @Schema(description = "Organization name", example = "Computer Science Department")
    private String name;

    @Schema(description = "Organization type", example = "functional")
    private OrgType type;

    @Schema(description = "Whether the organization is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Sort order", example = "0")
    private Integer sortOrder;

    @Schema(description = "Parent organization ID", example = "1")
    private Long parentOrgId;

    @Schema(description = "Organization level", example = "2")
    private Integer level;

    @Schema(description = "Organization creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Organization last update time")
    private LocalDateTime updatedAt;

    @Schema(description = "Child organizations (transient, not persisted)")
    @Builder.Default
    private List<OrgResponse> children = new ArrayList<>();

    @Schema(description = "Organization code", example = "FUNCTIONAL_DEPT_1")
    private String orgCode;

    @Schema(description = "Organization type name", example = "FUNCTIONAL_DEPT")
    private String orgType;
}
