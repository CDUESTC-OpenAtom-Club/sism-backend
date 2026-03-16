package com.sism.iam.application.service;

import com.sism.iam.domain.Notification;
import com.sism.iam.domain.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService
 * Tests alert event notification business logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    @DisplayName("Should get notifications by indicator ID with pagination")
    void shouldGetNotificationsByIndicatorId() {
        Long indicatorId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        
        Notification notif = new Notification();
        notif.setId(1L);
        notif.setIndicatorId(indicatorId);
        notif.setRuleId(200L);

        when(notificationRepository.findByIndicatorId(indicatorId, pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(notif), pageable, 1));

        var result = notificationService.getNotificationsByIndicatorId(indicatorId, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(indicatorId, result.getContent().get(0).getIndicatorId());
    }

    @Test
    @DisplayName("Should get notifications by rule ID")
    void shouldGetNotificationsByRuleId() {
        Long ruleId = 200L;
        
        Notification notif = new Notification();
        notif.setId(1L);
        notif.setIndicatorId(100L);
        notif.setRuleId(ruleId);

        when(notificationRepository.findByRuleId(ruleId))
                .thenReturn(Arrays.asList(notif));

        var result = notificationService.getNotificationsByRuleId(ruleId);

        assertEquals(1, result.size());
        assertEquals(ruleId, result.get(0).getRuleId());
    }

    @Test
    @DisplayName("Should get notifications by window ID")
    void shouldGetNotificationsByWindowId() {
        Long windowId = 300L;

        Notification notif = new Notification();
        notif.setId(1L);
        notif.setIndicatorId(100L);
        notif.setWindowId(windowId);

        when(notificationRepository.findByWindowId(windowId))
                .thenReturn(Arrays.asList(notif));

        var result = notificationService.getNotificationsByWindowId(windowId);

        assertEquals(1, result.size());
        assertEquals(windowId, result.get(0).getWindowId());
    }

    @Test
    @DisplayName("Should return empty list when no notifications by indicator ID")
    void shouldReturnEmptyListWhenNoNotificationsByIndicatorId() {
        Long indicatorId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        when(notificationRepository.findByIndicatorId(indicatorId, pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(), pageable, 0));

        var result = notificationService.getNotificationsByIndicatorId(indicatorId, pageable);

        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Should return empty list when no notifications by rule ID")
    void shouldReturnEmptyListWhenNoNotificationsByRuleId() {
        Long ruleId = 999L;

        when(notificationRepository.findByRuleId(ruleId))
                .thenReturn(Arrays.asList());

        var result = notificationService.getNotificationsByRuleId(ruleId);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should return empty list when no notifications by window ID")
    void shouldReturnEmptyListWhenNoNotificationsByWindowId() {
        Long windowId = 999L;

        when(notificationRepository.findByWindowId(windowId))
                .thenReturn(Arrays.asList());

        var result = notificationService.getNotificationsByWindowId(windowId);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should get multiple notifications by indicator ID")
    void shouldGetMultipleNotificationsByIndicatorId() {
        Long indicatorId = 100L;
        Pageable pageable = PageRequest.of(0, 10);

        Notification notif1 = new Notification();
        notif1.setId(1L);
        notif1.setIndicatorId(indicatorId);
        notif1.setRuleId(200L);

        Notification notif2 = new Notification();
        notif2.setId(2L);
        notif2.setIndicatorId(indicatorId);
        notif2.setRuleId(201L);

        when(notificationRepository.findByIndicatorId(indicatorId, pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(notif1, notif2), pageable, 2));

        var result = notificationService.getNotificationsByIndicatorId(indicatorId, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("Should get multiple notifications by rule ID")
    void shouldGetMultipleNotificationsByRuleId() {
        Long ruleId = 200L;

        Notification notif1 = new Notification();
        notif1.setId(1L);
        notif1.setIndicatorId(100L);
        notif1.setRuleId(ruleId);

        Notification notif2 = new Notification();
        notif2.setId(2L);
        notif2.setIndicatorId(101L);
        notif2.setRuleId(ruleId);

        when(notificationRepository.findByRuleId(ruleId))
                .thenReturn(Arrays.asList(notif1, notif2));

        var result = notificationService.getNotificationsByRuleId(ruleId);

        assertEquals(2, result.size());
    }

}
