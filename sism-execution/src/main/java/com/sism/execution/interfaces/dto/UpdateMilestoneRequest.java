package com.sism.execution.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UpdateMilestoneRequest - 更新里程碑请求DTO
 * 用于接收更新里程碑信息的请求参数
 */
@Data
public class UpdateMilestoneRequest {

    private Long indicatorId;

    @NotBlank(message = "里程碑名称不能为空")
    private String milestoneName;

    private String description;

    private LocalDateTime dueDate;

    @Min(value = 0, message = "目标进度不能小于0")
    @Max(value = 100, message = "目标进度不能大于100")
    private Integer targetProgress;

    @Pattern(
            regexp = "^(?i)(PLANNED|NOT_STARTED|IN_PROGRESS|COMPLETED|DELAYED|CANCELLED)$",
            message = "里程碑状态不合法"
    )
    private String status;

    private Integer sortOrder;

    private Boolean isPaired;

    private Long inheritedFrom;
}
