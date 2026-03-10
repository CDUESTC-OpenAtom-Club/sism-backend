package com.sism.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库表检查工具
 * 用于直接连接数据库查看实际的表结构
 */
public class DatabaseTableInspector {

    public static void main(String[] args) {
        String url = "jdbc:postgresql://175.24.139.148:8386/strategic?stringtype=unspecified";
        String username = "postgres";
        String password = "64378561huaW";

        System.out.println("========================================");
        System.out.println("数据库表检查工具");
        System.out.println("========================================");
        System.out.println("连接: " + url);
        System.out.println();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("✅ 数据库连接成功!");
            System.out.println();

            // 获取数据库元数据
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = "public";

            System.out.println("数据库: " + catalog);
            System.out.println("Schema: " + schema);
            System.out.println();

            // 获取所有表
            List<TableInfo> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String remarks = rs.getString("REMARKS");
                    tables.add(new TableInfo(tableName, remarks));
                }
            }

            // 获取每个表的行数
            System.out.println("========================================");
            System.out.println("数据库中的所有表 (" + tables.size() + " 张)");
            System.out.println("========================================");
            System.out.println();

            for (TableInfo table : tables) {
                int rowCount = getTableRowCount(conn, table.name);
                System.out.println(String.format("%-40s | 行数: %6d | %s",
                    table.name, rowCount, table.remarks));
            }

            System.out.println();
            System.out.println("========================================");
            System.out.println("总计: " + tables.size() + " 张表");
            System.out.println("========================================");

        } catch (SQLException e) {
            System.err.println("❌ 数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int getTableRowCount(Connection conn, String tableName) {
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            return -1; // 查询失败
        }
        return 0;
    }

    static class TableInfo {
        String name;
        String remarks;

        TableInfo(String name, String remarks) {
            this.name = name;
            this.remarks = remarks;
        }
    }
}
