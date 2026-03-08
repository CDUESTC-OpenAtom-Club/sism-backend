package com.sism.controller;

import com.sism.service.DatabaseAdminService;
import com.sism.util.DatabaseDataChecker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Database Admin Controller
 * For debugging and data inspection purposes only
 * RESTRICTED TO DEVELOPMENT ENVIRONMENT ONLY
 * 
 * This controller provides database inspection capabilities for development
 * and debugging purposes. It is automatically disabled in production environments
 * through Spring profile restrictions.
 * 
 * @security This controller exposes sensitive database information and should
 *           NEVER be enabled in production environments.
 */
@Slf4j
@RestController
@RequestMapping("/admin/database")
@RequiredArgsConstructor
@Profile("dev")
@Tag(name = "Database Admin", description = "Database inspection endpoints (DEVELOPMENT ONLY - Disabled in production)")
public class DatabaseAdminController {

    private final DatabaseDataChecker checker;
    private final DatabaseAdminService adminService;
    private final Environment environment;

    /**
     * Check if running in development environment and log warning if not
     */
    private void checkEnvironmentAndLogWarning(String operation) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevProfile = Arrays.asList(activeProfiles).contains("dev");
        
        if (!isDevProfile) {
            log.error("SECURITY WARNING: Database admin operation '{}' accessed in non-development environment. Active profiles: {}",
                    operation, Arrays.toString(activeProfiles));
        } else {
            log.warn("SECURITY-SENSITIVE: Database admin operation '{}' accessed in development environment", operation);
        }
    }

    /**
     * Get all table information
     * 
     * @apiNote DEVELOPMENT ONLY - This endpoint is disabled in production environments
     */
    @GetMapping("/tables")
    @Operation(
        summary = "List all database tables", 
        description = "DEVELOPMENT ONLY: List all tables with column counts. This endpoint is automatically disabled in production through profile restrictions."
    )
    public ResponseEntity<Map<String, Object>> getAllTables() {
        checkEnvironmentAndLogWarning("getAllTables");

        List<Map<String, Object>> tables = checker.getAllTables();

        return ResponseEntity.ok(Map.of(
            "total", tables.size(),
            "tables", tables
        ));
    }

    /**
     * Get record counts for all tables
     * 
     * @apiNote DEVELOPMENT ONLY - This endpoint is disabled in production environments
     */
    @GetMapping("/counts")
    @Operation(
        summary = "Get record counts", 
        description = "DEVELOPMENT ONLY: Get record counts for all tables. This endpoint is automatically disabled in production through profile restrictions."
    )
    public ResponseEntity<Map<String, Long>> getTableCounts() {
        checkEnvironmentAndLogWarning("getTableCounts");

        Map<String, Long> counts = checker.getAllTableCounts();

        return ResponseEntity.ok(counts);
    }

    /**
     * Check data quality
     * 
     * @apiNote DEVELOPMENT ONLY - This endpoint is disabled in production environments
     */
    @GetMapping("/quality-check")
    @Operation(
        summary = "Check data quality", 
        description = "DEVELOPMENT ONLY: Check for orphaned records and data issues. This endpoint is automatically disabled in production through profile restrictions."
    )
    public ResponseEntity<Map<String, Object>> checkDataQuality() {
        checkEnvironmentAndLogWarning("checkDataQuality");

        Map<String, Object> result = checker.checkDataQuality();

        return ResponseEntity.ok(result);
    }

    /**
     * Sample data from key tables
     * 
     * @apiNote DEVELOPMENT ONLY - This endpoint is disabled in production environments
     */
    @GetMapping("/sample")
    @Operation(
        summary = "Get sample data", 
        description = "DEVELOPMENT ONLY: Get sample data from key tables. This endpoint is automatically disabled in production through profile restrictions."
    )
    public ResponseEntity<Map<String, Object>> getSampleData() {
        checkEnvironmentAndLogWarning("getSampleData");

        Map<String, Object> samples = checker.sampleData();

        return ResponseEntity.ok(samples);
    }

    /**
     * Full database report
     * 
     * @apiNote DEVELOPMENT ONLY - This endpoint is disabled in production environments
     */
    @GetMapping("/report")
    @Operation(
        summary = "Full database report", 
        description = "DEVELOPMENT ONLY: Complete database status report. This endpoint is automatically disabled in production through profile restrictions."
    )
    public ResponseEntity<Map<String, Object>> getFullReport() {
        checkEnvironmentAndLogWarning("getFullReport");

        Map<String, Object> report = adminService.getDatabaseReport();

        return ResponseEntity.ok(report);
    }
}
