package com.sism.workflow.infrastructure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * WorkflowModuleConfig - 工作流模块配置
 * 配置JPA实体扫描和仓储扫描
 */
@Configuration
@EntityScan(basePackages = {"com.sism.workflow.domain", "com.sism.shared.domain.model.workflow"})
@EnableJpaRepositories(basePackages = "com.sism.workflow.infrastructure")
public class WorkflowModuleConfig {
}
