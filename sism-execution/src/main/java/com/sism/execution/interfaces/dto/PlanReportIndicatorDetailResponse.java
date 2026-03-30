package com.sism.execution.interfaces.dto;

import com.sism.execution.domain.repository.PlanReportIndicatorSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private List<ReportAttachmentResponse> attachments;

    public static PlanReportIndicatorDetailResponse fromSnapshot(PlanReportIndicatorSnapshot snapshot) {
        return PlanReportIndicatorDetailResponse.builder()
                .indicatorId(snapshot.indicatorId())
                .progress(snapshot.progress())
                .comment(snapshot.comment())
                .milestoneNote(snapshot.milestoneNote())
                .attachments(snapshot.attachments().stream()
                        .map(attachment -> ReportAttachmentResponse.builder()
                                .id(attachment.id())
                                .fileName(attachment.fileName())
                                .fileSize(attachment.fileSize())
                                .fileType(attachment.fileType())
                                .url(attachment.url())
                                .uploadedBy(attachment.uploadedBy())
                                .uploadedAt(attachment.uploadedAt())
                                .build())
                        .toList())
                .build();
    }
}
