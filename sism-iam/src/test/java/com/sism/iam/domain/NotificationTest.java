package com.sism.iam.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 Unit tests for Notification domain entity (alert events)
 * Tests notification validation and core alert event functionality
 */
@DisplayName("Notification Entity Tests")
class NotificationTest {

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
    }

    @Test
    @DisplayName("Should create Notification with required fields")
    void shouldCreateNotificationWithRequiredFields() {
        notification.setId(1L);
        notification.setIndicatorId(100L);
        notification.setRuleId(200L);
        notification.setWindowId(300L);
        notification.setSeverity("HIGH");
        notification.setStatus("PENDING");

        assertNotNull(notification);
        assertEquals(1L, notification.getId());
        assertEquals(100L, notification.getIndicatorId());
        assertEquals(200L, notification.getRuleId());
        assertEquals(300L, notification.getWindowId());
        assertEquals("HIGH", notification.getSeverity());
        assertEquals("PENDING", notification.getStatus());
    }

    @Test
    @DisplayName("Should support different severity levels")
    void shouldSupportDifferentSeverities() {
        notification.setIndicatorId(100L);
        notification.setRuleId(200L);
        notification.setWindowId(300L);
        notification.setStatus("PENDING");

        notification.setSeverity("CRITICAL");
        assertEquals("CRITICAL", notification.getSeverity());

        notification.setSeverity("HIGH");
        assertEquals("HIGH", notification.getSeverity());

        notification.setSeverity("MEDIUM");
        assertEquals("MEDIUM", notification.getSeverity());

        notification.setSeverity("LOW");
        assertEquals("LOW", notification.getSeverity());
    }

    @Test
    @DisplayName("Should support different status values")
    void shouldSupportDifferentStatus() {
        notification.setIndicatorId(100L);
        notification.setRuleId(200L);
        notification.setWindowId(300L);
        notification.setSeverity("HIGH");

        notification.setStatus("PENDING");
        assertEquals("PENDING", notification.getStatus());

        notification.setStatus("PROCESSING");
        assertEquals("PROCESSING", notification.getStatus());

        notification.setStatus("HANDLED");
        assertEquals("HANDLED", notification.getStatus());
    }

    @Test
    @DisplayName("Should handle notification handling metadata")
    void shouldHandleNotificationMetadata() {
        notification.setIndicatorId(100L);
        notification.setRuleId(200L);
        notification.setWindowId(300L);
        notification.setSeverity("HIGH");
        notification.setStatus("PENDING");
        notification.setHandledBy(50L);
        notification.setHandledNote("Resolved by action plan");

        assertEquals(50L, notification.getHandledBy());
        assertEquals("Resolved by action plan", notification.getHandledNote());
    }

    @Test
    @DisplayName("Should store detail JSON")
    void shouldStoreDetailJson() {
        notification.setIndicatorId(100L);
        notification.setRuleId(200L);
        notification.setWindowId(300L);
        notification.setSeverity("HIGH");
        notification.setStatus("PENDING");
        
        String detailJson = "{\"threshold\": 80.0, \"actual\": 75.0}";
        notification.setDetailJson(detailJson);

        assertEquals(detailJson, notification.getDetailJson());
    }

}
