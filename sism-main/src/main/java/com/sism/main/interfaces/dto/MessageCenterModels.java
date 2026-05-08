package com.sism.main.interfaces.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class MessageCenterModels {

    private MessageCenterModels() {
    }

    public record Capabilities(
            boolean riskEnabled,
            boolean approvalAggregationEnabled,
            boolean detailDrawerEnabled
    ) {
    }

    public record Summary(
            long totalCount,
            long todoCount,
            long approvalCount,
            long reminderCount,
            long systemCount,
            long riskCount,
            Capabilities capabilities,
            LocalDateTime lastRefreshTime,
            boolean partialSuccess,
            List<String> unavailableSources
    ) {
    }

    public record Item(
            String messageId,
            String sourceType,
            String sourceId,
            String category,
            String bizType,
            String title,
            String summary,
            String content,
            String priority,
            String severity,
            String readState,
            String actionState,
            LocalDateTime createdAt,
            LocalDateTime eventAt,
            String actionUrl,
            String entityType,
            Long entityId,
            Long approvalInstanceId,
            String currentStepName,
            String currentAssigneeDisplay,
            String senderDisplay,
            boolean canMarkAsRead,
            boolean canViewDetail,
            boolean canProcess,
            Map<String, Object> metadata
    ) {
    }

    public record ListResponse(
            List<Item> items,
            long total,
            int pageNum,
            int pageSize,
            int totalPages,
            boolean partialSuccess,
            List<String> unavailableSources,
            Capabilities capabilities
    ) {
    }

    public record ReadResult(
            String messageId,
            boolean success,
            String status,
            LocalDateTime timestamp,
            long affectedCount
    ) {
    }
}
