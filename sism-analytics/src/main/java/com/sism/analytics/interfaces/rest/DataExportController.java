package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.DataExportApplicationService;
import com.sism.analytics.application.ExportService;
import com.sism.analytics.application.AnalyticsFileStorageService;
import com.sism.analytics.domain.DataExport;
import com.sism.analytics.interfaces.dto.CompleteDataExportRequest;
import com.sism.analytics.interfaces.dto.CreateDataExportRequest;
import com.sism.analytics.interfaces.dto.DataExportDTO;
import com.sism.common.PageResult;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
    private final AnalyticsFileStorageService analyticsFileStorageService;

    // ==================== Data Export Endpoints ====================

    @PostMapping
    @Operation(summary = "创建新的数据导出任务")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DataExportDTO>> createDataExport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateDataExportRequest request) {
        Long currentUserId = requireCurrentUserId(currentUser);
        DataExport dataExport = dataExportApplicationService.createDataExport(
                request.getName(),
                request.getType(),
                request.getFormat(),
                currentUserId,
                request.getParameters()
        );
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "开始处理数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DataExportDTO>> startProcessing(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        DataExport dataExport = dataExportApplicationService.startProcessing(id, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DataExportDTO>> completeDataExport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody CompleteDataExportRequest request) {
        Long currentUserId = requireCurrentUserId(currentUser);
        DataExport dataExport = dataExportApplicationService.completeDataExport(
                id,
                currentUserId,
                analyticsFileStorageService.prepareManagedExportFile(
                        dataExportApplicationService.findDataExportById(id, currentUserId)
                                .orElseThrow(() -> new AccessDeniedException("No permission to access export: " + id))
                ).toString(),
                request.getFileSize()
        );
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "标记数据导出为失败")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DataExportDTO>> failDataExport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        DataExport dataExport = dataExportApplicationService.failDataExport(
                id,
                requireCurrentUserId(currentUser),
                request.get("errorMessage"));
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "重试失败的数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DataExportDTO>> retryDataExport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        DataExport dataExport = dataExportApplicationService.retryDataExport(id, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteDataExport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        dataExportApplicationService.deleteDataExport(id, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DataExportDTO>> getDataExportById(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        return dataExportApplicationService.findDataExportById(id, requireCurrentUserId(currentUser))
                .map(dataExport -> ResponseEntity.ok(ApiResponse.success(toDataExportDTO(dataExport))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{requestedBy}")
    @Operation(summary = "根据请求用户获取数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByRequestedBy(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long requestedBy) {
        Long currentUserId = requireCurrentUserId(currentUser);
        ensureCurrentUserOwnsRequestedUser(currentUserId, requestedBy);
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByRequestedBy(requestedBy, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/user/{requestedBy}/downloadable")
    @Operation(summary = "获取用户可下载的数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDownloadableByRequestedBy(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long requestedBy) {
        Long currentUserId = requireCurrentUserId(currentUser);
        ensureCurrentUserOwnsRequestedUser(currentUserId, requestedBy);
        List<DataExport> dataExports = dataExportApplicationService.findDownloadableByRequestedBy(requestedBy, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/downloadable")
    @Operation(summary = "获取所有可下载的数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllDownloadable(
            @AuthenticationPrincipal CurrentUser currentUser) {
        List<DataExport> dataExports = dataExportApplicationService.findAllDownloadable(requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/downloadable", params = {"pageNum", "pageSize"})
    @Operation(summary = "分页获取所有可下载的数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DataExportDTO>>> getAllDownloadablePage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        var page = dataExportApplicationService.findAllDownloadable(requireCurrentUserId(currentUser), pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toDataExportDTO))));
    }

    @GetMapping("/pending")
    @Operation(summary = "获取所有待处理的数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllPending(
            @AuthenticationPrincipal CurrentUser currentUser) {
        List<DataExport> dataExports = dataExportApplicationService.findAllPending(requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/failed")
    @Operation(summary = "获取所有失败的数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllFailed(
            @AuthenticationPrincipal CurrentUser currentUser) {
        List<DataExport> dataExports = dataExportApplicationService.findAllFailed(requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/failed", params = {"pageNum", "pageSize"})
    @Operation(summary = "分页获取所有失败的数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DataExportDTO>>> getAllFailedPage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        var page = dataExportApplicationService.findAllFailed(requireCurrentUserId(currentUser), pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toDataExportDTO))));
    }

    @GetMapping("/retryable")
    @Operation(summary = "获取所有可重试的数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getAllRetryable(
            @AuthenticationPrincipal CurrentUser currentUser) {
        List<DataExport> dataExports = dataExportApplicationService.findAllRetryable(requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "按状态获取数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String status) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByStatus(status, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/status/{status}", params = {"pageNum", "pageSize"})
    @Operation(summary = "分页按状态获取数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DataExportDTO>>> getDataExportsByStatusPage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        var page = dataExportApplicationService.findDataExportsByStatus(status, requireCurrentUserId(currentUser), pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toDataExportDTO))));
    }

    @GetMapping("/user/{requestedBy}/status/{status}")
    @Operation(summary = "按用户和状态获取数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByRequestedByAndStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long requestedBy,
            @PathVariable String status) {
        Long currentUserId = requireCurrentUserId(currentUser);
        ensureCurrentUserOwnsRequestedUser(currentUserId, requestedBy);
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByRequestedByAndStatus(requestedBy, status, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/date-range")
    @Operation(summary = "按日期范围获取数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> getDataExportsByDateRange(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<DataExport> dataExports = dataExportApplicationService.findDataExportsByDateRange(
                startDate,
                endDate,
                requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/search")
    @Operation(summary = "按名称搜索数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DataExportDTO>>> searchDataExportsByName(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam String name) {
        List<DataExport> dataExports = dataExportApplicationService.searchDataExportsByName(name, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(dataExports.stream().map(this::toDataExportDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/search", params = {"name", "pageNum", "pageSize"})
    @Operation(summary = "分页按名称搜索数据导出")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DataExportDTO>>> searchDataExportsByNamePage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam String name,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        var page = dataExportApplicationService.searchDataExportsByName(name, requireCurrentUserId(currentUser), pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toDataExportDTO))));
    }

    @GetMapping("/count/user/{requestedBy}")
    @Operation(summary = "统计请求用户的数据导出数量")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> countDataExportsByRequestedBy(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long requestedBy) {
        Long currentUserId = requireCurrentUserId(currentUser);
        ensureCurrentUserOwnsRequestedUser(currentUserId, requestedBy);
        long count = dataExportApplicationService.countDataExportsByRequestedBy(requestedBy, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "按状态统计数据导出数量")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> countDataExportsByStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String status) {
        long count = dataExportApplicationService.countDataExportsByStatus(status, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ==================== File Download ====================

    @GetMapping("/{id}/download")
    @Operation(summary = "下载导出文件")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadExportedFile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) throws IOException {
        Path filePath = exportService.getExportFilePath(id, requireCurrentUserId(currentUser));
        InputStreamResource resource = new InputStreamResource(Files.newInputStream(filePath));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName() + "\"")
                .body(resource);
    }

    // ==================== Cleanup ====================

    @PostMapping("/cleanup")
    @Operation(summary = "清理过期的数据导出")
    @PreAuthorize("hasRole('ADMIN')")
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
                .filePath(sanitizeFilePath(dataExport.getFilePath()))
                .fileSize(dataExport.getFileSize())
                .requestedBy(dataExport.getRequestedBy())
                .requestedAt(dataExport.getRequestedAt())
                .startedAt(dataExport.getStartedAt())
                .completedAt(dataExport.getCompletedAt())
                .errorMessage(sanitizeErrorMessage(dataExport.getErrorMessage()))
                .parameters(dataExport.getParameters())
                .createdAt(dataExport.getCreatedAt())
                .updatedAt(dataExport.getUpdatedAt())
                .build();
    }

    private String sanitizeFilePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        try {
            return Path.of(filePath).getFileName().toString();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String sanitizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        String compact = errorMessage.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (compact.contains("/") || compact.contains("\\") || compact.contains("Exception") || compact.length() > 200) {
            return "导出失败，请稍后重试或联系管理员";
        }
        return compact;
    }

    private Long requireCurrentUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getId() <= 0) {
            throw new AccessDeniedException("当前用户未登录或无效");
        }
        return currentUser.getId();
    }

    private void ensureCurrentUserOwnsRequestedUser(Long currentUserId, Long requestedUserId) {
        if (requestedUserId == null || requestedUserId <= 0) {
            throw new IllegalArgumentException("用户ID必须为正数");
        }
        if (!currentUserId.equals(requestedUserId)) {
            throw new AccessDeniedException("不能操作其他用户的数据导出");
        }
    }
}
