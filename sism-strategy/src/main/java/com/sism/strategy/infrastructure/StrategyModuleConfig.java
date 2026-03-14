package com.sism.strategy.infrastructure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.sism.strategy.domain")
@EnableJpaRepositories(basePackages = "com.sism.strategy.infrastructure")
public class StrategyModuleConfig {
}
