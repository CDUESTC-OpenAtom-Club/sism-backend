package com.sism.strategy.interfaces.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BatchSaveMilestonesRequest - 指标里程碑整包保存请求
 */
@Data
public class BatchSaveMilestonesRequest {

    @Valid
    @NotNull(message = "里程碑列表不能为空")
    private List<Item> milestones = new ArrayList<>();

    @Data
    public static class Item {
        private Long id;

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
}
