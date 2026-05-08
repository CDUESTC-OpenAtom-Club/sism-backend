package com.sism.alert.interfaces.rest;

import com.sism.alert.application.AlertAccessService;
import com.sism.alert.application.AlertApplicationService;
import com.sism.alert.domain.Alert;
import com.sism.alert.domain.enums.AlertSeverity;
import com.sism.alert.domain.enums.AlertStatus;
import com.sism.alert.interfaces.dto.AlertRequest;
import com.sism.alert.interfaces.dto.AlertResponse;
import com.sism.alert.interfaces.dto.AlertStatsDTO;
import com.sism.alert.interfaces.dto.ResolveAlertRequest;
import com.sism.common.PageResult;
import com.sism.shared.application.dto.CurrentUser;
import com.sism.shared.domain.exception.AuthorizationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertApplicationService alertApplicationService;

    @Mock
    private AlertAccessService alertAccessService;

    @InjectMocks
    private AlertController alertController;

    @Test
    void getAllAlertsShouldFilterByIndicatorOwnership() {
        Authentication authentication = authentication(11L, 35L, "ROLE_USER");

        Alert allowedAlert = alert(1L, 100L, AlertStatus.IN_PROGRESS, "CRITICAL");

        when(alertAccessService.getAccessibleAlerts(authentication)).thenReturn(List.of(allowedAlert));

        var response = alertController.getAllAlerts(authentication);

        assertEquals(1, response.getBody().getData().size());
        assertEquals(100L, response.getBody().getData().get(0).getIndicatorId());
        verify(alertAccessService).getAccessibleAlerts(authentication);
    }

    @Test
    void createAlertShouldRejectForeignIndicator() {
        Authentication authentication = authentication(11L, 35L, "ROLE_USER");
        AlertRequest request = newAlertRequest(200L);

        doThrow(new AuthorizationException("无权访问该指标"))
                .when(alertAccessService)
                .ensureIndicatorAccess(200L, authentication);

        assertThrows(AuthorizationException.class, () -> alertController.createAlert(request, authentication));
        verifyNoInteractions(alertApplicationService);
    }

    @Test
    void triggerAlertShouldAllowOwnedIndicator() {
        Authentication authentication = authentication(11L, 35L, "ROLE_USER");
        Alert alert = alert(1L, 100L, AlertStatus.IN_PROGRESS, "CRITICAL");

        when(alertAccessService.requireAccessibleAlert(1L, authentication)).thenReturn(alert);
        when(alertApplicationService.triggerAlert(1L)).thenReturn(alert);

        alertController.triggerAlert(1L, authentication);

        verify(alertAccessService).requireAccessibleAlert(1L, authentication);
        verify(alertApplicationService).triggerAlert(1L);
    }

    @Test
    void deleteAlertShouldUseApplicationServiceAfterAccessCheck() {
        Authentication authentication = authentication(11L, 35L, "ROLE_USER");
        Alert alert = alert(1L, 100L, AlertStatus.IN_PROGRESS, "CRITICAL");

        when(alertAccessService.requireAccessibleAlert(1L, authentication)).thenReturn(alert);

        alertController.deleteAlert(1L, authentication);

        verify(alertAccessService).requireAccessibleAlert(1L, authentication);
        verify(alertApplicationService).deleteAlert(1L);
    }

    @Test
    void resolveAlertShouldRejectMissingAuthentication() {
        ResolveAlertRequest request = new ResolveAlertRequest();
        request.setResolution("done");

        assertThrows(AuthorizationException.class, () -> alertController.resolveAlert(1L, request, null));
    }

    @Test
    void getAlertStatsShouldKeepLegacySeverityLabelsWhileCountingCanonicalValues() {
        Authentication authentication = authentication(11L, 35L, "ROLE_ADMIN");

        when(alertAccessService.isAdmin(authentication)).thenReturn(true);
        when(alertApplicationService.getAlertStats()).thenReturn(new AlertStatsDTO(
                2L,
                java.util.Map.of(
                        "CRITICAL", 0L,
                        "WARNING", 1L,
                        "INFO", 1L
                )
        ));

        var response = alertController.getAlertStats(authentication);
        var data = response.getBody().getData();

        assertEquals(2L, data.getTotalOpen());
        var countBySeverity = data.getCountBySeverity();
        assertEquals(0L, countBySeverity.get("CRITICAL"));
        assertEquals(1L, countBySeverity.get("WARNING"));
        assertEquals(1L, countBySeverity.get("INFO"));

        verify(alertApplicationService).getAlertStats();
        verifyNoMoreInteractions(alertAccessService);
    }

    @Test
    void getAllAlertsPageShouldReturnPagedResponses() {
        Authentication authentication = authentication(11L, 35L, "ROLE_USER");
        Alert alert = alert(1L, 100L, AlertStatus.OPEN, "WARNING");

        when(alertAccessService.getAccessibleAlerts(authentication, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(alert), PageRequest.of(0, 20), 1));

        var response = alertController.getAllAlertsPage(authentication, 0, 20);
        PageResult<AlertResponse> page = response.getBody().getData();

        assertEquals(1, page.getItems().size());
        assertEquals(100L, page.getItems().get(0).getIndicatorId());
        verify(alertAccessService).getAccessibleAlerts(authentication, PageRequest.of(0, 20));
    }

    @Test
    void getAllAlertsPageShouldNormalizeInvalidPagingArguments() {
        Authentication authentication = authentication(11L, 35L, "ROLE_USER");
        when(alertAccessService.getAccessibleAlerts(authentication, PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        alertController.getAllAlertsPage(authentication, -1, 9999);

        verify(alertAccessService).getAccessibleAlerts(authentication, PageRequest.of(0, 100));
    }

    @Test
    void searchAlertsShouldDelegateToAccessService() {
        Authentication authentication = authentication(11L, 35L, "ROLE_USER");
        Alert alert = alert(1L, 100L, AlertStatus.OPEN, "CRITICAL");
        when(alertAccessService.searchAccessibleAlerts("OPEN", "CRITICAL", authentication, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(alert), PageRequest.of(0, 20), 1));

        var response = alertController.searchAlerts(authentication, "OPEN", "CRITICAL", 0, 20);
        PageResult<AlertResponse> page = response.getBody().getData();

        assertEquals(1, page.getItems().size());
        verify(alertAccessService).searchAccessibleAlerts("OPEN", "CRITICAL", authentication, PageRequest.of(0, 20));
    }

    @Test
    void countAlertsShouldUseAggregateQueriesForAdmin() {
        Authentication authentication = authentication(11L, 35L, "ROLE_ADMIN");

        when(alertAccessService.isAdmin(authentication)).thenReturn(true);
        when(alertApplicationService.countAlerts()).thenReturn(10L);
        when(alertApplicationService.countByStatus(Alert.STATUS_OPEN)).thenReturn(4L);
        when(alertApplicationService.countByStatus(Alert.STATUS_IN_PROGRESS)).thenReturn(3L);
        when(alertApplicationService.countByStatus(Alert.STATUS_RESOLVED)).thenReturn(3L);

        var response = alertController.countAlerts(authentication);
        var data = response.getBody().getData();

        assertEquals(10L, data.get("total"));
        assertEquals(4L, data.get("pending"));
        assertEquals(3L, data.get("triggered"));
        assertEquals(3L, data.get("resolved"));

        verify(alertApplicationService).countAlerts();
        verify(alertApplicationService).countByStatus(Alert.STATUS_OPEN);
        verify(alertApplicationService).countByStatus(Alert.STATUS_IN_PROGRESS);
        verify(alertApplicationService).countByStatus(Alert.STATUS_RESOLVED);
        verifyNoMoreInteractions(alertAccessService);
    }

    private Authentication authentication(Long userId, Long orgId, String authority) {
        CurrentUser currentUser = new CurrentUser(
                userId,
                String.valueOf(userId),
                "User-" + userId,
                "user@example.com",
                orgId,
                List.of(new SimpleGrantedAuthority(authority))
        );
        return UsernamePasswordAuthenticationToken.authenticated(
                currentUser,
                null,
                currentUser.getAuthorities()
        );
    }

    private Alert alert(Long id, Long indicatorId, AlertStatus status, String severity) {
        Alert alert = new Alert();
        alert.setId(id);
        alert.setIndicatorId(indicatorId);
        alert.setStatus(status);
        alert.setSeverity(AlertSeverity.valueOf(severity));
        alert.setActualPercent(BigDecimal.valueOf(90));
        alert.setExpectedPercent(BigDecimal.valueOf(100));
        alert.setGapPercent(BigDecimal.valueOf(10));
        return alert;
    }

    private AlertRequest newAlertRequest(Long indicatorId) {
        AlertRequest request = new AlertRequest();
        request.setIndicatorId(indicatorId);
        request.setSeverity("CRITICAL");
        request.setRuleId(1L);
        request.setWindowId(1L);
        request.setActualPercent(java.math.BigDecimal.TEN);
        request.setExpectedPercent(java.math.BigDecimal.valueOf(100));
        request.setGapPercent(java.math.BigDecimal.valueOf(-90));
        return request;
    }
}
