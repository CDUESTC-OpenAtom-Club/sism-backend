package com.sism.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
 */
@SpringBootApplication(scanBasePackages = {"com.sism.iam", "com.sism.organization",
                                   "com.sism.strategy", "com.sism.task",
                                   "com.sism.workflow", "com.sism.execution",
                                   "com.sism.analytics", "com.sism.alert",
                                   "com.sism.shared"})
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
