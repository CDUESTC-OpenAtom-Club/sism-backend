package com.sism.main.application;

import com.sism.iam.application.service.UserNotificationService;
import com.sism.iam.domain.UserNotification;
import com.sism.iam.domain.repository.UserNotificationRepository;
import com.sism.main.interfaces.dto.MessageCenterModels;
import com.sism.workflow.application.query.WorkflowReadModelService;
import com.sism.workflow.interfaces.dto.PageResult;
import com.sism.workflow.interfaces.dto.WorkflowTaskResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Message Center Application Service Tests")
class MessageCenterApplicationServiceTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private WorkflowReadModelService workflowReadModelService;

    @InjectMocks
    private MessageCenterApplicationService messageCenterApplicationService;

    @Test
    @DisplayName("Should aggregate summary and deduplicate approval notifications against pending workflow tasks")
    void shouldAggregateSummaryAndDeduplicateApprovalNotificationsAgainstPendingWorkflowTasks() {
        UserNotification reminder = notification(101L, "REMINDER", "催办提醒", "请尽快更新进展", "UNREAD", "/indicators/88", "INDICATOR", 88L);
        UserNotification duplicateApprovalNotification = notification(
                102L,
                "SYSTEM",
                "有新的计划待审批",
                "请及时处理。",
                "UNREAD",
                "/strategic-tasks?tab=approval&approvalInstanceId=12",
                "PLAN",
                101L
        );
        WorkflowTaskResponse pendingTask = WorkflowTaskResponse.builder()
                .taskId("501")
                .instanceId("12")
                .taskName("部门负责人审批")
                .currentStepName("部门负责人审批")
                .entityType("PLAN")
                .entityId(101L)
                .planName("2026年度计划")
                .sourceOrgName("计算机学院")
                .assigneeName("李主任")
                .createdTime(LocalDateTime.of(2026, 4, 17, 9, 0))
                .build();

        when(userNotificationRepository.findByRecipientUserId(eq(8L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(reminder, duplicateApprovalNotification), PageRequest.of(0, 200), 2));
        when(workflowReadModelService.getMyPendingTasks(8L, 1))
                .thenReturn(PageResult.of(List.of(pendingTask), 1, 1, 10));

        MessageCenterModels.Summary summary = messageCenterApplicationService.getSummary(8L);
        MessageCenterModels.ListResponse listResponse = messageCenterApplicationService.getMessages(8L, "ALL", 1, 100, null, false);

        assertEquals(2, summary.totalCount());
        assertEquals(1, summary.todoCount());
        assertEquals(1, summary.reminderCount());
        assertEquals(1, summary.approvalCount());
        assertEquals(2, listResponse.items().size());
        assertTrue(listResponse.items().stream().anyMatch(item -> "APPROVAL_TODO".equals(item.bizType())));
        assertTrue(listResponse.items().stream().anyMatch(item -> "REMINDER_NOTICE".equals(item.bizType())));
    }

    @Test
    @DisplayName("Should return notification detail and support read-all result mapping")
    void shouldReturnNotificationDetailAndSupportReadAllResultMapping() {
        UserNotification notification = notification(
                201L,
                "SYSTEM",
                "系统公告",
                "本周日晚间系统维护，请提前保存数据。",
                "READ",
                "/messages",
                null,
                null
        );
        when(userNotificationRepository.findByIdAndRecipientUserId(201L, 9L)).thenReturn(java.util.Optional.of(notification));
        when(userNotificationService.markAllNotificationsAsRead(9L)).thenReturn(Map.of(
                "readCount", 3L,
                "timestamp", LocalDateTime.of(2026, 4, 17, 10, 0)
        ));

        MessageCenterModels.Item detail = messageCenterApplicationService.getMessageDetail(9L, "notification:201");
        MessageCenterModels.ReadResult readResult = messageCenterApplicationService.markAllAsRead(9L);

        assertEquals("notification:201", detail.messageId());
        assertEquals("SYSTEM_NOTICE", detail.bizType());
        assertEquals("READ", detail.readState());
        assertNotNull(detail.content());
        assertEquals(3, readResult.affectedCount());
        assertEquals("READ", readResult.status());
    }

    private UserNotification notification(
            Long id,
            String type,
            String title,
            String content,
            String status,
            String actionUrl,
            String entityType,
            Long entityId) {
        UserNotification notification = new UserNotification();
        notification.setId(id);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setStatus(status);
        notification.setActionUrl(actionUrl);
        notification.setRelatedEntityType(entityType);
        notification.setRelatedEntityId(entityId);
        notification.setCreatedAt(LocalDateTime.of(2026, 4, 17, 8, 0));
        notification.setUpdatedAt(LocalDateTime.of(2026, 4, 17, 8, 0));
        return notification;
    }
}
