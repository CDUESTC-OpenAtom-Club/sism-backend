package com.sism;

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
        // 在启动 Spring 容器之前加载 .env 文件中的环境变量
        // 这样 DataSource 等 Bean 才能正确获取配置
        loadEnvVariables();
        
        SpringApplication.run(SismApplication.class, args);
    }

    private static void loadEnvVariables() {
        try {
            // 尝试在多个可能的目录中查找 .env 文件
            String[] possibleDirs = {
                System.getProperty("user.dir"),
                System.getProperty("user.dir") + File.separator + "sism-backend",
                new File(SismApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getParentFile().getParentFile().getAbsolutePath()
            };

            boolean loaded = false;
            for (String dir : possibleDirs) {
                File envFile = new File(dir, ".env");
                if (envFile.exists()) {
                    System.out.println(">>> Found .env file at: " + envFile.getAbsolutePath());
                    io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                            .directory(dir)
                            .ignoreIfMissing()
                            .load();

                    dotenv.entries().forEach(entry -> {
                        // 始终设置或覆盖系统属性，确保 .env 中的配置优先级高
                        System.setProperty(entry.getKey(), entry.getValue());
                    });
                    
                    System.out.println(">>> Environment variables loaded from " + dir);
                    loaded = true;
                    break;
                }
            }

            if (!loaded) {
                System.out.println(">>> No .env file found in searched locations. Using default configurations.");
            }
        } catch (Exception e) {
            System.err.println("!!! Error during environment variables loading: " + e.getMessage());
        }
    }

    @Override
    public void run(String... args) throws Exception {
        // CommandLineRunner logic if needed
    }
}
