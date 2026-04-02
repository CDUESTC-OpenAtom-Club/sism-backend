package com.sism.execution.interfaces.dto;

import com.sism.execution.domain.model.milestone.Milestone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MilestoneResponse - 里程碑响应DTO
 * 用于返回里程碑的完整信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneResponse {

    private Long id;
    private Long indicatorId;
    private String milestoneName;
    private String description;
    private LocalDateTime dueDate;
    private Integer targetProgress;
    private String status;
    private Integer sortOrder;
    private Boolean isPaired;
    private Long inheritedFrom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为响应DTO
     */
    public static MilestoneResponse fromEntity(Milestone milestone) {
        return MilestoneResponse.builder()
                .id(milestone.getId())
                .indicatorId(milestone.getIndicatorId())
                .milestoneName(milestone.getMilestoneName())
                .description(milestone.getDescription())
                .dueDate(milestone.getTargetDate())
                .targetProgress(milestone.getProgress())
                .status(milestone.getStatus())
                .sortOrder(milestone.getSortOrder())
                .isPaired(milestone.getIsPaired())
                .inheritedFrom(milestone.getInheritedFrom())
                .createdAt(milestone.getCreatedAt())
                .updatedAt(milestone.getUpdatedAt())
                .build();
    }
}
