package com.sism.organization.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Organization rename request DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenameOrgRequest {

    @Schema(description = "New organization name", requiredMode = Schema.RequiredMode.REQUIRED, example = "Computer Science Department")
    @NotBlank(message = "Organization name is required")
    @Size(max = 100, message = "Organization name cannot exceed 100 characters")
    private String name;
}
