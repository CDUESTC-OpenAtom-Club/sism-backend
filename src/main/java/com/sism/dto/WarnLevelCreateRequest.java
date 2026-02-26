package com.sism.dto;

import com.sism.enums.AlertSeverity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a warning level
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarnLevelCreateRequest {

    @NotBlank(message = "Level name is required")
    @Size(max = 100, message = "Level name must not exceed 100 characters")
    private String levelName;

    @NotBlank(message = "Level code is required")
    @Size(max = 50, message = "Level code must not exceed 50 characters")
    private String levelCode;

    @NotNull(message = "Threshold value is required")
    @Min(value = 0, message = "Threshold value must be non-negative")
    private Integer thresholdValue;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean isActive = true;
}
