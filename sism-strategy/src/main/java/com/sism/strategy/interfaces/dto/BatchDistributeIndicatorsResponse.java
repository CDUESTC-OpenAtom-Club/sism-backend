package com.sism.strategy.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDistributeIndicatorsResponse {
    private int totalCount;
    private List<ItemResult> items = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {
        private String clientRequestId;
        private Long indicatorId;
    }
}
