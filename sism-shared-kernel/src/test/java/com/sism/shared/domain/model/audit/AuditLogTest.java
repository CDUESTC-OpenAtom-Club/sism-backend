package com.sism.shared.domain.model.audit;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditLogTest {

    @Test
    void shouldDelegateAggregateIdAccessToLogId() {
        AuditLog log = new AuditLog();

        log.setId(42L);

        assertEquals(42L, log.getId());
        assertEquals(42L, log.getLogId());
        assertThrows(IllegalStateException.class, () -> log.setId(43L));

        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 10, 15);
        log.setCreatedAt(createdAt);
        assertEquals(createdAt, log.getCreatedAt());
    }
}
