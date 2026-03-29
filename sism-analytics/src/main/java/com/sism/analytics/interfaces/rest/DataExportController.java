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
@Tag(name = "分析数据导出", description = "数据导出管理接口")
public class DataExportController {

    private final DataExportApplicationService dataExportApplicationService;
    private final ExportService exportService;

    // ==================== Data Export Endpoints ====================

    @PostMapping
    @Operation(summary = "创建新的数据导出任务")
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
    @Operation(summary = "开始处理数据导出")
    public ResponseEntity<ApiResponse<DataExportDTO>> startProcessing(@PathVariable Long id) {
        DataExport dataExport = dataExportApplicationService.startProcessing(id);
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成数据导出")
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
    @Operation(summary = "标记数据导出为失败")
    public ResponseEntity<ApiResponse<DataExportDTO>> failDataExport(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        DataExport dataExport = dataExportApplicationService.failDataExport(id, request.get("errorMessage"));
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "重试失败的数据导出")
    public ResponseEntity<ApiResponse<DataExportDTO>> retryDataExport(@PathVariable Long id) {
        DataExport dataExport = dataExportApplicationService.retryDataExport(id);
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据导出")
    public ResponseEntity<ApiResponse<Void>> deleteDataExport(@PathVariable Long id) {
        dataExportApplicationService.deleteDataExport(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取数据导出")
    public ResponseEntity<ApiResponse<DataExportDTO>> getDataExportById(@PathVariable Long id) {
        return dataExportApplicationService.findDataExportById(id)
                .map(dataExport -> ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{requestedBy}")
    @Operation(summary = "根据请求用户获取数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByRequestedBy(@PathVariable Long requestedBy) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByRequestedBy(requestedBy);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/user/{requestedBy}/downloadable")
    @Operation(summary = "获取用户可下载的数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDownloadableByRequestedBy(@PathVariable Long requestedBy) {
        List<DataExport> dataExports = dataExportApplicationService.findDownloadableByRequestedBy(requestedBy);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/downloadable")
    @Operation(summary = "获取所有可下载的数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllDownloadable() {
        List<DataExport> dataExports = dataExportApplicationService.findAllDownloadable();
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/pending")
    @Operation(summary = "获取所有待处理的数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllPending() {
        List<DataExport> dataExports = dataExportApplicationService.findAllPending();
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/failed")
    @Operation(summary = "获取所有失败的数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllFailed() {
        List<DataExport> dataExports = dataExportApplicationService.findAllFailed();
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/retryable")
    @Operation(summary = "获取所有可重试的数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllRetryable() {
        List<DataExport> dataExports = dataExportApplicationService.findAllRetryable();
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "按状态获取数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByStatus(@PathVariable String status) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/user/{requestedBy}/status/{status}")
    @Operation(summary = "按用户和状态获取数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByRequestedByAndStatus(
            @PathVariable Long requestedBy,
            @PathVariable String status) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByRequestedByAndStatus(requestedBy, status);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/date-range")
    @Operation(summary = "按日期范围获取数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/search")
    @Operation(summary = "按名称搜索数据导出")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> searchDataExportsByName(@RequestParam String name) {
        List<DataExport> dataExports = dataExportApplicationService.searchDataExportsByName(name);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/count/user/{requestedBy}")
    @Operation(summary = "统计请求用户的数据导出数量")
    public ResponseEntity<ApiResponse<Long>> countDataExportsByRequestedBy(@PathVariable Long requestedBy) {
        long count = dataExportApplicationService.countDataExportsByRequestedBy(requestedBy);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "按状态统计数据导出数量")
    public ResponseEntity<ApiResponse<Long>> countDataExportsByStatus(@PathVariable String status) {
        long count = dataExportApplicationService.countDataExportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ==================== File Download ====================

    @GetMapping("/{id}/download")
    @Operation(summary = "下载导出文件")
    public ResponseEntity<byte[]> downloadExportedFile(@PathVariable Long id) throws IOException {
        Path filePath = exportService.getExportFilePath(id);
        byte[] fileContent = Files.readAllBytes(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileContent);
    }

    // ==================== Cleanup ====================

    @PostMapping("/cleanup")
    @Operation(summary = "清理过期的数据导出")
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
