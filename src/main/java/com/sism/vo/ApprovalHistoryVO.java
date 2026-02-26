package com.sism.vo;

import com.sism.enums.AuditEntityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VO for approval history
 */
@Data
public class ApprovalHistoryVO {

    private Long instanceId;
    private AuditEntityType entityType;
    private Long entityId;
    private String status;
    private Integer currentStepOrder;
    private List<HistoryItem> logs;

    @Data
    public static class HistoryItem {
        private String action;
        private String actor;
        private String comment;
        private LocalDateTime timestamp;
    }
}
