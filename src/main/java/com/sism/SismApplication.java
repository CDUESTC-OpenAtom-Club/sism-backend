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
     * 
     * 查找顺序：
     * 1. config/.env (生产环境)
     * 2. .env (开发环境)
     */
    private static void loadEnvironmentVariables() {
        try {
            File projectDir = new File(System.getProperty("user.dir"));
            System.out.println("📝 Loading environment variables from .env file...");
            System.out.println("📁 Project directory: " + projectDir.getAbsolutePath());

            // 尝试多个位置查找 .env 文件
            io.github.cdimascio.dotenv.Dotenv dotenv = null;
            
            // 1. 尝试 config/.env (生产环境)
            File configEnv = new File(projectDir, "config/.env");
            if (configEnv.exists()) {
                System.out.println("📄 Found .env at: " + configEnv.getAbsolutePath());
                dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                        .directory(new File(projectDir, "config").getAbsolutePath())
                        .load();
            } else {
                // 2. 尝试 .env (开发环境)
                File rootEnv = new File(projectDir, ".env");
                if (rootEnv.exists()) {
                    System.out.println("📄 Found .env at: " + rootEnv.getAbsolutePath());
                    dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                            .directory(projectDir.getAbsolutePath())
                            .load();
                }
            }

            if (dotenv != null) {
                // 将 .env 中的变量设置为系统属性
                dotenv.entries().forEach(entry -> {
                    System.setProperty(entry.getKey(), entry.getValue());
                });

                System.out.println("✅ Environment variables loaded successfully!");
                System.out.println("📊 Loaded " + dotenv.entries().size() + " variables");
            } else {
                System.out.println("⚠️ No .env file found, using system environment variables");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load .env file: " + e.getMessage());
            System.err.println("⚠️ Will use system environment variables instead");
        }
    }
}
