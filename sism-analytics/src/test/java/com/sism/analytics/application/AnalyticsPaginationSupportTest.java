package com.sism.analytics.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsPaginationSupportTest {

    @Test
    @DisplayName("normalizePageNum should clamp values below 1")
    void normalizePageNumShouldClampValuesBelowOne() {
        assertEquals(1, AnalyticsPaginationSupport.normalizePageNum(0));
        assertEquals(1, AnalyticsPaginationSupport.normalizePageNum(-5));
        assertEquals(3, AnalyticsPaginationSupport.normalizePageNum(3));
    }

    @Test
    @DisplayName("normalizePageSize should clamp to valid bounds")
    void normalizePageSizeShouldClampToValidBounds() {
        assertEquals(1, AnalyticsPaginationSupport.normalizePageSize(0));
        assertEquals(1, AnalyticsPaginationSupport.normalizePageSize(-8));
        assertEquals(20, AnalyticsPaginationSupport.normalizePageSize(20));
        assertEquals(100, AnalyticsPaginationSupport.normalizePageSize(1000));
    }
}
