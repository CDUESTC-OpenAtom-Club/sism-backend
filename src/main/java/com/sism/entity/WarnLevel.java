package com.sism.entity;

import com.sism.enums.AlertSeverity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Warning Level entity
 * Defines configurable warning/alert thresholds with severity levels
 */
@Getter
@Setter
@Entity
@Table(name = "warn_level", uniqueConstraints = {
    @UniqueConstraint(name = "uk_level_code", columnNames = "level_code")
})
public class WarnLevel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Level name is required")
    @Size(max = 100, message = "Level name must not exceed 100 characters")
    @Column(name = "level_name", nullable = false, length = 100)
    private String levelName;

    @NotBlank(message = "Level code is required")
    @Size(max = 50, message = "Level code must not exceed 50 characters")
    @Column(name = "level_code", nullable = false, unique = true, length = 50)
    private String levelCode;

    @NotNull(message = "Threshold value is required")
    @Min(value = 0, message = "Threshold value must be non-negative")
    @Column(name = "threshold_value", nullable = false)
    private Integer thresholdValue;

    @NotNull(message = "Severity is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    private AlertSeverity severity;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
