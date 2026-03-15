package com.sism.iam.application.service;

import com.sism.iam.domain.Notification;
import com.sism.iam.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// @Service  # TEMPORARILY DISABLED - Repository not implemented
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Page<Notification> getNotificationsByUserId(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientUserId(userId, pageable);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByRecipientUserIdAndIsReadFalse(userId);
    }

    public Map<String, Object> getNotificationStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", notificationRepository.countByRecipientUserId(userId));
        stats.put("unreadCount", notificationRepository.countByRecipientUserIdAndIsRead(userId, false));
        stats.put("readCount", notificationRepository.countByRecipientUserIdAndIsRead(userId, true));
        stats.put("highPriorityCount", notificationRepository.countByRecipientUserIdAndPriority(userId, "HIGH"));
        stats.put("urgentCount", notificationRepository.countByRecipientUserIdAndPriority(userId, "URGENT"));
        return stats;
    }

    public Page<Notification> searchNotifications(Long userId, String keyword, Pageable pageable) {
        return notificationRepository.searchNotifications(userId, keyword, pageable);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.markAsRead();
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByRecipientUserIdAndIsReadFalse(userId);
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public Notification createNotification(Long recipientUserId, String notificationType, String title, String message) {
        Notification notification = Notification.create(recipientUserId, notificationType, title, message);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.delete();
        notificationRepository.save(notification);
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id).orElse(null);
    }
}
