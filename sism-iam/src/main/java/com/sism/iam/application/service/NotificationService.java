package com.sism.iam.application.service;

import com.sism.iam.domain.Notification;
import com.sism.iam.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * NotificationService - 通知服务
 * 负责处理告警事件通知相关业务逻辑
 * 
 * 注意：此Service处理的Notification实体映射到alert_event表
 * 主要用于跟踪指标告警事件及其处理状态
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 根据指标ID查询告警事件（分页）
     */
    public Page<Notification> getNotificationsByIndicatorId(Long indicatorId, Pageable pageable) {
        return notificationRepository.findByIndicatorId(indicatorId, pageable);
    }

    /**
     * 根据规则ID查询告警事件
     */
    public List<Notification> getNotificationsByRuleId(Long ruleId) {
        return notificationRepository.findByRuleId(ruleId);
    }

    /**
     * 根据窗口ID查询告警事件
     */
    public List<Notification> getNotificationsByWindowId(Long windowId) {
        return notificationRepository.findByWindowId(windowId);
    }

    /**
     * 获取指定指标的告警事件总数
     */
    public long countNotificationsByIndicatorId(Long indicatorId) {
        return notificationRepository.countByIndicatorId(indicatorId);
    }

    /**
     * 获取指定规则且特定状态的告警事件数
     */
    public long countNotificationsByRuleIdAndStatus(Long ruleId, String status) {
        return notificationRepository.countByRuleIdAndStatus(ruleId, status);
    }

    /**
     * 创建告警事件通知
     */
    @Transactional
    public Notification createNotification(Long indicatorId, Long ruleId, Long windowId,
                                         String severity, String status) {
        Notification notification = new Notification();
        notification.setIndicatorId(indicatorId);
        notification.setRuleId(ruleId);
        notification.setWindowId(windowId);
        notification.setSeverity(severity);
        notification.setStatus(status);
        
        return notificationRepository.save(notification);
    }

    /**
     * 更新告警事件状态
     */
    @Transactional
    public Notification updateNotificationStatus(Long notificationId, String newStatus) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.setStatus(newStatus);
        return notificationRepository.save(notification);
    }

    /**
     * 标记告警事件为已处理
     */
    @Transactional
    public Notification handleNotification(Long notificationId, Long handledByUserId, String handledNote) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.setHandledBy(handledByUserId);
        notification.setHandledNote(handledNote);
        notification.setStatus("HANDLED");
        return notificationRepository.save(notification);
    }

    /**
     * 删除告警事件
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notificationRepository.delete(notification);
    }

    /**
     * 根据ID查询告警事件
     */
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    /**
     * 查询所有告警事件
     */
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
}
