package com.sism.vo;

import com.sism.enums.ReportStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Value Object for progress report response
 */
@Data
public class ReportVO {

    private Long reportId;
    private Long indicatorId;
    private String indicatorDesc;
    private Long milestoneId;
    private String milestoneName;
    private Long adhocTaskId;
    private String adhocTaskTitle;
    private BigDecimal percentComplete;
    private Boolean achievedMilestone;
    private String narrative;
    private Long reporterId;
    private String reporterName;
    private Long reporterOrgId;
    private String reporterOrgName;
    private ReportStatus status;
    private Boolean isFinal;
    private Integer versionNo;
    private LocalDateTime reportedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
