package com.sism.organization.infrastructure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.sism.organization.domain")
@EnableJpaRepositories(basePackages = "com.sism.organization.infrastructure")
public class OrganizationModuleConfig {
}
