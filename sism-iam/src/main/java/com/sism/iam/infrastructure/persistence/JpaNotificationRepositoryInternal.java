package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaNotificationRepositoryInternal extends JpaRepository<Notification, Long> {

    Page<Notification> findByIndicatorId(Long indicatorId, Pageable pageable);

    List<Notification> findByRuleId(Long ruleId);

    List<Notification> findByWindowId(Long windowId);

    long countByIndicatorId(Long indicatorId);

    long countByRuleIdAndStatus(Long ruleId, String status);

    @Query("SELECT n FROM Notification n WHERE n.severity = :severity")
    Page<Notification> findBySeverity(@Param("severity") String severity, Pageable pageable);
}
