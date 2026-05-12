package com.sism.strategy.application;

import com.sism.iam.domain.user.User;
import com.sism.iam.domain.user.UserRepository;
import com.sism.shared.domain.notification.NotificationProvider;
import com.sism.strategy.domain.indicator.Indicator;
import com.sism.strategy.domain.indicator.IndicatorStatus;
import com.sism.strategy.domain.milestone.Milestone;
import com.sism.strategy.domain.milestone.MilestoneStatus;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.strategy.domain.repository.MilestoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndicatorOverdueScanner {

    private static final String DISTRIBUTED_STATUS = IndicatorStatus.DISTRIBUTED.name();

    private final IndicatorRepository indicatorRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final NotificationProvider notificationProvider;

    @Scheduled(cron = "${app.indicator-overdue-scan-cron:0 0 8 * * ?}")
    @Transactional
    public void scanOverdueIndicators() {
        LocalDateTime now = LocalDateTime.now();
        List<Indicator> indicators = indicatorRepository.findByStatus(DISTRIBUTED_STATUS);
        if (indicators.isEmpty()) {
            return;
        }

        List<Long> indicatorIds = indicators.stream()
                .map(Indicator::getId)
                .filter(Objects::nonNull)
                .toList();
        if (indicatorIds.isEmpty()) {
            return;
        }

        Map<Long, List<Milestone>> milestonesByIndicatorId = milestoneRepository.findByIndicatorIdIn(indicatorIds).stream()
                .collect(Collectors.groupingBy(Milestone::getIndicatorId));

        for (Indicator indicator : indicators) {
            processIndicator(indicator, milestonesByIndicatorId.getOrDefault(indicator.getId(), List.of()), now);
        }
    }

    private void processIndicator(Indicator indicator, List<Milestone> milestones, LocalDateTime now) {
        if (indicator == null || milestones.isEmpty() || indicator.getTargetOrg() == null || indicator.getTargetOrg().getId() == null) {
            return;
        }

        int actualProgress = indicator.getProgress() == null ? 0 : indicator.getProgress();
        Milestone overdueMilestone = milestones.stream()
                .filter(milestone -> shouldMarkDelayed(milestone, actualProgress, now))
                .max(Comparator.comparing(Milestone::getTargetDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        if (overdueMilestone == null) {
            return;
        }

        String previousStatus = normalizeStatus(overdueMilestone.getStatus());
        overdueMilestone.setStatus(MilestoneStatus.DELAYED.name());
        overdueMilestone.setUpdatedAt(now);
        milestoneRepository.save(overdueMilestone);

        if (MilestoneStatus.DELAYED.name().equals(previousStatus)) {
            return;
        }

        List<User> recipients = userRepository.findByOrgId(indicator.getTargetOrg().getId()).stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .toList();
        for (User recipient : recipients) {
            notificationProvider.createOverdueNotification(
                    recipient.getId(),
                    null,
                    indicator.getOwnerOrg() != null ? indicator.getOwnerOrg().getId() : null,
                    indicator.getId(),
                    indicator.getIndicatorDesc(),
                    overdueMilestone.getId(),
                    overdueMilestone.getMilestoneName(),
                    overdueMilestone.getTargetDate(),
                    actualProgress,
                    overdueMilestone.getProgress()
            );
        }

        log.info("Marked overdue milestone and notified recipients: indicatorId={}, milestoneId={}, recipientCount={}",
                indicator.getId(), overdueMilestone.getId(), recipients.size());
    }

    private boolean shouldMarkDelayed(Milestone milestone, int actualProgress, LocalDateTime now) {
        if (milestone == null || milestone.getTargetDate() == null || milestone.getProgress() == null) {
            return false;
        }
        if (milestone.getTargetDate().isAfter(now)) {
            return false;
        }
        if (actualProgress >= milestone.getProgress()) {
            return false;
        }

        String normalizedStatus = normalizeStatus(milestone.getStatus());
        return !MilestoneStatus.COMPLETED.name().equals(normalizedStatus)
                && !MilestoneStatus.DELAYED.name().equals(normalizedStatus)
                && !"CANCELLED".equals(normalizedStatus)
                && !MilestoneStatus.CANCELED.name().equals(normalizedStatus);
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }
}
