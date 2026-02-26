package com.sism.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Check the structure of task-related tables in the database
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SysTaskStructureChecker implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        checkTableStructure();
    }

    private void checkTableStructure() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Check if sys_task table exists
            ResultSet tables = metaData.getTables(null, null, "sys_task", null);

            if (tables.next()) {
                log.info("✓ sys_task table exists in database");

                // Get column information
                ResultSet columns = metaData.getColumns(null, null, "sys_task", null);
                log.info("Columns in sys_task table:");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    int columnSize = columns.getInt("COLUMN_SIZE");
                    log.info("  - {} ({}({}))", columnName, columnType, columnSize);
                }
            } else {
                log.warn("✗ sys_task table does not exist");

                // Check if strategic_task exists
                ResultSet strategicTables = metaData.getTables(null, null, "strategic_task", null);
                if (strategicTables.next()) {
                    log.info("✓ strategic_task table exists instead");

                    ResultSet columns = metaData.getColumns(null, null, "strategic_task", null);
                    log.info("Columns in strategic_task table:");
                    while (columns.next()) {
                        String columnName = columns.getString("COLUMN_NAME");
                        String columnType = columns.getString("TYPE_NAME");
                        int columnSize = columns.getInt("COLUMN_SIZE");
                        log.info("  - {} ({}({}))", columnName, columnType, columnSize);
                    }

                    // Rename the table
                    log.info("Renaming strategic_task to sys_task...");
                    jdbcTemplate.execute("ALTER TABLE strategic_task RENAME TO sys_task");
                    log.info("✓ Table renamed successfully");

                    // Check new structure
                    ResultSet newColumns = metaData.getColumns(null, null, "sys_task", null);
                    log.info("Columns in sys_task table after rename:");
                    while (newColumns.next()) {
                        String columnName = newColumns.getString("COLUMN_NAME");
                        String columnType = newColumns.getString("TYPE_NAME");
                        int columnSize = newColumns.getInt("COLUMN_SIZE");
                        log.info("  - {} ({}({}))", columnName, columnType, columnSize);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error checking table structure: {}", e.getMessage(), e);
        }

        // Also check sequence
        try {
            String sequenceQuery = """
                SELECT sequence_name, start_value, increment_by
                FROM information_schema.sequences
                WHERE sequence_name LIKE '%task%'
                """;
            jdbcTemplate.query(sequenceQuery, (rs) -> {
                log.info("Sequence: {} (start: {}, increment: {})",
                    rs.getString("sequence_name"),
                    rs.getLong("start_value"),
                    rs.getLong("increment_by"));
            });
        } catch (Exception e) {
            log.warn("Error checking sequences: {}", e.getMessage());
        }
    }
}
