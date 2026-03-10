package com.sism.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库表列表命令行工具
 * 运行方式: mvn spring-boot:run -Dspring-boot.run.arguments="--table-list"
 */
@Component
public class DatabaseTableListCommand implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseTableListCommand(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        // 检查是否有 --table-list 参数
        boolean shouldRun = false;
        for (String arg : args) {
            if ("--table-list".equals(arg)) {
                shouldRun = true;
                break;
            }
        }

        if (!shouldRun) {
            return;
        }

        System.out.println("========================================");
        System.out.println("数据库表检查工具");
        System.out.println("========================================");

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

            System.out.println("数据库: " + catalog);
            System.out.println();

            // 获取所有表
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(catalog, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    tables.add(tableName);
                }
            }

            // 排序
            tables.sort(String::compareToIgnoreCase);

            // 获取每个表的行数
            System.out.println("========================================");
            System.out.println("数据库中的所有表 (" + tables.size() + " 张)");
            System.out.println("========================================");
            System.out.println();

            int totalRows = 0;
            for (String tableName : tables) {
                try {
                    Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + tableName, Integer.class);
                    System.out.println(String.format("%-45s | 行数: %6d",
                        tableName, count != null ? count : 0));
                    totalRows += count != null ? count : 0;
                } catch (Exception e) {
                    System.out.println(String.format("%-45s | 行数: %6s (查询失败: %s)",
                        tableName, "N/A", e.getMessage()));
                }
            }

            System.out.println();
            System.out.println("========================================");
            System.out.println("总计: " + tables.size() + " 张表");
            System.out.println("========================================");

            // 退出应用
            System.exit(0);

        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
