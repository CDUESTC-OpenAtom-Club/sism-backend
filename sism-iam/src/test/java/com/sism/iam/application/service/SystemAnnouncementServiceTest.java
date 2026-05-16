package com.sism.iam.application.service;

import com.sism.iam.application.dto.AnnouncementResponse;
import com.sism.iam.application.dto.UpdateAnnouncementRequest;
import com.sism.iam.domain.announcement.AnnouncementStatus;
import com.sism.iam.domain.announcement.SystemAnnouncement;
import com.sism.iam.domain.announcement.SystemAnnouncementRepository;
import com.sism.iam.domain.notification.UserNotificationRepository;
import com.sism.iam.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("System Announcement Service Tests")
class SystemAnnouncementServiceTest {

    @Mock
    private SystemAnnouncementRepository announcementRepository;

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private SystemAnnouncementService systemAnnouncementService;

    @Test
    @DisplayName("Should remove published announcement notifications when withdrawn")
    void shouldRemovePublishedAnnouncementNotificationsWhenWithdrawn() {
        SystemAnnouncement announcement = new SystemAnnouncement();
        announcement.setId(12L);
        announcement.setTitle("系统维护公告");
        announcement.setContent("今晚维护");
        announcement.setStatus(AnnouncementStatus.PUBLISHED);

        when(announcementRepository.findById(12L)).thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement)).thenReturn(announcement);
        when(userNotificationRepository.deleteByNotificationTypeAndRelatedEntityTypeAndRelatedEntityId(
                "SYSTEM_ANNOUNCEMENT",
                "ANNOUNCEMENT",
                12L
        )).thenReturn(28L);

        AnnouncementResponse response = systemAnnouncementService.withdraw(12L);

        assertEquals("WITHDRAWN", response.status());
        verify(userNotificationRepository).deleteByNotificationTypeAndRelatedEntityTypeAndRelatedEntityId(
                "SYSTEM_ANNOUNCEMENT",
                "ANNOUNCEMENT",
                12L
        );
    }

    @Test
    @DisplayName("Should reject withdraw when announcement is not published")
    void shouldRejectWithdrawWhenAnnouncementIsNotPublished() {
        SystemAnnouncement announcement = new SystemAnnouncement();
        announcement.setId(13L);
        announcement.setTitle("草稿公告");
        announcement.setContent("内容");
        announcement.setStatus(AnnouncementStatus.DRAFT);

        when(announcementRepository.findById(13L)).thenReturn(Optional.of(announcement));

        assertThrows(IllegalStateException.class, () -> systemAnnouncementService.withdraw(13L));
    }

    @Test
    @DisplayName("Should update withdrawn announcement content")
    void shouldUpdateWithdrawnAnnouncementContent() {
        SystemAnnouncement announcement = new SystemAnnouncement();
        announcement.setId(14L);
        announcement.setTitle("旧标题");
        announcement.setContent("旧内容");
        announcement.setStatus(AnnouncementStatus.WITHDRAWN);
        announcement.setCreatedBy(99L);

        when(announcementRepository.findById(14L)).thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement)).thenReturn(announcement);

        AnnouncementResponse response = systemAnnouncementService.update(
                14L,
                new UpdateAnnouncementRequest("新标题", "新内容", null),
                99L
        );

        assertEquals("新标题", response.title());
        assertEquals("新内容", response.content());
        assertEquals("WITHDRAWN", response.status());
    }

    @Test
    @DisplayName("Should allow republishing withdrawn announcement")
    void shouldAllowRepublishingWithdrawnAnnouncement() {
        SystemAnnouncement announcement = new SystemAnnouncement();
        announcement.setId(15L);
        announcement.setTitle("撤回公告");
        announcement.setContent("内容");
        announcement.setStatus(AnnouncementStatus.WITHDRAWN);
        announcement.setCreatedBy(101L);

        when(announcementRepository.findById(15L)).thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement)).thenReturn(announcement);
        when(userRepository.findByIsActive(true)).thenReturn(java.util.List.of());

        AnnouncementResponse response = systemAnnouncementService.publish(15L);

        assertEquals("PUBLISHED", response.status());
    }
}
