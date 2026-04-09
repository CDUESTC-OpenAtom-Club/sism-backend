package com.sism.main;

import com.sism.main.interfaces.rest.AttachmentController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * SISM Main Application
 * SISM 主应用类
 *
 * This is the main entry point for the SISM backend application.
 * It aggregates all bounded contexts through the Maven multi-module structure.
 *
 * Architecture: DDD (Domain-Driven Design)
 * - sism-shared-kernel: Shared kernel for base classes and utilities
 * - sism-iam: Identity & Access Management context
 * - sism-organization: Organization Management context
 * - sism-strategy: Strategy & Planning context
 * - sism-task: Task & Execution context
 * - sism-workflow: Workflow & Approval context
 *
 * Note: JPA repositories and entities are scanned by each module's own ModuleConfig
 * (e.g., IamModuleConfig, StrategyModuleConfig). Do not add @EnableJpaRepositories here.
 */
@SpringBootApplication(scanBasePackages = {"com.sism.iam", "com.sism.organization",
                                   "com.sism.strategy", "com.sism.task",
                                   "com.sism.workflow", "com.sism.execution",
                                   "com.sism.analytics", "com.sism.alert",
                                   "com.sism.shared", "com.sism.config",
                                   "com.sism.exception", "com.sism.common",
                                   "com.sism.util", "com.sism.main"})
@EnableAsync
@Import(AttachmentController.class)
@EntityScan(basePackages = "com.sism.**.domain")
@ComponentScan(basePackages = {"com.sism.iam", "com.sism.organization",
                               "com.sism.strategy", "com.sism.task",
                               "com.sism.workflow", "com.sism.execution",
                               "com.sism.analytics", "com.sism.alert",
                               "com.sism.shared", "com.sism.config",
                               "com.sism.exception", "com.sism.common",
                               "com.sism.util", "com.sism.main"})
public class SismMainApplication {

    /**
     * Main entry point
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SismMainApplication.class, args);
    }
}
