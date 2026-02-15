package com.sism.dto;

import com.sism.enums.AlertSeverity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a warning level
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarnLevelUpdateRequest {

    @Size(max = 100, message = "Level name must not exceed 100 characters")
    private String levelName;

    @Min(value = 0, message = "Threshold value must be non-negative")
    private Integer thresholdValue;

    private AlertSeverity severity;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean isActive;
}
