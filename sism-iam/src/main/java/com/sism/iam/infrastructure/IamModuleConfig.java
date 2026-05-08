package com.sism.iam.infrastructure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.sism.iam.domain")
@EnableJpaRepositories(basePackages = "com.sism.iam.infrastructure")
public class IamModuleConfig {
}
