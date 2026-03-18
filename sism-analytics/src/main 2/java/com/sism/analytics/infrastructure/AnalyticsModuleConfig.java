package com.sism.analytics.infrastructure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.sism.analytics.domain")
@EnableJpaRepositories(basePackages = "com.sism.analytics.infrastructure")
public class AnalyticsModuleConfig {
}
