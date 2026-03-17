package com.sism.execution.infrastructure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.sism.execution.domain")
@EnableJpaRepositories(basePackages = "com.sism.execution.infrastructure")
public class ExecutionModuleConfig {
}
