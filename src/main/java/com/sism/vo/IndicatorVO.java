package com.sism.vo;

import com.sism.enums.IndicatorStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Value Object for indicator response
 * Simplified to match database structure
 *
 * Converted to record for immutability and simplicity
 * **Validates: Requirements 4.1**
 *
 * Note: Nested collections (childIndicators, milestones) are defensive copies
 * to maintain immutability guarantees
 *
 * @param indicatorId         Indicator ID
 * @param taskId              Task ID
 * @param parentIndicatorId   Parent indicator ID
 * @param indicatorDesc       Indicator description
 * @param weightPercent       Weight percentage
 * @param sortOrder           Sort order
 * @param remark              Remark
 * @param type                Type field
 * @param progress            Progress value
 * @param createdAt           Creation timestamp
 * @param updatedAt           Update timestamp
 * @param year                Assessment year (from task -> plan -> cycle)
 * @param ownerDept           Owner department
 * @param responsibleDept     Responsible department
 * @param weight              Alias for weightPercent
 * @param taskName            Task name
 * @param canWithdraw         Whether can be withdrawn
 * @param status              Indicator status
 * @param isQualitative       Whether is qualitative indicator
 * @param type1               Type 1 (定量/定性)
 * @param type2               Type 2 (基础性/发展性)
 * @param isStrategic         Whether is strategic indicator
 * @param childIndicators     Child indicators list
 * @param milestones          Milestones list
 */
public record IndicatorVO(
    Long indicatorId,
    Long taskId,
    Long parentIndicatorId,
    String indicatorDesc,
    BigDecimal weightPercent,
    Integer sortOrder,
    String remark,
    String type,
    Integer progress,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer year,
    String ownerDept,
    String responsibleDept,
    BigDecimal weight,
    String taskName,
    Boolean canWithdraw,
    IndicatorStatus status,
    Boolean isQualitative,
    String type1,
    String type2,
    Boolean isStrategic,
    List<IndicatorVO> childIndicators,
    List<MilestoneVO> milestones
) {
    /**
     * Compact constructor with validation and defensive copying
     */
    public IndicatorVO {
        if (indicatorDesc == null || indicatorDesc.isBlank()) {
            throw new IllegalArgumentException("Indicator description cannot be null or blank");
        }
        // Create defensive copies for mutable collections
        if (childIndicators == null) {
            childIndicators = List.of();
        } else {
            childIndicators = List.copyOf(childIndicators);
        }
        if (milestones == null) {
            milestones = List.of();
        } else {
            milestones = List.copyOf(milestones);
        }
    }

    /**
     * Canonical constructor for backward compatibility
     * Sets all optional fields to null
     */
    public IndicatorVO(
        Long indicatorId,
        Long taskId,
        Long parentIndicatorId,
        String indicatorDesc,
        BigDecimal weightPercent,
        Integer sortOrder,
        String remark,
        String type,
        Integer progress,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this(
            indicatorId, taskId, parentIndicatorId, indicatorDesc,
            weightPercent, sortOrder, remark, type, progress,
            createdAt, updatedAt, null, null, null, null,
            null, null, null, null, null, null, null, null, null
        );
    }
}
