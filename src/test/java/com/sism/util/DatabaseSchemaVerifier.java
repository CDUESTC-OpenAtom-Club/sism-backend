package com.sism.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据库表结构验证测试
 * 验证所有表、外键约束和索引是否正确创建
 */
@SpringBootTest
@ActiveProfiles("test")
public class DatabaseSchemaVerifier {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 验证所有必需的表是否存在
     */
    @Test
    public void verifyAllTablesExist() {
        String sql = """
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public' 
                AND table_type = 'BASE TABLE'
            ORDER BY table_name
            """;

        List<String> tables = jdbcTemplate.queryForList(sql, String.class);

        // 验证核心表存在
        assertThat(tables).contains(
            "org",
            "app_user",
            "assessment_cycle",
            "strategic_task",
            "indicator",
            "milestone",
            "progress_report",
            "approval_record",
            "alert_window",
            "alert_rule",
            "alert_event",
            "adhoc_task",
            "adhoc_task_target",
            "adhoc_task_indicator_map",
            "audit_log"
        );

        System.out.println("✓ 所有必需的表都已创建");
        System.out.println("  找到 " + tables.size() + " 个表");
    }

    /**
     * 验证所有枚举类型是否存在
     */
    @Test
    public void verifyAllEnumTypesExist() {
        String sql = """
            SELECT typname 
            FROM pg_type 
            WHERE typtype = 'e'
            ORDER BY typname
            """;

        List<String> enumTypes = jdbcTemplate.queryForList(sql, String.class);

        // 验证枚举类型存在
        assertThat(enumTypes).contains(
            "org_type",
            "task_type",
            "indicator_level",
            "indicator_status",
            "milestone_status",
            "report_status",
            "approval_action",
            "alert_severity",
            "alert_status",
            "adhoc_scope_type",
            "adhoc_task_status",
            "audit_action",
            "audit_entity_type"
        );

        System.out.println("✓ 所有枚举类型都已创建");
        System.out.println("  找到 " + enumTypes.size() + " 个枚举类型");
    }

    /**
     * 验证外键约束
     */
    @Test
    public void verifyForeignKeyConstraints() {
        String sql = """
            SELECT 
                tc.table_name,
                tc.constraint_name,
                kcu.column_name,
                ccu.table_name AS foreign_table_name,
                ccu.column_name AS foreign_column_name
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.key_column_usage AS kcu
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage AS ccu
                ON ccu.constraint_name = tc.constraint_name
                AND ccu.table_schema = tc.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
                AND tc.table_schema = 'public'
            ORDER BY tc.table_name, tc.constraint_name
            """;

        List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(sql);

        assertThat(foreignKeys).isNotEmpty();

        System.out.println("✓ 外键约束验证通过");
        System.out.println("  找到 " + foreignKeys.size() + " 个外键约束");

        // 打印部分外键信息
        foreignKeys.stream().limit(5).forEach(fk -> {
            System.out.println("  - " + fk.get("table_name") + "." + fk.get("column_name") +
                " -> " + fk.get("foreign_table_name") + "." + fk.get("foreign_column_name"));
        });
    }

    /**
     * 验证索引
     */
    @Test
    public void verifyIndexes() {
        String sql = """
            SELECT 
                tablename,
                indexname
            FROM pg_indexes
            WHERE schemaname = 'public'
            ORDER BY tablename, indexname
            """;

        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(sql);

        assertThat(indexes).isNotEmpty();

        System.out.println("✓ 索引验证通过");
        System.out.println("  找到 " + indexes.size() + " 个索引");
    }

    /**
     * 验证主键约束
     */
    @Test
    public void verifyPrimaryKeyConstraints() {
        String sql = """
            SELECT 
                tc.table_name,
                tc.constraint_name,
                kcu.column_name
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.key_column_usage AS kcu
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            WHERE tc.constraint_type = 'PRIMARY KEY'
                AND tc.table_schema = 'public'
            ORDER BY tc.table_name
            """;

        List<Map<String, Object>> primaryKeys = jdbcTemplate.queryForList(sql);

        // 15 tables, but some have composite primary keys (adhoc_task_indicator_map, adhoc_task_target)
        // So we check that we have at least 15 primary key entries
        assertThat(primaryKeys.size()).isGreaterThanOrEqualTo(15);

        System.out.println("✓ 主键约束验证通过");
        System.out.println("  找到 " + primaryKeys.size() + " 个主键");
    }

    /**
     * 验证检查约束
     */
    @Test
    public void verifyCheckConstraints() {
        String sql = """
            SELECT 
                tc.table_name,
                tc.constraint_name,
                cc.check_clause
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.check_constraints AS cc
                ON tc.constraint_name = cc.constraint_name
            WHERE tc.constraint_type = 'CHECK'
                AND tc.table_schema = 'public'
            ORDER BY tc.table_name
            """;

        List<Map<String, Object>> checkConstraints = jdbcTemplate.queryForList(sql);

        // 验证 progress_report 表的互斥约束存在
        boolean hasMutualExclusionCheck = checkConstraints.stream()
            .anyMatch(cc -> "progress_report".equals(cc.get("table_name")) &&
                cc.get("check_clause").toString().contains("milestone_id") &&
                cc.get("check_clause").toString().contains("adhoc_task_id"));

        assertThat(hasMutualExclusionCheck)
            .as("progress_report 表应该有 milestone_id 和 adhoc_task_id 的互斥约束")
            .isTrue();

        System.out.println("✓ 检查约束验证通过");
        System.out.println("  找到 " + checkConstraints.size() + " 个检查约束");
    }

    /**
     * 验证触发器
     */
    @Test
    public void verifyTriggers() {
        String sql = """
            SELECT 
                trigger_name,
                event_object_table AS table_name,
                action_timing,
                event_manipulation
            FROM information_schema.triggers
            WHERE trigger_schema = 'public'
            ORDER BY event_object_table, trigger_name
            """;

        List<Map<String, Object>> triggers = jdbcTemplate.queryForList(sql);

        assertThat(triggers).isNotEmpty();

        System.out.println("✓ 触发器验证通过");
        System.out.println("  找到 " + triggers.size() + " 个触发器");
    }

    /**
     * 验证视图
     */
    @Test
    public void verifyViews() {
        String sql = """
            SELECT table_name 
            FROM information_schema.views 
            WHERE table_schema = 'public'
            ORDER BY table_name
            """;

        List<String> views = jdbcTemplate.queryForList(sql, String.class);

        // 验证业务视图存在
        assertThat(views).contains(
            "v_milestone_weight_sum",
            "v_indicator_latest_report",
            "v_overdue_milestones"
        );

        System.out.println("✓ 视图验证通过");
        System.out.println("  找到 " + views.size() + " 个视图");
    }

    /**
     * 统计各表记录数
     */
    @Test
    public void verifyTableRowCounts() {
        String sql = """
            SELECT 
                relname AS table_name,
                n_live_tup AS row_count
            FROM pg_stat_user_tables
            WHERE schemaname = 'public'
            ORDER BY relname
            """;

        List<Map<String, Object>> rowCounts = jdbcTemplate.queryForList(sql);

        System.out.println("✓ 表记录数统计:");
        rowCounts.forEach(rc -> {
            System.out.println("  - " + rc.get("table_name") + ": " + rc.get("row_count") + " 行");
        });
    }
}
