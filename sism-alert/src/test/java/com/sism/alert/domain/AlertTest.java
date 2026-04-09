package com.sism.alert.domain;

import com.sism.alert.domain.enums.AlertStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlertTest {

    @Test
    void validateShouldAcceptFullyPopulatedAlert() {
        Alert alert = createValidAlert();

        assertDoesNotThrow(alert::validate);
    }

    @Test
    void validateShouldRejectMissingNotNullFields() {
        Alert alert = createValidAlert();
        alert.setRuleId(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, alert::validate);
        assertEquals("Rule ID is required", exception.getMessage());
    }

    @Test
    void triggerShouldRejectClosedAlerts() {
        Alert alert = createValidAlert();
        alert.setStatus(AlertStatus.CLOSED);

        assertThrows(IllegalStateException.class, alert::trigger);
    }

    @Test
    void resolveShouldRejectClosedAlerts() {
        Alert alert = createValidAlert();
        alert.setStatus(AlertStatus.CLOSED);

        assertThrows(IllegalStateException.class, () -> alert.resolve(1L, "handled"));
    }

    @Test
    void normalizeStatusShouldMapLegacyAliasesToEnumValues() {
        assertEquals(AlertStatus.OPEN, Alert.normalizeStatus("pending"));
        assertEquals(AlertStatus.IN_PROGRESS, Alert.normalizeStatus("triggered"));
        assertEquals(AlertStatus.RESOLVED, Alert.normalizeStatus("resolved"));
        assertEquals(AlertStatus.CLOSED, Alert.normalizeStatus("closed"));
    }

    private Alert createValidAlert() {
        Alert alert = new Alert();
        alert.setIndicatorId(1L);
        alert.setRuleId(2L);
        alert.setWindowId(3L);
        alert.setActualPercent(BigDecimal.valueOf(45.5));
        alert.setExpectedPercent(BigDecimal.valueOf(80.0));
        alert.setGapPercent(BigDecimal.valueOf(-34.5));
        alert.setSeverity("WARNING");
        alert.setStatus(AlertStatus.OPEN);
        return alert;
    }
}
