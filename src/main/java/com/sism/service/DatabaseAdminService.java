package com.sism.service;

import com.sism.util.DatabaseDataChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Database Admin Service
 * Provides database analysis and management functions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseAdminService {

    private final DatabaseDataChecker checker;

    /**
     * Get complete database report
     */
    public Map<String, Object> getDatabaseReport() {
        log.info("Generating database report...");

        Map<String, Long> counts = checker.getAllTableCounts();
        Map<String, Object> quality = checker.checkDataQuality();
        Map<String, Object> samples = checker.sampleData();

        // Calculate statistics
        long totalRecords = counts.values().stream().mapToLong(Long::longValue).sum();
        long emptyTables = counts.values().stream().filter(c -> c == 0).count();
        long tablesWithData = counts.size() - emptyTables;

        // Check for critical issues
        List<String> criticalIssues = new ArrayList<>();
        if (counts.getOrDefault("sys_org", 0L) == 0) {
            criticalIssues.add("sys_org table is empty - system cannot function!");
        }
        if (counts.getOrDefault("assessment_cycle", 0L) == 0) {
            criticalIssues.add("assessment_cycle table is empty - cannot create tasks!");
        }
        if (counts.getOrDefault("sys_task", 0L) == 0) {
            criticalIssues.add("sys_task table is empty - cannot create indicators!");
        }
        if (counts.getOrDefault("indicator", 0L) == 0) {
            criticalIssues.add("indicator table is empty - no strategic metrics!");
        }

        // Check for test data
        long totalTestUsers = 0;
        long totalTestOrgs = 0;
        long totalTestTasks = 0;
        long totalTestIndicators = 0;

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> indicators = (List<Map<String, Object>>) samples.get("indicators");
            if (indicators != null) {
                totalTestIndicators = indicators.stream()
                    .filter(i -> i.get("indicator_desc") != null)
                    .filter(i -> i.get("indicator_desc").toString().toLowerCase().contains("test"))
                    .count();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) samples.get("tasks");
            if (tasks != null) {
                totalTestTasks = tasks.stream()
                    .filter(t -> t.get("task_name") != null)
                    .filter(t -> t.get("task_name").toString().toLowerCase().contains("test"))
                    .count();
            }
        } catch (Exception e) {
            log.warn("Error detecting test data: {}", e.getMessage());
        }

        Map<String, Object> report = new HashMap<>();
        report.put("summary", Map.of(
            "totalTables", counts.size(),
            "tablesWithData", tablesWithData,
            "emptyTables", emptyTables,
            "totalRecords", totalRecords,
            "hasCriticalIssues", !criticalIssues.isEmpty(),
            "criticalIssues", criticalIssues
        ));
        report.put("tableCounts", counts);
        report.put("dataQuality", quality);
        report.put("sampleData", samples);
        report.put("testDataStats", Map.of(
            "testUsers", totalTestUsers,
            "testOrgs", totalTestOrgs,
            "testTasks", totalTestTasks,
            "testIndicators", totalTestIndicators
        ));

        return report;
    }
}
