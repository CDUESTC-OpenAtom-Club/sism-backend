package com.sism.cli;

import com.sism.util.DatabaseDataChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Database Analysis and Report CLI
 * Runs on application startup to analyze database state
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseAnalysisRunner implements CommandLineRunner {

    private final DatabaseDataChecker checker;

    @Override
    public void run(String... args) throws Exception {
        if (!shouldRun(args)) {
            log.info("Skipping database analysis. Use --analyze-db to enable.");
            return;
        }

        log.info("============================================");
        log.info("SISM Database Analysis Report");
        log.info("============================================");
        System.out.println();

        // Step 1: Count all tables
        log.info("Step 1: Counting all tables...");
        Map<String, Long> counts = checker.getAllTableCounts();
        printTableCounts(counts);
        System.out.println();

        // Step 2: Check data quality
        log.info("Step 2: Checking data quality...");
        @SuppressWarnings("unchecked")
        Map<String, Object> quality = checker.checkDataQuality();
        printDataQuality(quality);
        System.out.println();

        // Step 3: Sample data
        log.info("Step 3: Sampling data...");
        Map<String, Object> samples = checker.sampleData();
        printSampleData(samples);
        System.out.println();

        log.info("============================================");
        log.info("Database analysis complete!");
        log.info("============================================");
    }

    private boolean shouldRun(String[] args) {
        for (String arg : args) {
            if ("--analyze-db".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private void printTableCounts(Map<String, Long> counts) {
        System.out.println(">>> Table Record Counts");
        System.out.println();

        long totalRecords = 0;
        long emptyTables = 0;

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            System.out.printf("%-35s %,6d records%n", entry.getKey(), entry.getValue());
            totalRecords += entry.getValue();
            if (entry.getValue() == 0) {
                emptyTables++;
            }
        }

        System.out.println();
        System.out.printf("Summary:%n");
        System.out.printf("  - Total tables: %d%n", counts.size());
        System.out.printf("  - Tables with data: %d%n", counts.size() - emptyTables);
        System.out.printf("  - Empty tables: %d%n", emptyTables);
        System.out.printf("  - Total records: %,d%n", totalRecords);
    }

    private void printDataQuality(Map<String, Object> quality) {
        System.out.println(">>> Data Quality Issues");
        System.out.println();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) quality.get("issues");

        if (issues.isEmpty()) {
            System.out.println("✓ No data quality issues found!");
        } else {
            System.out.printf("Found %d issues:%n%n", issues.size());

            for (Map<String, Object> issue : issues) {
                System.out.printf("[%s] %s%n", issue.get("type"), issue.get("description"));
                System.out.printf("  Count: %d%n", issue.get("count"));

                @SuppressWarnings("unchecked")
                List<?> samples = (List<?>) issue.get("samples");
                if (samples != null && !samples.isEmpty()) {
                    System.out.println("  Samples:");
                    for (Object sample : samples) {
                        System.out.println("    - " + sample);
                    }
                }
                System.out.println();
            }
        }
    }

    private void printSampleData(Map<String, Object> samples) {
        System.out.println(">>> Sample Data (First 5 records)");
        System.out.println();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> indicators = (List<Map<String, Object>>) samples.get("indicators");
        if (indicators != null && !indicators.isEmpty()) {
            System.out.println("Indicators:");
            for (Map<String, Object> indicator : indicators) {
                System.out.printf("  - ID: %s, Desc: %s, Task: %s, Progress: %s%%%n",
                    indicator.get("id"),
                    indicator.get("indicator_desc"),
                    indicator.get("task_id"),
                    indicator.get("progress"));
            }
            System.out.println();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) samples.get("tasks");
        if (tasks != null && !tasks.isEmpty()) {
            System.out.println("Tasks:");
            for (Map<String, Object> task : tasks) {
                System.out.printf("  - ID: %s, Name: %s, Cycle: %s, Org: %s%n",
                    task.get("id"),
                    task.get("task_name"),
                    task.get("cycle_id"),
                    task.get("org_id"));
            }
            System.out.println();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orgs = (List<Map<String, Object>>) samples.get("organizations");
        if (orgs != null && !orgs.isEmpty()) {
            System.out.println("Organizations:");
            for (Map<String, Object> org : orgs) {
                System.out.printf("  - ID: %s, Name: %s, Type: %s, Parent: %s%n",
                    org.get("id"),
                    org.get("name"),
                    org.get("type"),
                    org.get("parent_id"));
            }
        }
    }
}
