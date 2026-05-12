package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.announcement.AnnouncementStatus;
import com.sism.iam.domain.announcement.SystemAnnouncement;
import com.sism.iam.domain.announcement.SystemAnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaSystemAnnouncementRepository implements SystemAnnouncementRepository {

    private final JpaSystemAnnouncementRepositoryInternal jpaRepository;

    @Override
    public Optional<SystemAnnouncement> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<SystemAnnouncement> findAll(Pageable pageable) {
        return jpaRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<SystemAnnouncement> findByStatus(AnnouncementStatus status, Pageable pageable) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Override
    public List<SystemAnnouncement> findByStatusAndScheduledAtBefore(AnnouncementStatus status, LocalDateTime dateTime) {
        return jpaRepository.findByStatusAndScheduledAtBefore(status, dateTime);
    }

    @Override
    public SystemAnnouncement save(SystemAnnouncement announcement) {
        return jpaRepository.save(announcement);
    }

    @Override
    public void delete(SystemAnnouncement announcement) {
        jpaRepository.delete(announcement);
    }
}
