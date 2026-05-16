package com.sism.iam.application.service;

import com.sism.iam.application.dto.AnnouncementResponse;
import com.sism.iam.application.dto.CreateAnnouncementRequest;
import com.sism.iam.application.dto.UpdateAnnouncementRequest;
import com.sism.iam.domain.announcement.AnnouncementStatus;
import com.sism.iam.domain.announcement.SystemAnnouncement;
import com.sism.iam.domain.announcement.SystemAnnouncementRepository;
import com.sism.iam.domain.notification.UserNotification;
import com.sism.iam.domain.notification.UserNotificationRepository;
import com.sism.iam.domain.user.User;
import com.sism.iam.domain.user.UserRepository;
import com.sism.iam.application.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemAnnouncementService {

    private static final String TYPE_SYSTEM_ANNOUNCEMENT = "SYSTEM_ANNOUNCEMENT";
    private static final String STATUS_UNREAD = "UNREAD";

    private final SystemAnnouncementRepository announcementRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public AnnouncementResponse create(CreateAnnouncementRequest request, Long currentUserId) {
        SystemAnnouncement announcement = new SystemAnnouncement();
        applyAnnouncementDraftFields(announcement, request.title(), request.content(), request.scheduledAt());
        announcement.setStatus(AnnouncementStatus.DRAFT);
        announcement.setCreatedBy(currentUserId);
        announcement.validate();

        SystemAnnouncement saved = announcementRepository.save(announcement);
        log.info("System announcement created: id={}, title={}, createdBy={}", saved.getId(), saved.getTitle(), currentUserId);
        return toResponse(saved);
    }

    @Transactional
    public AnnouncementResponse publish(Long id) {
        SystemAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("公告不存在: " + id));

        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw new IllegalStateException("公告已发布，不能重复发布");
        }

        LocalDateTime now = LocalDateTime.now();
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(now);
        // 发布时清除定时时间（如果有的话），避免重复触发
        announcement.setScheduledAt(null);

        SystemAnnouncement saved = announcementRepository.save(announcement);
        log.info("System announcement published: id={}, title={}", saved.getId(), saved.getTitle());

        // 发布后为所有活跃用户创建通知
        notifyAllActiveUsers(saved);

        return toResponse(saved);
    }

    @Transactional
    public AnnouncementResponse update(Long id, UpdateAnnouncementRequest request, Long currentUserId) {
        SystemAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("公告不存在: " + id));

        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw new IllegalStateException("已发布公告不允许修改");
        }
        if (!announcement.getCreatedBy().equals(currentUserId)) {
            log.info(
                    "Announcement updated by different operator: announcementId={}, owner={}, operator={}",
                    announcement.getId(),
                    announcement.getCreatedBy(),
                    currentUserId
            );
        }

        applyAnnouncementDraftFields(announcement, request.title(), request.content(), request.scheduledAt());
        SystemAnnouncement saved = announcementRepository.save(announcement);
        log.info("System announcement updated: id={}, title={}, status={}", saved.getId(), saved.getTitle(), saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public AnnouncementResponse withdraw(Long id) {
        SystemAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("公告不存在: " + id));

        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new IllegalStateException("仅已发布的公告可以撤回");
        }

        announcement.setStatus(AnnouncementStatus.WITHDRAWN);
        SystemAnnouncement saved = announcementRepository.save(announcement);
        long removedNotifications = userNotificationRepository
                .deleteByNotificationTypeAndRelatedEntityTypeAndRelatedEntityId(
                        TYPE_SYSTEM_ANNOUNCEMENT,
                        "ANNOUNCEMENT",
                        saved.getId()
                );
        log.info("System announcement withdrawn: id={}, title={}", saved.getId(), saved.getTitle());
        log.info(
                "Announcement notifications removed after withdraw: announcementId={}, count={}",
                saved.getId(),
                removedNotifications
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse getById(Long id) {
        SystemAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("公告不存在: " + id));
        return toResponse(announcement);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> list(String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<SystemAnnouncement> announcements;
        if (status != null && !status.isBlank()) {
            AnnouncementStatus announcementStatus = AnnouncementStatus.valueOf(status.trim().toUpperCase());
            announcements = announcementRepository.findByStatus(announcementStatus, pageRequest);
        } else {
            announcements = announcementRepository.findAll(pageRequest);
        }
        return announcements.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> listPublished(int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return announcementRepository.findByStatus(AnnouncementStatus.PUBLISHED, pageRequest)
                .map(this::toResponse);
    }

    /**
     * 定时任务：每分钟检查一次，自动发布到达定时时间的公告
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoPublishScheduledAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<SystemAnnouncement> pending = announcementRepository
                .findByStatusAndScheduledAtBefore(AnnouncementStatus.DRAFT, now);

        if (pending.isEmpty()) {
            return;
        }

        for (SystemAnnouncement announcement : pending) {
            try {
                announcement.setStatus(AnnouncementStatus.PUBLISHED);
                announcement.setPublishedAt(now);
                announcement.setScheduledAt(null);
                announcementRepository.save(announcement);
                log.info("Auto-published scheduled announcement: id={}, title={}", announcement.getId(), announcement.getTitle());

                notifyAllActiveUsers(announcement);
            } catch (Exception ex) {
                log.error("Failed to auto-publish announcement: id={}, error={}", announcement.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void notifyAllActiveUsers(SystemAnnouncement announcement) {
        List<User> activeUsers = userRepository.findByIsActive(true);
        if (activeUsers.isEmpty()) {
            log.warn("No active users found for announcement notification: announcementId={}", announcement.getId());
            return;
        }

        String title = "系统维护公告";
        String content = announcement.getTitle();
        String actionUrl = "/announcements/" + announcement.getId();

        List<UserNotification> notifications = activeUsers.stream()
                .map(user -> {
                    UserNotification notification = new UserNotification();
                    notification.setRecipientUserId(user.getId());
                    notification.setSenderUserId(announcement.getCreatedBy());
                    notification.setNotificationType(TYPE_SYSTEM_ANNOUNCEMENT);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setStatus(STATUS_UNREAD);
                    notification.setActionUrl(actionUrl);
                    notification.setRelatedEntityType("ANNOUNCEMENT");
                    notification.setRelatedEntityId(announcement.getId());
                    notification.validate();
                    return notification;
                })
                .toList();

        List<UserNotification> saved = userNotificationRepository.saveAll(notifications);
        log.info("Announcement notifications created: announcementId={}, recipientCount={}", announcement.getId(), saved.size());

        // 异步发送邮件通知
        saved.forEach(this::publishNotificationEmailIfPossible);
    }

    private void applyAnnouncementDraftFields(
            SystemAnnouncement announcement,
            String title,
            String content,
            LocalDateTime scheduledAt
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("公告标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("公告内容不能为空");
        }

        announcement.setTitle(title.trim());
        announcement.setContent(content.trim());
        announcement.setScheduledAt(scheduledAt);
    }

    private void publishNotificationEmailIfPossible(UserNotification notification) {
        if (notification == null || notification.getRecipientUserId() == null) {
            return;
        }

        userRepository.findById(notification.getRecipientUserId())
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .ifPresent(email -> applicationEventPublisher.publishEvent(
                        new NotificationEvent(
                                notification.getTitle(),
                                notification.getContent(),
                                email,
                                true
                        )
                ));
    }

    private AnnouncementResponse toResponse(SystemAnnouncement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getStatus().name(),
                announcement.getScheduledAt(),
                announcement.getPublishedAt(),
                announcement.getCreatedBy(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
