package com.sism;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SISM (Strategic Indicator Management System) Application Entry Point
 * 
 * This is the main Spring Boot application class that bootstraps the entire backend system.
 * 
 * Key Features Enabled:
 * - @SpringBootApplication: Enables auto-configuration, component scanning, and configuration
 * - @EnableScheduling: Enables scheduled task execution for alert processing
 * - JPA Auditing is configured in JpaAuditConfig class
 * 
 * @author SISM Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class SismApplication {

    public static void main(String[] args) {
        SpringApplication.run(SismApplication.class, args);
    }
}
