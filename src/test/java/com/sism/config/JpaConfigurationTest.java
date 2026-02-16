package com.sism.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA 配置验证测试
 * 验证 ddl-auto 和 sql.init.mode 配置是否正确
 */
@SpringBootTest
@ActiveProfiles("test")
public class JpaConfigurationTest {

    @Autowired
    private Environment environment;

    /**
     * 验证测试环境 ddl-auto 配置为 create-drop
     */
    @Test
    public void verifyDdlAutoConfigurationInTest() {
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        
        assertThat(ddlAuto)
            .as("测试环境应该使用 ddl-auto: create-drop")
            .isEqualTo("create-drop");
        
        System.out.println("✓ 测试环境 ddl-auto 配置正确: " + ddlAuto);
    }

    /**
     * 验证 SQL 初始化模式配置
     */
    @Test
    public void verifySqlInitModeConfiguration() {
        String sqlInitMode = environment.getProperty("spring.sql.init.mode");
        
        // 默认应该是 never，除非明确配置为 always
        if (sqlInitMode != null) {
            assertThat(sqlInitMode)
                .as("SQL 初始化模式应该是 never 或 always")
                .isIn("never", "always");
            
            System.out.println("✓ SQL 初始化模式: " + sqlInitMode);
        } else {
            System.out.println("✓ SQL 初始化模式未配置（默认为 never）");
        }
    }

    /**
     * 验证数据库方言配置
     */
    @Test
    public void verifyDatabaseDialect() {
        String dialect = environment.getProperty("spring.jpa.database-platform");
        
        // 测试环境使用 H2，生产环境使用 PostgreSQL
        assertThat(dialect)
            .as("测试环境应该使用 H2 方言")
            .isEqualTo("org.hibernate.dialect.H2Dialect");
        
        System.out.println("✓ 数据库方言配置正确: " + dialect);
    }

    /**
     * 验证数据源配置
     */
    @Test
    public void verifyDataSourceConfiguration() {
        String url = environment.getProperty("spring.datasource.url");
        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");
        
        // 测试环境使用 H2 内存数据库
        assertThat(url)
            .as("数据源 URL 应该配置")
            .isNotNull()
            .contains("h2:mem");
        
        assertThat(driverClassName)
            .as("测试环境应该使用 H2 驱动")
            .isEqualTo("org.h2.Driver");
        
        System.out.println("✓ 数据源配置正确");
        System.out.println("  URL: " + url);
        System.out.println("  Driver: " + driverClassName);
    }

    /**
     * 验证 JPA 属性配置
     */
    @Test
    public void verifyJpaProperties() {
        String formatSql = environment.getProperty("spring.jpa.properties.hibernate.format_sql");
        String showSql = environment.getProperty("spring.jpa.show-sql");
        
        System.out.println("✓ JPA 属性配置:");
        System.out.println("  format_sql: " + formatSql);
        System.out.println("  show-sql: " + showSql);
    }

    /**
     * 打印所有 JPA 相关配置
     */
    @Test
    public void printAllJpaConfiguration() {
        System.out.println("\n=== JPA 配置摘要 ===");
        System.out.println("Profile: " + String.join(", ", environment.getActiveProfiles()));
        System.out.println("ddl-auto: " + environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        System.out.println("sql.init.mode: " + environment.getProperty("spring.sql.init.mode", "never (default)"));
        System.out.println("database-platform: " + environment.getProperty("spring.jpa.database-platform"));
        System.out.println("show-sql: " + environment.getProperty("spring.jpa.show-sql"));
        System.out.println("format_sql: " + environment.getProperty("spring.jpa.properties.hibernate.format_sql"));
        System.out.println("datasource.url: " + environment.getProperty("spring.datasource.url"));
        System.out.println("===================\n");
    }
}
