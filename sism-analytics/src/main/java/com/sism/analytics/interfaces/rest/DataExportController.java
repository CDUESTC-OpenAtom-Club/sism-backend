package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.DataExportApplicationService;
import com.sism.analytics.application.ExportService;
import com.sism.analytics.domain.DataExport;
import com.sism.analytics.interfaces.dto.CompleteDataExportRequest;
import com.sism.analytics.interfaces.dto.CreateDataExportRequest;
import com.sism.analytics.interfaces.dto.DataExportDTO;
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
import java.util.stream.Collectors;

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
    public ResponseEntity<ApiResponse<DataExportDTO>> createDataExport(@RequestBody CreateDataExportRequest request) {
        DataExport dataExport = dataExportApplicationService.createDataExport(
                request.getName(),
                request.getType(),
                request.getFormat(),
                request.getRequestedBy(),
                request.getParameters()
        );
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start processing a data export")
    public ResponseEntity<ApiResponse<DataExportDTO>> startProcessing(@PathVariable Long id) {
        DataExport dataExport = dataExportApplicationService.startProcessing(id);
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a data export")
    public ResponseEntity<ApiResponse<DataExportDTO>> completeDataExport(
            @PathVariable Long id,
            @RequestBody CompleteDataExportRequest request) {
        DataExport dataExport = dataExportApplicationService.completeDataExport(
                id,
                request.getFilePath(),
                request.getFileSize()
        );
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "Mark data export as failed")
    public ResponseEntity<ApiResponse<DataExportDTO>> failDataExport(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        DataExport dataExport = dataExportApplicationService.failDataExport(id, request.get("errorMessage"));
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed data export")
    public ResponseEntity<ApiResponse<DataExportDTO>> retryDataExport(@PathVariable Long id) {
        DataExport dataExport = dataExportApplicationService.retryDataExport(id);
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a data export")
    public ResponseEntity<ApiResponse<Void>> deleteDataExport(@PathVariable Long id) {
        dataExportApplicationService.deleteDataExport(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get data export by ID")
    public ResponseEntity<ApiResponse<DataExportDTO>> getDataExportById(@PathVariable Long id) {
        return dataExportApplicationService.findDataExportById(id)
                .map(dataExport -> ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{requestedBy}")
    @Operation(summary = "Get data exports by requested user")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByRequestedBy(@PathVariable Long requestedBy) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByRequestedBy(requestedBy);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/user/{requestedBy}/downloadable")
    @Operation(summary = "Get downloadable data exports by user")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDownloadableByRequestedBy(@PathVariable Long requestedBy) {
        List<DataExport> dataExports = dataExportApplicationService.findDownloadableByRequestedBy(requestedBy);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/downloadable")
    @Operation(summary = "Get all downloadable data exports")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllDownloadable() {
        List<DataExport> dataExports = dataExportApplicationService.findAllDownloadable();
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all pending data exports")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllPending() {
        List<DataExport> dataExports = dataExportApplicationService.findAllPending();
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/failed")
    @Operation(summary = "Get all failed data exports")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllFailed() {
        List<DataExport> dataExports = dataExportApplicationService.findAllFailed();
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/retryable")
    @Operation(summary = "Get all retryable data exports")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllRetryable() {
        List<DataExport> dataExports = dataExportApplicationService.findAllRetryable();
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get data exports by status")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByStatus(@PathVariable String status) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/user/{requestedBy}/status/{status}")
    @Operation(summary = "Get data exports by user and status")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByRequestedByAndStatus(
            @PathVariable Long requestedBy,
            @PathVariable String status) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByRequestedByAndStatus(requestedBy, status);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get data exports by date range")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/search")
    @Operation(summary = "Search data exports by name")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> searchDataExportsByName(@RequestParam String name) {
        List<DataExport> dataExports = dataExportApplicationService.searchDataExportsByName(name);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
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

    /**
     * 将 DataExport 实体转换为 DTO
     */
    private DataExportDTO toDataExportDTO(DataExport dataExport) {
        return DataExportDTO.builder()
                .id(dataExport.getId())
                .name(dataExport.getName())
                .type(dataExport.getType())
                .format(dataExport.getFormat())
                .status(dataExport.getStatus())
                .filePath(dataExport.getFilePath())
                .fileSize(dataExport.getFileSize())
                .requestedBy(dataExport.getRequestedBy())
                .requestedAt(dataExport.getRequestedAt())
                .startedAt(dataExport.getStartedAt())
                .completedAt(dataExport.getCompletedAt())
                .errorMessage(dataExport.getErrorMessage())
                .parameters(dataExport.getParameters())
                .createdAt(dataExport.getCreatedAt())
                .updatedAt(dataExport.getUpdatedAt())
                .build();
    }
}
