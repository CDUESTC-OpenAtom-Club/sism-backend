package com.sism.iam.application.service;

import com.sism.iam.domain.User;
import com.sism.iam.domain.UserNotification;
import com.sism.iam.domain.repository.UserNotificationRepository;
import com.sism.iam.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserNotificationServiceTest {

    private UserNotificationRepository userNotificationRepository;
    private UserRepository userRepository;
    private UserNotificationService service;

    @BeforeEach
    void setUp() {
        userNotificationRepository = mock(UserNotificationRepository.class);
        userRepository = mock(UserRepository.class);
        service = new UserNotificationService(userNotificationRepository, userRepository);
    }

    @Test
    @DisplayName("createReminderNotification should create record and notifications")
    void shouldCreateReminderNotification() {
        when(userNotificationRepository.findLatestReminder(10L, 1L)).thenReturn(Optional.empty());

        User recipient = new User();
        recipient.setId(88L);
        recipient.setOrgId(7L);
        recipient.setIsActive(true);
        when(userRepository.findByOrgId(7L)).thenReturn(List.of(recipient));

        when(userNotificationRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<UserNotification> notifications = invocation.getArgument(0);
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < notifications.size(); i++) {
                notifications.get(i).setId(501L + i);
                notifications.get(i).setCreatedAt(now);
            }
            return notifications;
        });
        when(userNotificationRepository.countReminderBatches(10L, 1L)).thenReturn(1L);

        UserNotificationService.ReminderResult result = service.createReminderNotification(
                10L,
                "推进重点工作",
                7L,
                "国际合作与交流处",
                1L,
                "战略发展部",
                2L,
                "DASHBOARD",
                "请尽快更新"
        );

        assertEquals(501L, result.reminderId());
        assertEquals(1, result.sentCount());
        verify(userNotificationRepository).saveAll(any());
    }

    @Test
    @DisplayName("createReminderNotification should reject reminders within cooldown")
    void shouldRejectReminderInCooldown() {
        UserNotification record = new UserNotification();
        record.setCreatedAt(LocalDateTime.now().minusHours(2));
        when(userNotificationRepository.findLatestReminder(10L, 1L)).thenReturn(Optional.of(record));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createReminderNotification(
                        10L, "推进重点工作", 7L, "国际合作与交流处",
                        1L, "战略发展部", 2L, "DASHBOARD", "请尽快更新"
                )
        );

        assertTrue(exception.getMessage().contains("24 小时内已催办"));
    }

    @Test
    @DisplayName("getReminderStatuses should return cooldown state")
    void shouldReturnReminderStatuses() {
        UserNotification record = new UserNotification();
        record.setRelatedEntityId(10L);
        record.setCreatedAt(LocalDateTime.now().minusHours(1));
        when(userNotificationRepository.findLatestReminders(List.of(10L, 11L), 1L))
                .thenReturn(List.of(record));
        when(userNotificationRepository.countReminderBatches(10L, 1L)).thenReturn(2L);

        Map<Long, UserNotificationService.ReminderStatus> statuses =
                service.getReminderStatuses(List.of(10L, 11L), 1L);

        assertFalse(statuses.get(10L).canRemind());
        assertTrue(statuses.get(11L).canRemind());
        assertEquals(2L, statuses.get(10L).remindCount());
    }

    @Test
    @DisplayName("markNotificationAsRead should return id and isRead")
    void shouldReturnIdAndIsReadWhenMarkingNotificationAsRead() {
        UserNotification notification = new UserNotification();
        notification.setId(21L);
        notification.setRecipientUserId(9L);
        notification.setStatus("UNREAD");

        when(userNotificationRepository.findByIdAndRecipientUserId(21L, 9L))
                .thenReturn(Optional.of(notification));
        when(userNotificationRepository.save(any(UserNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.markNotificationAsRead(21L, 9L);

        assertEquals(21L, result.get("id"));
        assertEquals(21L, result.get("notificationId"));
        assertEquals("READ", result.get("status"));
        assertEquals(true, result.get("isRead"));
        assertNotNull(result.get("readAt"));
    }
}
