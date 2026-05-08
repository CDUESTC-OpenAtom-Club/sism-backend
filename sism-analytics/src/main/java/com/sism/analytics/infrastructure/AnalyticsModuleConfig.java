package com.sism.analytics.infrastructure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Duration;

@EnableCaching
@Configuration
@EntityScan(basePackages = "com.sism.analytics.domain")
@EnableJpaRepositories(basePackages = "com.sism.analytics.infrastructure")
public class AnalyticsModuleConfig {

    @Bean("analyticsCacheManager")
    public CacheManager analyticsCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "dashboard-summary",
                "department-progress",
                "recent-activities"
        );
        cacheManager.setCaffeine(com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(100));
        return cacheManager;
    }
}
