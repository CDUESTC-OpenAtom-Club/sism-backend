package com.sism.analytics.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("AnalyticsModuleConfig Tests")
class AnalyticsModuleConfigTest {

    @Test
    @DisplayName("analyticsCacheManager should expose analytics caches without using the generic bean name")
    void analyticsCacheManagerShouldExposeAnalyticsCaches() {
        AnalyticsModuleConfig config = new AnalyticsModuleConfig();

        CacheManager cacheManager = config.analyticsCacheManager();

        assertNotNull(cacheManager.getCache("dashboard-summary"));
        assertNotNull(cacheManager.getCache("department-progress"));
        assertNotNull(cacheManager.getCache("recent-activities"));
        assertEquals(null, cacheManager.getCache("missing-cache"));
    }
}
