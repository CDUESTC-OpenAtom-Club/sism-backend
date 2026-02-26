package com.sism.vo;

import com.sism.enums.AlertSeverity;
import com.sism.enums.AlertStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value Object for AlertEvent response
 * Contains all alert event information for API responses
 */
@Data
public class AlertEventVO {

    private Long eventId;

    // Indicator information
    private Long indicatorId;
    private String indicatorDesc;
    private Long targetOrgId;
    private String targetOrgName;

    // Alert window information
    private Long windowId;
    private String windowName;
    private LocalDate cutoffDate;

    // Alert rule information
    private Long ruleId;
    private String ruleName;

    // Progress information
    private BigDecimal expectedPercent;
    private BigDecimal actualPercent;
    private BigDecimal gapPercent;

    // Alert status
    private AlertSeverity severity;
    private AlertStatus status;

    // Handler information
    private Long handledById;
    private String handledByName;
    private String handledNote;

    // Additional details
    private Map<String, Object> detailJson;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
