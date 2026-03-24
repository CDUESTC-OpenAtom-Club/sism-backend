package com.sism.strategy.interfaces.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CreateMilestoneRequest - 创建里程碑请求DTO
 * 用于接收创建里程碑的请求参数
 */
@Data
public class CreateMilestoneRequest {

    @NotNull(message = "指标ID不能为空")
    private Long indicatorId;

    @NotBlank(message = "里程碑名称不能为空")
    private String milestoneName;

    private String description;

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime dueDate;

    @Min(value = 0, message = "目标进度不能小于0")
    @Max(value = 100, message = "目标进度不能大于100")
    private Integer targetProgress;

    private String status;

    private Integer sortOrder;

    private Boolean isPaired;

    private Long inheritedFrom;
}
