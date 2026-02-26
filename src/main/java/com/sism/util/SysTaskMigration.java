package com.sism.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migrate strategic_task table to sys_task
 * This is a one-time migration utility
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SysTaskMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // Check if migration is needed
        String checkTable = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'strategic_task'";
        Integer strategicTaskExists = jdbcTemplate.queryForObject(checkTable, Integer.class);

        if (strategicTaskExists != null && strategicTaskExists > 0) {
            log.info("=== Starting sys_task migration ===");

            // Backup data
            log.info("Step 1: Backing up strategic_task data...");
            int backupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM strategic_task", Integer.class);
            log.info("✓ Found {} records in strategic_task", backupCount);

            // Create backup table
            log.info("Step 2: Creating backup table...");
            try {
                jdbcTemplate.execute("DROP TABLE IF EXISTS strategic_task_backup");
                jdbcTemplate.execute("CREATE TABLE strategic_task_backup AS SELECT * FROM strategic_task");
                log.info("✓ Backup table created");
            } catch (Exception e) {
                log.warn("Backup table creation failed: {}", e.getMessage());
            }

            // Rename the table
            log.info("Step 3: Renaming strategic_task to sys_task...");
            try {
                jdbcTemplate.execute("ALTER TABLE strategic_task RENAME TO sys_task");
                log.info("✓ Table successfully renamed to sys_task");
            } catch (Exception e) {
                log.error("✗ Table rename failed: {}", e.getMessage());
                return;
            }

            // Verify the rename
            log.info("Step 4: Verifying table rename...");
            Integer sysTaskExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'sys_task'",
                Integer.class);

            if (sysTaskExists != null && sysTaskExists > 0) {
                int recordCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_task", Integer.class);
                log.info("✓ Verification successful - sys_task table exists with {} records", recordCount);
                log.info("=== Migration completed successfully ===");
            } else {
                log.error("✗ Verification failed - sys_task table not found");
            }
        } else {
            log.info("strategic_task table not found - migration not needed");
        }
    }
}
