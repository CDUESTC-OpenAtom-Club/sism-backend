package com.sism.alert.domain;

import com.sism.alert.domain.enums.AlertSeverity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AlertSeverityTest {

    @Test
    void normalizeShouldMapLegacyAliasesToCanonicalValues() {
        assertEquals(AlertSeverity.WARNING, AlertSeverity.normalize("major"));
        assertEquals(AlertSeverity.INFO, AlertSeverity.normalize("minor"));
        assertEquals(AlertSeverity.CRITICAL, AlertSeverity.normalize("critical"));
        assertEquals(AlertSeverity.WARNING, AlertSeverity.normalize("warning"));
    }

    @Test
    void normalizeShouldRejectUnsupportedValues() {
        assertNull(AlertSeverity.normalize("severe"));
        assertNull(AlertSeverity.normalize(""));
        assertNull(AlertSeverity.normalize(null));
    }
}
