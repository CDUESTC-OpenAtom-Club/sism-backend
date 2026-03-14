package com.sism.task.infrastructure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.sism.task.domain")
@EnableJpaRepositories(basePackages = "com.sism.task.infrastructure")
public class TaskModuleConfig {
}
