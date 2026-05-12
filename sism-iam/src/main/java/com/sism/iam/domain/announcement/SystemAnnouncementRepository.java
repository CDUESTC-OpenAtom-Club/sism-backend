package com.sism.iam.domain.announcement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SystemAnnouncementRepository {

    Optional<SystemAnnouncement> findById(Long id);

    Page<SystemAnnouncement> findAll(Pageable pageable);

    Page<SystemAnnouncement> findByStatus(AnnouncementStatus status, Pageable pageable);

    List<SystemAnnouncement> findByStatusAndScheduledAtBefore(AnnouncementStatus status, LocalDateTime dateTime);

    SystemAnnouncement save(SystemAnnouncement announcement);

    void delete(SystemAnnouncement announcement);
}
