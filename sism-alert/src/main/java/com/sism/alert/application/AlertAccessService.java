package com.sism.alert.application;

import com.sism.alert.domain.Alert;
import com.sism.alert.domain.enums.AlertSeverity;
import com.sism.alert.domain.enums.AlertStatus;
import com.sism.alert.domain.repository.AlertRepository;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.shared.domain.exception.AuthorizationException;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * AlertAccessService - 预警访问控制与可见性判定
 * 将控制器中的仓储访问与权限过滤收口到应用层。
 */
@Service
@RequiredArgsConstructor
public class AlertAccessService {

    private final AlertRepository alertRepository;
    private final IndicatorRepository indicatorRepository;

    public Alert requireAccessibleAlert(Long alertId, Authentication authentication) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        validateAlertAccess(alert, authentication);
        return alert;
    }

    public void ensureIndicatorAccess(Long indicatorId, Authentication authentication) {
        if (indicatorId == null || indicatorId <= 0) {
            throw new AuthorizationException("无权访问该指标");
        }
        if (!hasIndicatorAccess(indicatorId, authentication)) {
            throw new AuthorizationException("无权访问该指标");
        }
    }

    public void validateAlertAccess(Alert alert, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }
        if (alert == null || alert.getIndicatorId() == null) {
            throw new AuthorizationException("无权访问该预警");
        }
        if (!hasIndicatorAccess(alert.getIndicatorId(), authentication)) {
            throw new AuthorizationException("无权访问该预警");
        }
    }

    public List<Alert> filterAlertsByPermission(List<Alert> alerts, Authentication authentication) {
        if (isAdmin(authentication)) {
            return alerts;
        }
        Set<Long> accessibleIndicatorIds = resolveAccessibleIndicatorIds(authentication);
        if (accessibleIndicatorIds.isEmpty()) {
            return List.of();
        }
        return alerts.stream()
                .filter(alert -> alert != null
                        && alert.getIndicatorId() != null
                        && accessibleIndicatorIds.contains(alert.getIndicatorId()))
                .toList();
    }

    public Map<String, Long> countAlertsForCurrentOrg(Authentication authentication) {
        Set<Long> accessibleIndicatorIds = resolveAccessibleIndicatorIds(authentication);
        if (accessibleIndicatorIds.isEmpty()) {
            return zeroCounts();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("total", alertRepository.countByIndicatorIdIn(accessibleIndicatorIds));
        counts.put("pending", alertRepository.countByIndicatorIdInAndStatus(accessibleIndicatorIds, AlertStatus.OPEN));
        counts.put("triggered", alertRepository.countByIndicatorIdInAndStatus(accessibleIndicatorIds, AlertStatus.IN_PROGRESS));
        counts.put("resolved", alertRepository.countByIndicatorIdInAndStatus(accessibleIndicatorIds, AlertStatus.RESOLVED));
        return counts;
    }

    public Map<String, Object> buildAlertStatsForCurrentOrg(Authentication authentication) {
        Set<Long> accessibleIndicatorIds = resolveAccessibleIndicatorIds(authentication);
        if (accessibleIndicatorIds.isEmpty()) {
            return emptyStats();
        }

        long totalOpen = alertRepository.countByIndicatorIdInAndStatus(accessibleIndicatorIds, AlertStatus.IN_PROGRESS)
                + alertRepository.countByIndicatorIdInAndStatus(accessibleIndicatorIds, AlertStatus.OPEN);

        Map<String, Long> countBySeverity = new LinkedHashMap<>();
        countBySeverity.put("CRITICAL", countBySeverityAndOpenStatus(accessibleIndicatorIds, "CRITICAL"));
        countBySeverity.put("WARNING", countBySeverityAndOpenStatus(accessibleIndicatorIds, "WARNING"));
        countBySeverity.put("INFO", countBySeverityAndOpenStatus(accessibleIndicatorIds, "INFO"));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOpen", totalOpen);
        stats.put("countBySeverity", countBySeverity);
        return stats;
    }

    private boolean hasIndicatorAccess(Long indicatorId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        Set<Long> accessibleIndicatorIds = resolveAccessibleIndicatorIds(authentication);
        return accessibleIndicatorIds.contains(indicatorId);
    }

    private long countBySeverityAndOpenStatus(Set<Long> indicatorIds, String severity) {
        String normalizedSeverity = AlertSeverity.normalize(severity);
        if (normalizedSeverity == null) {
            return 0L;
        }
        return alertRepository.countByIndicatorIdInAndSeverityAndStatus(indicatorIds, normalizedSeverity, AlertStatus.IN_PROGRESS)
                + alertRepository.countByIndicatorIdInAndSeverityAndStatus(indicatorIds, normalizedSeverity, AlertStatus.OPEN);
    }

    private Set<Long> resolveAccessibleIndicatorIds(Authentication authentication) {
        CurrentUser currentUser = requireCurrentUser(authentication);
        if (currentUser.getOrgId() == null) {
            return Set.of();
        }

        Set<Long> indicatorIds = new LinkedHashSet<>();
        indicatorRepository.findByOwnerOrgId(currentUser.getOrgId()).stream()
                .map(Indicator::getId)
                .filter(Objects::nonNull)
                .forEach(indicatorIds::add);
        indicatorRepository.findByTargetOrgId(currentUser.getOrgId()).stream()
                .map(Indicator::getId)
                .filter(Objects::nonNull)
                .forEach(indicatorIds::add);
        return indicatorIds;
    }

    private Map<String, Long> zeroCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("total", 0L);
        counts.put("pending", 0L);
        counts.put("triggered", 0L);
        counts.put("resolved", 0L);
        return counts;
    }

    private Map<String, Object> emptyStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOpen", 0L);
        stats.put("countBySeverity", Map.of(
                "CRITICAL", 0L,
                "WARNING", 0L,
                "INFO", 0L
        ));
        return stats;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private CurrentUser requireCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new AuthorizationException("无法获取当前用户信息，请联系管理员");
        }
        return currentUser;
    }
}
