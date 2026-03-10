package com.sism.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * 数据库信息查询控制器
 * 仅用于开发环境调试
 */
@RestController
@RequestMapping("/api/debug/database")
public class DatabaseInfoController {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseInfoController(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @GetMapping("/tables")
    public Map<String, Object> listTables() throws Exception {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

            // 获取所有表
            List<Map<String, Object>> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(catalog, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    Map<String, Object> tableInfo = new HashMap<>();
                    String tableName = rs.getString("TABLE_NAME");
                    tableInfo.put("name", tableName);
                    tableInfo.put("remarks", rs.getString("REMARKS"));
                    tables.add(tableInfo);
                }
            }

            // 排序
            tables.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));

            // 获取每个表的行数
            List<Map<String, Object>> tablesWithCount = new ArrayList<>();
            int totalRows = 0;

            for (Map<String, Object> table : tables) {
                String tableName = (String) table.get("name");
                try {
                    Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName + "\"", Integer.class);

                    Map<String, Object> tableData = new HashMap<>();
                    tableData.put("name", tableName);
                    tableData.put("rowCount", count != null ? count : 0);
                    tableData.put("remarks", table.get("remarks"));
                    tablesWithCount.add(tableData);

                    totalRows += count != null ? count : 0;
                } catch (Exception e) {
                    Map<String, Object> tableData = new HashMap<>();
                    tableData.put("name", tableName);
                    tableData.put("rowCount", "ERROR");
                    tableData.put("error", e.getMessage());
                    tablesWithCount.add(tableData);
                }
            }

            result.put("database", catalog);
            result.put("tableCount", tablesWithCount.size());
            result.put("totalRows", totalRows);
            result.put("tables", tablesWithCount);
        }

        return result;
    }
}
