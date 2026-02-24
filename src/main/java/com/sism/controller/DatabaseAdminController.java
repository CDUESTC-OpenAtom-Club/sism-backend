package com.sism.controller;

import com.sism.util.DatabaseDataChecker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Database Admin Controller
 * For debugging and data inspection purposes only
 * SHOULD BE DISABLED IN PRODUCTION
 */
@Slf4j
@RestController
@RequestMapping("/admin/database")
@RequiredArgsConstructor
@Tag(name = "Database Admin", description = "Database inspection endpoints (DEBUG ONLY)")
public class DatabaseAdminController {

    private final DatabaseDataChecker checker;

    /**
     * Get all table information
     */
    @GetMapping("/tables")
    @Operation(summary = "List all database tables", description = "DEBUG: List all tables with column counts")
    public ResponseEntity<Map<String, Object>> getAllTables() {
        log.warn("DEBUG: Accessing database tables list");

        List<Map<String, Object>> tables = checker.getAllTables();

        return ResponseEntity.ok(Map.of(
            "total", tables.size(),
            "tables", tables
        ));
    }

    /**
     * Get record counts for all tables
     */
    @GetMapping("/counts")
    @Operation(summary = "Get record counts", description = "DEBUG: Get record counts for all tables")
    public ResponseEntity<Map<String, Long>> getTableCounts() {
        log.warn("DEBUG: Accessing database table counts");

        Map<String, Long> counts = checker.getAllTableCounts();

        return ResponseEntity.ok(counts);
    }

    /**
     * Check data quality
     */
    @GetMapping("/quality-check")
    @Operation(summary = "Check data quality", description = "DEBUG: Check for orphaned records and data issues")
    public ResponseEntity<Map<String, Object>> checkDataQuality() {
        log.warn("DEBUG: Running data quality check");

        Map<String, Object> result = checker.checkDataQuality();

        return ResponseEntity.ok(result);
    }

    /**
     * Sample data from key tables
     */
    @GetMapping("/sample")
    @Operation(summary = "Get sample data", description = "DEBUG: Get sample data from key tables")
    public ResponseEntity<Map<String, Object>> getSampleData() {
        log.warn("DEBUG: Accessing sample data");

        Map<String, Object> samples = checker.sampleData();

        return ResponseEntity.ok(samples);
    }

    /**
     * Full database report
     */
    @GetMapping("/report")
    @Operation(summary = "Full database report", description = "DEBUG: Complete database status report")
    public ResponseEntity<Map<String, Object>> getFullReport() {
        log.warn("DEBUG: Generating full database report");

        Map<String, Long> counts = checker.getAllTableCounts();
        Map<String, Object> quality = checker.checkDataQuality();
        Map<String, Object> samples = checker.sampleData();
        List<Map<String, Object>> tables = checker.getAllTables();

        // Calculate statistics
        long totalRecords = counts.values().stream().mapToLong(Long::longValue).sum();
        long emptyTables = counts.values().stream().filter(c -> c == 0).count();
        long tablesWithData = counts.size() - emptyTables;

        return ResponseEntity.ok(Map.of(
            "summary", Map.of(
                "totalTables", counts.size(),
                "tablesWithData", tablesWithData,
                "emptyTables", emptyTables,
                "totalRecords", totalRecords
            ),
            "tableCounts", counts,
            "dataQuality", quality,
            "sampleData", samples,
            "tableDetails", tables
        ));
    }
}
