package com.sism.strategy.application;

import com.sism.iam.domain.user.User;
import com.sism.iam.domain.user.UserRepository;
import com.sism.organization.domain.SysOrg;
import com.sism.shared.domain.notification.NotificationProvider;
import com.sism.strategy.domain.indicator.Indicator;
import com.sism.strategy.domain.indicator.IndicatorStatus;
import com.sism.strategy.domain.milestone.Milestone;
import com.sism.strategy.domain.milestone.MilestoneStatus;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.strategy.domain.repository.MilestoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndicatorOverdueScanner Tests")
class IndicatorOverdueScannerTest {

    @Mock
    private IndicatorRepository indicatorRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationProvider notificationProvider;

    private IndicatorOverdueScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new IndicatorOverdueScanner(
                indicatorRepository,
                milestoneRepository,
                userRepository,
                notificationProvider
        );
    }

    @Test
    @DisplayName("Should mark overdue milestone as delayed and notify active target-org users once")
    void shouldMarkOverdueMilestoneAndNotifyUsers() {
        Indicator indicator = createIndicator(2001L, 35L, 44L, 20);
        Milestone overdueMilestone = createMilestone(
                301L,
                2001L,
                "阶段一",
                LocalDateTime.now().minusDays(1),
                50,
                MilestoneStatus.IN_PROGRESS.name()
        );
        User activeUser = createUser(501L, 44L, true, "a@example.com");
        User inactiveUser = createUser(502L, 44L, false, "b@example.com");

        when(indicatorRepository.findByStatus(IndicatorStatus.DISTRIBUTED.name())).thenReturn(List.of(indicator));
        when(milestoneRepository.findByIndicatorIdIn(List.of(2001L))).thenReturn(List.of(overdueMilestone));
        when(userRepository.findByOrgId(44L)).thenReturn(List.of(activeUser, inactiveUser));

        scanner.scanOverdueIndicators();

        verify(milestoneRepository).save(overdueMilestone);
        verify(notificationProvider).createOverdueNotification(
                eq(501L),
                eq(null),
                eq(35L),
                eq(2001L),
                eq("指标A"),
                eq(301L),
                eq("阶段一"),
                any(LocalDateTime.class),
                eq(20),
                eq(50)
        );
        verify(notificationProvider, never()).createOverdueNotification(
                eq(502L),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Should skip notification when milestone already delayed")
    void shouldSkipNotificationWhenAlreadyDelayed() {
        Indicator indicator = createIndicator(2002L, 35L, 44L, 20);
        Milestone delayedMilestone = createMilestone(
                302L,
                2002L,
                "阶段二",
                LocalDateTime.now().minusDays(1),
                50,
                MilestoneStatus.DELAYED.name()
        );

        when(indicatorRepository.findByStatus(IndicatorStatus.DISTRIBUTED.name())).thenReturn(List.of(indicator));
        when(milestoneRepository.findByIndicatorIdIn(List.of(2002L))).thenReturn(List.of(delayedMilestone));

        scanner.scanOverdueIndicators();

        verify(milestoneRepository, never()).save(delayedMilestone);
        verify(notificationProvider, never()).createOverdueNotification(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    private Indicator createIndicator(Long id, Long ownerOrgId, Long targetOrgId, int progress) {
        Indicator indicator = new Indicator();
        indicator.setId(id);
        indicator.setIndicatorDesc("指标A");
        indicator.setProgress(progress);
        indicator.setStatus(IndicatorStatus.DISTRIBUTED);

        SysOrg ownerOrg = new SysOrg();
        ownerOrg.setId(ownerOrgId);
        indicator.setOwnerOrg(ownerOrg);

        SysOrg targetOrg = new SysOrg();
        targetOrg.setId(targetOrgId);
        indicator.setTargetOrg(targetOrg);
        return indicator;
    }

    private Milestone createMilestone(
            Long id,
            Long indicatorId,
            String name,
            LocalDateTime dueDate,
            Integer targetProgress,
            String status
    ) {
        Milestone milestone = new Milestone();
        milestone.setId(id);
        milestone.setIndicatorId(indicatorId);
        milestone.setMilestoneName(name);
        milestone.setTargetDate(dueDate);
        milestone.setProgress(targetProgress);
        milestone.setStatus(status);
        return milestone;
    }

    private User createUser(Long id, Long orgId, boolean active, String email) {
        User user = new User();
        user.setId(id);
        user.setOrgId(orgId);
        user.setIsActive(active);
        user.setEmail(email);
        return user;
    }
}
