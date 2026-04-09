package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.ReportApplicationService;
import com.sism.analytics.domain.Report;
import com.sism.analytics.interfaces.dto.CreateReportRequest;
import com.sism.analytics.interfaces.dto.GenerateReportRequest;
import com.sism.analytics.interfaces.dto.ReportDTO;
import com.sism.analytics.interfaces.dto.UpdateReportRequest;
import com.sism.common.PageResult;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReportController - 分析报告API控制器
 * 提供分析报告管理相关的REST API端点
 */
@RestController("analyticsReportController")
@RequestMapping("/api/v1/analytics/reports")
@RequiredArgsConstructor
@Tag(name = "分析报告", description = "分析报告管理接口")
public class ReportController {

    private final ReportApplicationService reportApplicationService;

    // ==================== Report Endpoints ====================

    @PostMapping
    @Operation(summary = "创建新报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportDTO>> createReport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateReportRequest request) {
        Long currentUserId = requireCurrentUserId(currentUser);
        Report report = reportApplicationService.createReport(
                request.getName(),
                request.getType(),
                request.getFormat(),
                currentUserId,
                request.getParameters(),
                request.getDescription()
        );
        return ResponseEntity.ok(ApiResponse.success(toReportDTO(report)));
    }

    @PostMapping("/{id}/generate")
    @Operation(summary = "生成报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportDTO>> generateReport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody GenerateReportRequest request) {
        Report report = reportApplicationService.generateReport(
                id,
                requireCurrentUserId(currentUser),
                request.getFilePath(),
                request.getFileSize()
        );
        return ResponseEntity.ok(ApiResponse.success(toReportDTO(report)));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "标记报告生成为失败")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportDTO>> failReport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Report report = reportApplicationService.failReport(id, requireCurrentUserId(currentUser), request.get("errorMessage"));
        return ResponseEntity.ok(ApiResponse.success(toReportDTO(report)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新报告信息")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportDTO>> updateReport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportRequest request) {
        Report report = reportApplicationService.updateReport(
                id,
                requireCurrentUserId(currentUser),
                request.getName(),
                request.getType(),
                request.getFormat(),
                request.getDescription()
        );
        return ResponseEntity.ok(ApiResponse.success(toReportDTO(report)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        reportApplicationService.deleteReport(id, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportDTO>> getReportById(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        return reportApplicationService.findReportById(id, requireCurrentUserId(currentUser))
                .map(report -> ResponseEntity.ok(ApiResponse.success(toReportDTO(report))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{generatedBy}")
    @Operation(summary = "根据生成用户获取报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReportsByGeneratedBy(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long generatedBy) {
        Long currentUserId = requireCurrentUserId(currentUser);
        ensureCurrentUserOwnsRequestedUser(currentUserId, generatedBy);
        List<Report> reports = reportApplicationService.findReportsByGeneratedBy(generatedBy, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/generated")
    @Operation(summary = "获取所有已生成的报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getAllGeneratedReports(
            @AuthenticationPrincipal CurrentUser currentUser) {
        List<Report> reports = reportApplicationService.findAllGeneratedReports(requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/generated", params = {"pageNum", "pageSize"})
    @Operation(summary = "分页获取所有已生成的报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<ReportDTO>>> getAllGeneratedReportsPage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        var page = reportApplicationService.findAllGeneratedReports(requireCurrentUserId(currentUser), pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toReportDTO))));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "按类型获取报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReportsByType(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String type) {
        List<Report> reports = reportApplicationService.findReportsByType(type, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "按状态获取报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReportsByStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String status) {
        List<Report> reports = reportApplicationService.findReportsByStatus(status, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/status/{status}", params = {"pageNum", "pageSize"})
    @Operation(summary = "分页按状态获取报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<ReportDTO>>> getReportsByStatusPage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        var page = reportApplicationService.findReportsByStatus(status, requireCurrentUserId(currentUser), pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toReportDTO))));
    }

    @GetMapping("/date-range")
    @Operation(summary = "按日期范围获取报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReportsByDateRange(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Report> reports = reportApplicationService.findReportsByDateRange(
                startDate,
                endDate,
                requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/search")
    @Operation(summary = "按名称搜索报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> searchReportsByName(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam String name) {
        List<Report> reports = reportApplicationService.searchReportsByName(name, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/search", params = {"name", "pageNum", "pageSize"})
    @Operation(summary = "分页按名称搜索报告")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<ReportDTO>>> searchReportsByNamePage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam String name,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        var page = reportApplicationService.searchReportsByName(name, requireCurrentUserId(currentUser), pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toReportDTO))));
    }

    @GetMapping("/count/user/{generatedBy}")
    @Operation(summary = "统计生成用户的报告数量")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> countReportsByGeneratedBy(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long generatedBy) {
        Long currentUserId = requireCurrentUserId(currentUser);
        ensureCurrentUserOwnsRequestedUser(currentUserId, generatedBy);
        long count = reportApplicationService.countReportsByGeneratedBy(generatedBy, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "按状态统计报告数量")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> countReportsByStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String status) {
        long count = reportApplicationService.countReportsByStatus(status, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 将 Report 实体转换为 DTO
     */
    private ReportDTO toReportDTO(Report report) {
        return ReportDTO.builder()
                .id(report.getId())
                .name(report.getName())
                .type(report.getType())
                .format(report.getFormat())
                .status(report.getStatus())
                .filePath(report.getFilePath())
                .fileSize(report.getFileSize())
                .generatedBy(report.getGeneratedBy())
                .generatedAt(report.getGeneratedAt())
                .parameters(report.getParameters())
                .description(report.getDescription())
                .errorMessage(report.getErrorMessage())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
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
            throw new AccessDeniedException("不能操作其他用户的报告");
        }
    }
}
