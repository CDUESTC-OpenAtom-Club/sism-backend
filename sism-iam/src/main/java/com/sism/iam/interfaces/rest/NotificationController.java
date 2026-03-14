package com.sism.iam.interfaces.rest;

import com.sism.iam.application.service.NotificationService;
import com.sism.iam.domain.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications with pagination")
    public ResponseEntity<Page<Notification>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sentAt,desc") String sort) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Get user's unread notifications")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/user/{userId}/statistics")
    @Operation(summary = "Get user notification statistics")
    public ResponseEntity<Map<String, Object>> getNotificationStatistics(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotificationStatistics(userId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search notifications")
    public ResponseEntity<Page<Notification>> searchNotifications(
            @RequestParam Long userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.searchNotifications(userId, keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Long id) {
        Notification notification = notificationService.getNotificationById(id);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(notification);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PostMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @Operation(summary = "Create notification")
    public ResponseEntity<Notification> createNotification(
            @RequestParam Long recipientUserId,
            @RequestParam String notificationType,
            @RequestParam String title,
            @RequestParam String message) {
        return ResponseEntity.ok(notificationService.createNotification(recipientUserId, notificationType, title, message));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }
}
