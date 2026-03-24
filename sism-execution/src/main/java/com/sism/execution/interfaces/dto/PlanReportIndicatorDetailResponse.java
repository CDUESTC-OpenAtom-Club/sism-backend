package com.sism.execution.interfaces.dto;

import com.sism.execution.domain.repository.PlanReportIndicatorSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 报告中单个指标填报明细响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanReportIndicatorDetailResponse {

    private Long indicatorId;
    private Integer progress;
    private String comment;
    private String milestoneNote;

    public static PlanReportIndicatorDetailResponse fromSnapshot(PlanReportIndicatorSnapshot snapshot) {
        return PlanReportIndicatorDetailResponse.builder()
                .indicatorId(snapshot.indicatorId())
                .progress(snapshot.progress())
                .comment(snapshot.comment())
                .milestoneNote(snapshot.milestoneNote())
                .build();
    }
}
