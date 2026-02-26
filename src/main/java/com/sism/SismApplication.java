package com.sism;

import com.sism.config.EnvConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

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
public class SismApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SismApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            File projectDir = new File(System.getProperty("user.dir"));
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                    .directory(projectDir.getAbsolutePath())
                    .ignoreIfMissing()
                    .load();

            System.out.println("Loaded environment variables from .env file");

            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
                System.out.println("Set system property: " + entry.getKey() + " = " + entry.getValue());
            });

            System.out.println("Environment variables loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load environment variables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
