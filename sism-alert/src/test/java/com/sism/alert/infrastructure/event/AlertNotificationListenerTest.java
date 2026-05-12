package com.sism.alert.infrastructure.event;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.event.AlertCreatedEvent;
import com.sism.alert.domain.event.AlertTriggeredEvent;
import com.sism.alert.domain.enums.AlertSeverity;
import com.sism.alert.domain.repository.AlertRepository;
import com.sism.iam.domain.user.User;
import com.sism.iam.domain.user.UserRepository;
import com.sism.organization.domain.SysOrg;
import com.sism.shared.domain.notification.NotificationProvider;
import com.sism.strategy.domain.indicator.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertNotificationListener Tests")
class AlertNotificationListenerTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private IndicatorRepository indicatorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationProvider notificationProvider;

    private AlertNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new AlertNotificationListener(
                alertRepository,
                indicatorRepository,
                userRepository,
                notificationProvider
        );
    }

    @Test
    @DisplayName("Should notify active target-org users when alert created")
    void shouldNotifyActiveUsersWhenAlertCreated() {
        Alert alert = new Alert();
        alert.setId(11L);
        alert.setIndicatorId(2001L);
        alert.setSeverity(AlertSeverity.WARNING);
        alert.setActualPercent(BigDecimal.valueOf(30));
        alert.setExpectedPercent(BigDecimal.valueOf(60));
        alert.setGapPercent(BigDecimal.valueOf(30));

        Indicator indicator = new Indicator();
        indicator.setId(2001L);
        indicator.setIndicatorDesc("指标A");
        SysOrg ownerOrg = new SysOrg();
        ownerOrg.setId(35L);
        indicator.setOwnerOrg(ownerOrg);
        SysOrg targetOrg = new SysOrg();
        targetOrg.setId(44L);
        indicator.setTargetOrg(targetOrg);

        User activeUser = new User();
        activeUser.setId(501L);
        activeUser.setIsActive(true);
        User inactiveUser = new User();
        inactiveUser.setId(502L);
        inactiveUser.setIsActive(false);

        when(alertRepository.findById(11L)).thenReturn(Optional.of(alert));
        when(indicatorRepository.findById(2001L)).thenReturn(Optional.of(indicator));
        when(userRepository.findByOrgId(44L)).thenReturn(List.of(activeUser, inactiveUser));

        listener.handleAlertCreated(new AlertCreatedEvent(11L, 2001L, "WARNING", "OPEN"));

        verify(notificationProvider).createAlertNotification(
                eq(501L),
                eq(null),
                eq(35L),
                eq(11L),
                eq(2001L),
                eq("指标A"),
                eq("WARNING"),
                eq(BigDecimal.valueOf(30)),
                eq(BigDecimal.valueOf(60)),
                eq(BigDecimal.valueOf(30))
        );
        verify(notificationProvider, never()).createAlertNotification(
                eq(502L), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("Should notify active target-org users when alert triggered")
    void shouldNotifyActiveUsersWhenAlertTriggered() {
        Alert alert = new Alert();
        alert.setId(12L);
        alert.setIndicatorId(2002L);
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setActualPercent(BigDecimal.valueOf(10));
        alert.setExpectedPercent(BigDecimal.valueOf(70));
        alert.setGapPercent(BigDecimal.valueOf(60));

        Indicator indicator = new Indicator();
        indicator.setId(2002L);
        indicator.setIndicatorDesc("指标B");
        SysOrg ownerOrg = new SysOrg();
        ownerOrg.setId(35L);
        indicator.setOwnerOrg(ownerOrg);
        SysOrg targetOrg = new SysOrg();
        targetOrg.setId(45L);
        indicator.setTargetOrg(targetOrg);

        User activeUser = new User();
        activeUser.setId(601L);
        activeUser.setIsActive(true);

        when(alertRepository.findById(12L)).thenReturn(Optional.of(alert));
        when(indicatorRepository.findById(2002L)).thenReturn(Optional.of(indicator));
        when(userRepository.findByOrgId(45L)).thenReturn(List.of(activeUser));

        listener.handleAlertTriggered(new AlertTriggeredEvent(12L, 2002L, "CRITICAL", "IN_PROGRESS"));

        verify(notificationProvider).createAlertNotification(
                eq(601L),
                eq(null),
                eq(35L),
                eq(12L),
                eq(2002L),
                eq("指标B"),
                eq("CRITICAL"),
                eq(BigDecimal.valueOf(10)),
                eq(BigDecimal.valueOf(70)),
                eq(BigDecimal.valueOf(60))
        );
    }
}
