package com.sism.vo;

import com.sism.enums.AlertSeverity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Value Object for warning level response
 */
@Data
public class WarnLevelVO {

    private Long id;
    private String levelName;
    private String levelCode;
    private Integer thresholdValue;
    private AlertSeverity severity;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
