package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.announcement.AnnouncementStatus;
import com.sism.iam.domain.announcement.SystemAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaSystemAnnouncementRepositoryInternal extends JpaRepository<SystemAnnouncement, Long> {

    Page<SystemAnnouncement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<SystemAnnouncement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status, Pageable pageable);

    List<SystemAnnouncement> findByStatusAndScheduledAtBefore(AnnouncementStatus status, LocalDateTime dateTime);
}
