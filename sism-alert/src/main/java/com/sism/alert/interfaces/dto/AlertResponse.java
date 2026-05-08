package com.sism.alert.interfaces.dto;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.enums.AlertStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class AlertResponse {
    Long id;
    Long indicatorId;
    Long ruleId;
    Long windowId;
    BigDecimal actualPercent;
    BigDecimal expectedPercent;
    BigDecimal gapPercent;
    Long handledBy;
    String handledNote;
    String severity;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static AlertResponse fromEntity(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .indicatorId(alert.getIndicatorId())
                .ruleId(alert.getRuleId())
                .windowId(alert.getWindowId())
                .actualPercent(alert.getActualPercent())
                .expectedPercent(alert.getExpectedPercent())
                .gapPercent(alert.getGapPercent())
                .handledBy(alert.getHandledBy())
                .handledNote(alert.getHandledNote())
                .severity(alert.getSeverity() != null ? alert.getSeverity().name() : null)
                .status(alert.getStatus() != null ? alert.getStatus().name() : null)
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
