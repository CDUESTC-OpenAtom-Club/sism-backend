package com.sism.shared.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeTest {

    @Test
    void containsShouldReturnFalseForNullInputs() {
        DateRange range = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertFalse(range.contains((LocalDate) null));
        assertFalse(range.contains((LocalDateTime) null));
    }

    @Test
    void containsShouldAcceptDatesWithinBounds() {
        DateRange range = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertTrue(range.contains(LocalDate.of(2026, 1, 1)));
        assertTrue(range.contains(LocalDate.of(2026, 1, 31)));
        assertTrue(range.contains(LocalDateTime.of(2026, 1, 15, 12, 0)));
    }
}
