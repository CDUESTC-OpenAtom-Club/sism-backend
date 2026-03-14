package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.DataExportApplicationService;
import com.sism.analytics.application.ExportService;
import com.sism.analytics.domain.DataExport;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DataExportController - 数据导出API控制器
 * 提供数据导出管理相关的REST API端点
 */
@RestController
@RequestMapping("/api/v1/analytics/exports")
@RequiredArgsConstructor
@Tag(name = "Analytics Data Exports", description = "Data export management endpoints")
public class DataExportController {

    private final DataExportApplicationService dataExportApplicationService;
    private final ExportService exportService;

    // ==================== Data Export Endpoints ====================

    @PostMapping
    @Operation(summary = "Create a new data export task")
    public ResponseEntity<ApiResponse<DataExport>> createDataExport(@RequestBody CreateDataExportRequest request) {
        DataExport dataExport = dataExportApplicationService.createDataExport(
                request.getName(),
                request.getType(),
                request.getFormat(),
                request.getRequestedBy(),
                request.getParameters()
        );
        return ResponseEntity.ok(ApiResponse.success(dataExport));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start processing a data export")
    public ResponseEntity<ApiResponse<DataExport>> startProcessing(@PathVariable Long id) {
        DataExport dataExport = dataExportApplicationService.startProcessing(id);
        return ResponseEntity.ok(ApiResponse.success(dataExport));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a data export")
    public ResponseEntity<ApiResponse<DataExport>> completeDataExport(
            @PathVariable Long id,
            @RequestBody CompleteDataExportRequest request) {
        DataExport dataExport = dataExportApplicationService.completeDataExport(
                id,
                request.getFilePath(),
                request.getFileSize()
        );
        return ResponseEntity.ok(ApiResponse.success(dataExport));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "Mark data export as failed")
    public ResponseEntity<ApiResponse<DataExport>> failDataExport(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        DataExport dataExport = dataExportApplicationService.failDataExport(id, request.get("errorMessage"));
        return ResponseEntity.ok(ApiResponse.success(dataExport));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed data export")
    public ResponseEntity<ApiResponse<DataExport>> retryDataExport(@PathVariable Long id) {
        DataExport dataExport = dataExportApplicationService.retryDataExport(id);
        return ResponseEntity.ok(ApiResponse.success(dataExport));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a data export")
    public ResponseEntity<ApiResponse<Void>> deleteDataExport(@PathVariable Long id) {
        dataExportApplicationService.deleteDataExport(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get data export by ID")
    public ResponseEntity<ApiResponse<DataExport>> getDataExportById(@PathVariable Long id) {
        return dataExportApplicationService.findDataExportById(id)
                .map(dataExport -> ResponseEntity.ok(ApiResponse.success(dataExport)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{requestedBy}")
    @Operation(summary = "Get data exports by requested user")
    public ResponseEntity<ApiResponse<List<DataExport>>> getDataExportsByRequestedBy(@PathVariable Long requestedBy) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByRequestedBy(requestedBy);
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/user/{requestedBy}/downloadable")
    @Operation(summary = "Get downloadable data exports by user")
    public ResponseEntity<ApiResponse<List<DataExport>>> getDownloadableByRequestedBy(@PathVariable Long requestedBy) {
        List<DataExport> dataExports = dataExportApplicationService.findDownloadableByRequestedBy(requestedBy);
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/downloadable")
    @Operation(summary = "Get all downloadable data exports")
    public ResponseEntity<ApiResponse<List<DataExport>>> getAllDownloadable() {
        List<DataExport> dataExports = dataExportApplicationService.findAllDownloadable();
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all pending data exports")
    public ResponseEntity<ApiResponse<List<DataExport>>> getAllPending() {
        List<DataExport> dataExports = dataExportApplicationService.findAllPending();
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/failed")
    @Operation(summary = "Get all failed data exports")
    public ResponseEntity<ApiResponse<List<DataExport>>> getAllFailed() {
        List<DataExport> dataExports = dataExportApplicationService.findAllFailed();
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/retryable")
    @Operation(summary = "Get all retryable data exports")
    public ResponseEntity<ApiResponse<List<DataExport>>> getAllRetryable() {
        List<DataExport> dataExports = dataExportApplicationService.findAllRetryable();
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get data exports by status")
    public ResponseEntity<ApiResponse<List<DataExport>>> getDataExportsByStatus(@PathVariable String status) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/user/{requestedBy}/status/{status}")
    @Operation(summary = "Get data exports by user and status")
    public ResponseEntity<ApiResponse<List<DataExport>>> getDataExportsByRequestedByAndStatus(
            @PathVariable Long requestedBy,
            @PathVariable String status) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByRequestedByAndStatus(requestedBy, status);
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get data exports by date range")
    public ResponseEntity<ApiResponse<List<DataExport>>> getDataExportsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/search")
    @Operation(summary = "Search data exports by name")
    public ResponseEntity<ApiResponse<List<DataExport>>> searchDataExportsByName(@RequestParam String name) {
        List<DataExport> dataExports = dataExportApplicationService.searchDataExportsByName(name);
        return ResponseEntity.ok(ApiResponse.success(dataExports));
    }

    @GetMapping("/count/user/{requestedBy}")
    @Operation(summary = "Count data exports by requested user")
    public ResponseEntity<ApiResponse<Long>> countDataExportsByRequestedBy(@PathVariable Long requestedBy) {
        long count = dataExportApplicationService.countDataExportsByRequestedBy(requestedBy);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "Count data exports by status")
    public ResponseEntity<ApiResponse<Long>> countDataExportsByStatus(@PathVariable String status) {
        long count = dataExportApplicationService.countDataExportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ==================== File Download ====================

    @GetMapping("/{id}/download")
    @Operation(summary = "Download exported file")
    public ResponseEntity<byte[]> downloadExportedFile(@PathVariable Long id) throws IOException {
        Path filePath = exportService.getExportFilePath(id);
        byte[] fileContent = Files.readAllBytes(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileContent);
    }

    // ==================== Cleanup ====================

    @PostMapping("/cleanup")
    @Operation(summary = "Cleanup expired data exports")
    public ResponseEntity<ApiResponse<Integer>> cleanupExpiredExports(@RequestParam(defaultValue = "7") int daysToKeep) {
        int cleanedCount = exportService.cleanupExpiredExports(daysToKeep);
        return ResponseEntity.ok(ApiResponse.success(cleanedCount));
    }

    // ==================== Request DTOs ====================

    @lombok.Data
    public static class CreateDataExportRequest {
        private String name;
        private String type;
        private String format;
        private Long requestedBy;
        private String parameters;
    }

    @lombok.Data
    public static class CompleteDataExportRequest {
        private String filePath;
        private Long fileSize;
    }
}
