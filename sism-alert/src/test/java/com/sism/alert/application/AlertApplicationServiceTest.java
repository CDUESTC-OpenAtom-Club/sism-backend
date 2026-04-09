package com.sism.alert.application;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.enums.AlertStatus;
import com.sism.alert.domain.repository.AlertRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertApplicationServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private AlertApplicationService alertApplicationService;

    @Test
    void createAlertShouldNormalizeSeverityAndPersistCanonicalStatus() {
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert alert = alertApplicationService.createAlert(
                1L,
                2L,
                3L,
                "major",
                BigDecimal.valueOf(45.5),
                BigDecimal.valueOf(80.0),
                BigDecimal.valueOf(-34.5),
                "{}"
        );

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());

        assertSame(captor.getValue(), alert);
        assertEquals("WARNING", alert.getSeverity());
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        verify(eventPublisher).publishAll(anyList());
    }

    @Test
    void getAlertsBySeverityShouldUseCanonicalSeverity() {
        Alert alert = new Alert();
        when(alertRepository.findBySeverity("WARNING")).thenReturn(List.of(alert));

        List<Alert> alerts = alertApplicationService.getAlertsBySeverity("major");

        assertEquals(List.of(alert), alerts);
        verify(alertRepository).findBySeverity("WARNING");
    }

    @Test
    void countBySeverityShouldCountCanonicalSeverityOnly() {
        when(alertRepository.countBySeverity("CRITICAL")).thenReturn(7L);

        long count = alertApplicationService.countBySeverity("critical");

        assertEquals(7L, count);
        verify(alertRepository).countBySeverity("CRITICAL");
    }

    @Test
    void triggerAndResolveShouldPublishDomainEvents() {
        Alert alert = new Alert();
        alert.setId(1L);
        alert.setIndicatorId(2L);
        alert.setRuleId(3L);
        alert.setWindowId(4L);
        alert.setSeverity("CRITICAL");
        alert.setActualPercent(BigDecimal.valueOf(90));
        alert.setExpectedPercent(BigDecimal.valueOf(100));
        alert.setGapPercent(BigDecimal.valueOf(10));
        alert.setStatus(AlertStatus.OPEN);

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertApplicationService.triggerAlert(1L);
        alertApplicationService.resolveAlert(1L, 8L, "done");

        verify(eventPublisher, times(2)).publishAll(anyList());
    }

    @Test
    void missingAlertShouldNotLeakIdentifierInExceptionMessage() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException triggerEx = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> alertApplicationService.triggerAlert(99L)
        );
        IllegalArgumentException resolveEx = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> alertApplicationService.resolveAlert(99L, 8L, "done")
        );

        assertEquals("Alert not found", triggerEx.getMessage());
        assertEquals("Alert not found", resolveEx.getMessage());
    }

    @Test
    void getAlertStatsShouldExposeCanonicalSeverityKeys() {
        when(alertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(4L);
        when(alertRepository.countByStatus(AlertStatus.IN_PROGRESS)).thenReturn(2L);
        when(alertRepository.countBySeverityAndStatus("CRITICAL", AlertStatus.IN_PROGRESS)).thenReturn(1L);
        when(alertRepository.countBySeverityAndStatus("CRITICAL", AlertStatus.OPEN)).thenReturn(0L);
        when(alertRepository.countBySeverityAndStatus("WARNING", AlertStatus.IN_PROGRESS)).thenReturn(2L);
        when(alertRepository.countBySeverityAndStatus("WARNING", AlertStatus.OPEN)).thenReturn(1L);
        when(alertRepository.countBySeverityAndStatus("INFO", AlertStatus.IN_PROGRESS)).thenReturn(4L);
        when(alertRepository.countBySeverityAndStatus("INFO", AlertStatus.OPEN)).thenReturn(1L);

        Map<String, Object> stats = alertApplicationService.getAlertStats();

        assertEquals(6L, stats.get("totalOpen"));
        @SuppressWarnings("unchecked")
        Map<String, Long> countBySeverity = (Map<String, Long>) stats.get("countBySeverity");
        assertEquals(1L, countBySeverity.get("CRITICAL"));
        assertEquals(3L, countBySeverity.get("WARNING"));
        assertEquals(5L, countBySeverity.get("INFO"));
    }

    @Test
    void getAlertsByStatusShouldNormalizeLegacyAliases() {
        Alert alert = new Alert();
        when(alertRepository.findByStatus(AlertStatus.IN_PROGRESS)).thenReturn(List.of(alert));

        List<Alert> alerts = alertApplicationService.getAlertsByStatus("triggered");

        assertEquals(List.of(alert), alerts);
        verify(alertRepository).findByStatus(AlertStatus.IN_PROGRESS);
    }
}
