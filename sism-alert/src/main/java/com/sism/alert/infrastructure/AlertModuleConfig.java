package com.sism.alert.infrastructure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.sism.alert.domain")
@EnableJpaRepositories(basePackages = "com.sism.alert.infrastructure")
public class AlertModuleConfig {
}
