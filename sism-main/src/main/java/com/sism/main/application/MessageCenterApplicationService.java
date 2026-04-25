package com.sism.main.application;

import com.sism.iam.application.service.UserNotificationService;
import com.sism.iam.domain.UserNotification;
import com.sism.iam.domain.repository.UserNotificationRepository;
import com.sism.main.interfaces.dto.MessageCenterModels;
import com.sism.workflow.application.query.WorkflowReadModelService;
import com.sism.workflow.interfaces.dto.PageResult;
import com.sism.workflow.interfaces.dto.WorkflowTaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageCenterApplicationService {

    private static final int NOTIFICATION_BATCH_SIZE = 200;
    private static final String CATEGORY_ALL = "ALL";
    private static final String CATEGORY_TODO = "TODO";
    private static final String CATEGORY_APPROVAL = "APPROVAL";
    private static final String CATEGORY_REMINDER = "REMINDER";
    private static final String CATEGORY_SYSTEM = "SYSTEM";
    private static final String CATEGORY_RISK = "RISK";

    private static final String BIZ_APPROVAL_TODO = "APPROVAL_TODO";
    private static final String BIZ_APPROVAL_RESULT = "APPROVAL_RESULT";
    private static final String BIZ_REMINDER_NOTICE = "REMINDER_NOTICE";
    private static final String BIZ_SYSTEM_NOTICE = "SYSTEM_NOTICE";
    private static final String BIZ_BUSINESS_NOTICE = "BUSINESS_NOTICE";

    private static final String READ_UNREAD = "UNREAD";
    private static final String READ_READ = "READ";
    private static final String ACTION_REQUIRED = "ACTION_REQUIRED";

    private final UserNotificationRepository userNotificationRepository;
    private final UserNotificationService userNotificationService;
    private final WorkflowReadModelService workflowReadModelService;

    public MessageCenterModels.Summary getSummary(Long userId) {
        Aggregation aggregation = aggregate(userId);
        return buildSummary(aggregation);
    }

    public MessageCenterModels.ListResponse getMessages(
            Long userId,
            String category,
            int pageNum,
            int pageSize,
            String keyword,
            Boolean includeRisk) {
        Aggregation aggregation = aggregate(userId);
        List<MessageCenterModels.Item> filteredItems = aggregation.items().stream()
                .filter(item -> matchesCategory(item, category))
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> shouldInclude(item, includeRisk))
                .toList();

        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int startIndex = Math.min((safePageNum - 1) * safePageSize, filteredItems.size());
        int endIndex = Math.min(startIndex + safePageSize, filteredItems.size());
        int totalPages = filteredItems.isEmpty() ? 0 : (int) Math.ceil((double) filteredItems.size() / safePageSize);

        return new MessageCenterModels.ListResponse(
                filteredItems.subList(startIndex, endIndex),
                filteredItems.size(),
                safePageNum,
                safePageSize,
                totalPages,
                aggregation.partialSuccess(),
                aggregation.unavailableSources(),
                aggregation.capabilities()
        );
    }

    public MessageCenterModels.Item getMessageDetail(Long userId, String messageId) {
        ParsedMessageId parsedMessageId = parseMessageId(messageId);
        return switch (parsedMessageId.sourceType()) {
            case "notification" -> userNotificationRepository.findByIdAndRecipientUserId(parsedMessageId.notificationId(), userId)
                    .map(this::toNotificationItem)
                    .orElseThrow(() -> new IllegalArgumentException("消息不存在或无权限访问"));
            case "workflow" -> loadAllPendingWorkflowTasks(userId).stream()
                    .filter(task -> Objects.equals(task.getInstanceId(), parsedMessageId.instanceId()))
                    .filter(task -> Objects.equals(task.getTaskId(), parsedMessageId.sourceId()))
                    .findFirst()
                    .map(this::toWorkflowItem)
                    .orElseThrow(() -> new IllegalArgumentException("消息不存在或已完成处理"));
            default -> throw new IllegalArgumentException("暂不支持的消息类型: " + parsedMessageId.sourceType());
        };
    }

    public MessageCenterModels.ReadResult markMessageAsRead(Long userId, String messageId) {
        ParsedMessageId parsedMessageId = parseMessageId(messageId);
        if (!"notification".equals(parsedMessageId.sourceType())) {
            throw new IllegalArgumentException("仅普通通知支持标记已读");
        }

        Map<String, Object> result = userNotificationService.markNotificationAsRead(parsedMessageId.notificationId(), userId);
        LocalDateTime timestamp = toLocalDateTime(result.get("readAt"));
        return new MessageCenterModels.ReadResult(messageId, true, READ_READ, timestamp, 1);
    }

    public MessageCenterModels.ReadResult markAllAsRead(Long userId) {
        Map<String, Object> result = userNotificationService.markAllNotificationsAsRead(userId);
        LocalDateTime timestamp = toLocalDateTime(result.get("timestamp"));
        long affectedCount = toLong(result.get("readCount"));
        return new MessageCenterModels.ReadResult("notification:*", true, READ_READ, timestamp, affectedCount);
    }

    private MessageCenterModels.Summary buildSummary(Aggregation aggregation) {
        long todoCount = 0;
        long approvalResultUnread = 0;
        long reminderUnread = 0;
        long systemUnread = 0;
        long riskUnread = 0;

        for (MessageCenterModels.Item item : aggregation.items()) {
            String bizType = item.bizType();
            boolean unread = READ_UNREAD.equalsIgnoreCase(item.readState());

            if (BIZ_APPROVAL_TODO.equals(bizType)) {
                todoCount++;
            }
            if (BIZ_APPROVAL_RESULT.equals(bizType) && unread) {
                approvalResultUnread++;
            }
            if (BIZ_REMINDER_NOTICE.equals(bizType) && unread) {
                reminderUnread++;
            }
            if ((BIZ_SYSTEM_NOTICE.equals(bizType) || BIZ_BUSINESS_NOTICE.equals(bizType)) && unread) {
                systemUnread++;
            }
            if (CATEGORY_RISK.equals(item.category()) && unread) {
                riskUnread++;
            }
        }

        long approvalCount = todoCount + approvalResultUnread;
        long totalCount = todoCount + approvalResultUnread + reminderUnread + systemUnread + riskUnread;

        return new MessageCenterModels.Summary(
                totalCount,
                todoCount,
                approvalCount,
                reminderUnread,
                systemUnread,
                riskUnread,
                aggregation.capabilities(),
                aggregation.lastRefreshTime(),
                aggregation.partialSuccess(),
                aggregation.unavailableSources()
        );
    }

    private Aggregation aggregate(Long userId) {
        List<MessageCenterModels.Item> items = new ArrayList<>();
        List<String> unavailableSources = new ArrayList<>();
        boolean partialSuccess = false;

        List<WorkflowTaskResponse> workflowTasks = List.of();
        try {
            workflowTasks = loadAllPendingWorkflowTasks(userId);
            items.addAll(workflowTasks.stream().map(this::toWorkflowItem).toList());
        } catch (RuntimeException ex) {
            partialSuccess = true;
            unavailableSources.add("workflow");
        }

        Set<String> workflowIdentityKeys = workflowTasks.stream()
                .map(this::buildWorkflowIdentityKeys)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        try {
            List<UserNotification> notifications = loadAllNotifications(userId);
            items.addAll(notifications.stream()
                    .filter(notification -> !isDuplicateApprovalNotification(notification, workflowIdentityKeys))
                    .map(this::toNotificationItem)
                    .toList());
        } catch (RuntimeException ex) {
            partialSuccess = true;
            unavailableSources.add("notifications");
        }

        items.sort(Comparator
                .comparing(MessageCenterModels.Item::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MessageCenterModels.Item::priority, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MessageCenterModels.Item::messageId));

        return new Aggregation(
                List.copyOf(items),
                new MessageCenterModels.Capabilities(false, true, true),
                partialSuccess,
                List.copyOf(unavailableSources),
                LocalDateTime.now()
        );
    }

    private List<UserNotification> loadAllNotifications(Long userId) {
        List<UserNotification> notifications = new ArrayList<>();
        int pageNum = 0;
        Page<UserNotification> page;
        do {
            page = userNotificationRepository.findByRecipientUserId(userId, PageRequest.of(pageNum, NOTIFICATION_BATCH_SIZE));
            notifications.addAll(page.getContent());
            pageNum++;
        } while (page.hasNext());
        return notifications;
    }

    private List<WorkflowTaskResponse> loadAllPendingWorkflowTasks(Long userId) {
        List<WorkflowTaskResponse> tasks = new ArrayList<>();
        int pageNum = 1;
        PageResult<WorkflowTaskResponse> currentPage;
        do {
            currentPage = workflowReadModelService.getMyPendingTasks(userId, pageNum);
            if (currentPage.getItems() != null) {
                tasks.addAll(currentPage.getItems());
            }
            pageNum++;
        } while (currentPage.getPageNum() < currentPage.getTotalPages());
        return tasks;
    }

    private MessageCenterModels.Item toWorkflowItem(WorkflowTaskResponse task) {
        Long approvalInstanceId = toLong(task.getInstanceId());
        Long entityId = task.getEntityId();
        String entityType = normalize(task.getEntityType());
        String title = switch (entityType) {
            case "PLAN" -> "有新的计划待审批";
            case "PLAN_REPORT" -> "有新的月报待审批";
            case "INDICATOR" -> "有新的指标待审批";
            case "INDICATOR_DISTRIBUTION" -> "有新的指标下发待审批";
            default -> "有新的审批事项待处理";
        };
        String applicant = firstNonBlank(task.getSourceOrgName(), task.getTargetOrgName(), task.getAssigneeName(), "相关部门");
        String stepName = firstNonBlank(task.getCurrentStepName(), task.getTaskName(), "当前审批环节");
        String businessName = firstNonBlank(task.getPlanName(), entityId == null ? null : "业务对象#" + entityId);
        String summary = applicant + "提交了“" + businessName + "”，当前环节为“" + stepName + "”，请及时处理。";
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("flowCode", task.getFlowCode());
        metadata.put("flowName", task.getFlowName());
        metadata.put("sourceOrgName", task.getSourceOrgName());
        metadata.put("targetOrgName", task.getTargetOrgName());
        metadata.put("planName", task.getPlanName());

        return new MessageCenterModels.Item(
                buildWorkflowMessageId(task),
                "workflow",
                String.valueOf(task.getTaskId()),
                CATEGORY_TODO,
                BIZ_APPROVAL_TODO,
                title,
                summary,
                summary,
                "P2",
                null,
                null,
                ACTION_REQUIRED,
                firstNonNull(task.getCreatedTime(), task.getStartedAt(), LocalDateTime.now()),
                firstNonNull(task.getCreatedTime(), task.getStartedAt(), LocalDateTime.now()),
                resolveWorkflowActionUrl(task, approvalInstanceId),
                entityType,
                entityId,
                approvalInstanceId,
                stepName,
                task.getAssigneeName(),
                applicant,
                false,
                true,
                true,
                metadata
        );
    }

    private MessageCenterModels.Item toNotificationItem(UserNotification notification) {
        String bizType = resolveNotificationBizType(notification);
        String category = resolveCategoryByBizType(bizType);
        boolean isRead = READ_READ.equalsIgnoreCase(notification.getStatus());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("notificationType", notification.getNotificationType());
        metadata.put("batchKey", notification.getBatchKey());
        metadata.put("metadataJson", notification.getMetadataJson());

        return new MessageCenterModels.Item(
                "notification:" + notification.getId(),
                "notification",
                String.valueOf(notification.getId()),
                category,
                bizType,
                firstNonBlank(notification.getTitle(), "系统通知"),
                summarize(notification.getContent()),
                firstNonBlank(notification.getContent(), ""),
                BIZ_REMINDER_NOTICE.equals(bizType) ? "P2" : "P3",
                null,
                isRead ? READ_READ : READ_UNREAD,
                null,
                firstNonNull(notification.getCreatedAt(), LocalDateTime.now()),
                firstNonNull(notification.getCreatedAt(), LocalDateTime.now()),
                normalizeInternalUrl(notification.getActionUrl()),
                normalize(notification.getRelatedEntityType()),
                notification.getRelatedEntityId(),
                extractApprovalInstanceId(notification.getActionUrl()),
                null,
                null,
                buildSenderDisplay(notification),
                true,
                true,
                false,
                metadata
        );
    }

    private boolean isDuplicateApprovalNotification(UserNotification notification, Set<String> workflowIdentityKeys) {
        if (!isApprovalLikeNotification(notification) || workflowIdentityKeys.isEmpty()) {
            return false;
        }
        return buildNotificationIdentityKeys(notification).stream().anyMatch(workflowIdentityKeys::contains);
    }

    private Set<String> buildWorkflowIdentityKeys(WorkflowTaskResponse task) {
        Set<String> keys = new LinkedHashSet<>();
        Long instanceId = toLong(task.getInstanceId());
        if (instanceId != null) {
            keys.add("instance:" + instanceId);
        }
        String entityType = normalize(task.getEntityType());
        if (StringUtils.hasText(entityType) && task.getEntityId() != null) {
            keys.add("entity:" + entityType + ":" + task.getEntityId());
        }
        return keys;
    }

    private Set<String> buildNotificationIdentityKeys(UserNotification notification) {
        Set<String> keys = new LinkedHashSet<>();
        Long approvalInstanceId = extractApprovalInstanceId(notification.getActionUrl());
        if (approvalInstanceId != null) {
            keys.add("instance:" + approvalInstanceId);
        }
        String entityType = normalize(notification.getRelatedEntityType());
        if (StringUtils.hasText(entityType) && notification.getRelatedEntityId() != null) {
            keys.add("entity:" + entityType + ":" + notification.getRelatedEntityId());
        }
        return keys;
    }

    private String resolveNotificationBizType(UserNotification notification) {
        String notificationType = normalize(notification.getNotificationType());
        if ("REMINDER".equals(notificationType)) {
            return BIZ_REMINDER_NOTICE;
        }
        if (isApprovalLikeNotification(notification)) {
            return BIZ_APPROVAL_RESULT;
        }
        if (notificationType.contains("BUSINESS")) {
            return BIZ_BUSINESS_NOTICE;
        }
        return BIZ_SYSTEM_NOTICE;
    }

    private boolean isApprovalLikeNotification(UserNotification notification) {
        String notificationType = normalize(notification.getNotificationType());
        String title = normalize(notification.getTitle());
        String content = normalize(notification.getContent());
        return notificationType.contains("APPROVAL")
                || title.contains("审批")
                || title.contains("审核")
                || content.contains("审批")
                || content.contains("审核");
    }

    private String resolveCategoryByBizType(String bizType) {
        return switch (bizType) {
            case BIZ_APPROVAL_TODO -> CATEGORY_TODO;
            case BIZ_APPROVAL_RESULT -> CATEGORY_APPROVAL;
            case BIZ_REMINDER_NOTICE -> CATEGORY_REMINDER;
            default -> CATEGORY_SYSTEM;
        };
    }

    private boolean matchesCategory(MessageCenterModels.Item item, String category) {
        String normalizedCategory = normalize(category);
        if (!StringUtils.hasText(normalizedCategory) || CATEGORY_ALL.equals(normalizedCategory)) {
            return true;
        }
        return switch (normalizedCategory) {
            case CATEGORY_TODO -> BIZ_APPROVAL_TODO.equals(item.bizType());
            case CATEGORY_APPROVAL -> BIZ_APPROVAL_TODO.equals(item.bizType()) || BIZ_APPROVAL_RESULT.equals(item.bizType());
            case CATEGORY_REMINDER -> BIZ_REMINDER_NOTICE.equals(item.bizType());
            case CATEGORY_SYSTEM -> BIZ_SYSTEM_NOTICE.equals(item.bizType()) || BIZ_BUSINESS_NOTICE.equals(item.bizType());
            case CATEGORY_RISK -> CATEGORY_RISK.equals(item.category());
            default -> true;
        };
    }

    private boolean matchesKeyword(MessageCenterModels.Item item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return String.valueOf(item.title()).toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                || String.valueOf(item.summary()).toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                || String.valueOf(item.content()).toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                || String.valueOf(item.currentStepName()).toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private boolean shouldInclude(MessageCenterModels.Item item, Boolean includeRisk) {
        return !CATEGORY_RISK.equals(item.category()) || Boolean.TRUE.equals(includeRisk);
    }

    private String buildWorkflowMessageId(WorkflowTaskResponse task) {
        return "workflow:" + task.getInstanceId() + ":" + task.getTaskId();
    }

    private String resolveWorkflowActionUrl(WorkflowTaskResponse task, Long approvalInstanceId) {
        String claimUrl = normalizeInternalUrl(task.getClaimUrl());
        if (StringUtils.hasText(claimUrl)) {
            return claimUrl;
        }

        String entityType = normalize(task.getEntityType());
        if ("PLAN".equals(entityType) || "PLAN_REPORT".equals(entityType) || "TASK".equals(entityType)) {
            return approvalInstanceId == null
                    ? "/strategic-tasks"
                    : "/strategic-tasks?tab=approval&approvalInstanceId=" + approvalInstanceId;
        }
        if ("INDICATOR".equals(entityType)) {
            return task.getEntityId() == null ? "/indicators" : "/indicators/" + task.getEntityId();
        }
        if ("INDICATOR_DISTRIBUTION".equals(entityType)) {
            return "/distribution";
        }
        return "/workflow-tasks";
    }

    private String normalizeInternalUrl(String actionUrl) {
        if (!StringUtils.hasText(actionUrl)) {
            return null;
        }
        String trimmed = actionUrl.trim();
        if (!trimmed.startsWith("/")) {
            return null;
        }
        return trimmed;
    }

    private Long extractApprovalInstanceId(String actionUrl) {
        if (!StringUtils.hasText(actionUrl)) {
            return null;
        }
        try {
            URI uri = URI.create(actionUrl);
            String value = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .getFirst("approvalInstanceId");
            return toLong(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String summarize(String content) {
        String normalized = firstNonBlank(content, "").replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 117) + "...";
    }

    private String buildSenderDisplay(UserNotification notification) {
        if (notification.getSenderUserId() != null) {
            return "用户#" + notification.getSenderUserId();
        }
        if (notification.getSenderOrgId() != null) {
            return "组织#" + notification.getSenderOrgId();
        }
        return "系统";
    }

    private ParsedMessageId parseMessageId(String messageId) {
        if (!StringUtils.hasText(messageId)) {
            throw new IllegalArgumentException("messageId 不能为空");
        }
        if (messageId.startsWith("notification:")) {
            String sourceId = messageId.substring("notification:".length());
            Long notificationId = toLong(sourceId);
            if (notificationId == null) {
                throw new IllegalArgumentException("无效的通知消息标识: " + messageId);
            }
            return new ParsedMessageId("notification", sourceId, null, notificationId);
        }
        if (messageId.startsWith("workflow:")) {
            String[] parts = messageId.split(":", 3);
            if (parts.length < 3) {
                throw new IllegalArgumentException("无效的审批待办标识: " + messageId);
            }
            return new ParsedMessageId("workflow", parts[2], parts[1], null);
        }
        throw new IllegalArgumentException("不支持的消息标识: " + messageId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private <T> T firstNonNull(T... candidates) {
        if (candidates == null) {
            return null;
        }
        for (T candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String normalized = String.valueOf(value).trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }

    private record Aggregation(
            List<MessageCenterModels.Item> items,
            MessageCenterModels.Capabilities capabilities,
            boolean partialSuccess,
            List<String> unavailableSources,
            LocalDateTime lastRefreshTime
    ) {
    }

    private record ParsedMessageId(
            String sourceType,
            String sourceId,
            String instanceId,
            Long notificationId
    ) {
    }
}
