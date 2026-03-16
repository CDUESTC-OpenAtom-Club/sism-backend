package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.Notification;
import com.sism.iam.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaNotificationRepository implements NotificationRepository {

    private final JpaNotificationRepositoryInternal jpaRepository;

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Notification> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Page<Notification> findByIndicatorId(Long indicatorId, Pageable pageable) {
        return jpaRepository.findByIndicatorId(indicatorId, pageable);
    }

    @Override
    public List<Notification> findByRuleId(Long ruleId) {
        return jpaRepository.findByRuleId(ruleId);
    }

    @Override
    public List<Notification> findByWindowId(Long windowId) {
        return jpaRepository.findByWindowId(windowId);
    }

    @Override
    public long countByIndicatorId(Long indicatorId) {
        return jpaRepository.countByIndicatorId(indicatorId);
    }

    @Override
    public long countByRuleIdAndStatus(Long ruleId, String status) {
        return jpaRepository.countByRuleIdAndStatus(ruleId, status);
    }

    @Override
    public Notification save(Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public void delete(Notification notification) {
        jpaRepository.delete(notification);
    }
}
