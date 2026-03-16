package com.sism.iam.domain.repository;

import com.sism.iam.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Optional<Notification> findById(Long id);

    List<Notification> findAll();

    Page<Notification> findByIndicatorId(Long indicatorId, Pageable pageable);

    List<Notification> findByRuleId(Long ruleId);

    List<Notification> findByWindowId(Long windowId);

    long countByIndicatorId(Long indicatorId);

    long countByRuleIdAndStatus(Long ruleId, String status);

    Notification save(Notification notification);

    void delete(Notification notification);
}
