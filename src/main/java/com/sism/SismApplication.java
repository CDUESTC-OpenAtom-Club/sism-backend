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
public class SismApplication {

    public static void main(String[] args) {
        // 在 Spring Boot 启动之前加载 .env 文件
        loadEnvironmentVariables();
        
        SpringApplication.run(SismApplication.class, args);
    }

    /**
     * 加载 .env 文件中的环境变量到系统属性
     * 必须在 Spring Boot 启动之前执行
     */
    private static void loadEnvironmentVariables() {
        try {
            File projectDir = new File(System.getProperty("user.dir"));
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                    .directory(projectDir.getAbsolutePath())
                    .ignoreIfMissing()
                    .load();

            System.out.println("📝 Loading environment variables from .env file...");
            System.out.println("📁 Project directory: " + projectDir.getAbsolutePath());

            // 将 .env 中的变量设置为系统属性
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });

            System.out.println("✅ Environment variables loaded successfully!");
            System.out.println("📊 Loaded " + dotenv.entries().size() + " variables");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load .env file: " + e.getMessage());
            System.err.println("⚠️ Will use system environment variables instead");
        }
    }
}
