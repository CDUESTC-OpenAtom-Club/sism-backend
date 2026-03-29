package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.ReportApplicationService;
import com.sism.analytics.domain.Report;
import com.sism.analytics.interfaces.dto.CreateReportRequest;
import com.sism.analytics.interfaces.dto.GenerateReportRequest;
import com.sism.analytics.interfaces.dto.ReportDTO;
import com.sism.analytics.interfaces.dto.UpdateReportRequest;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<ReportDTO>> createReport(@RequestBody CreateReportRequest request) {
        Report report = reportApplicationService.createReport(
                request.getName(),
                request.getType(),
                request.getFormat(),
                request.getGeneratedBy(),
                request.getParameters(),
                request.getDescription()
        );
        return ResponseEntity.ok(ApiResponse.success(toReportDTO(report)));
    }

    @PostMapping("/{id}/generate")
    @Operation(summary = "生成报告")
    public ResponseEntity<ApiResponse<ReportDTO>> generateReport(
            @PathVariable Long id,
            @RequestBody GenerateReportRequest request) {
        Report report = reportApplicationService.generateReport(
                id,
                request.getFilePath(),
                request.getFileSize()
        );
        return ResponseEntity.ok(ApiResponse.success(toReportDTO(report)));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "标记报告生成为失败")
    public ResponseEntity<ApiResponse<ReportDTO>> failReport(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Report report = reportApplicationService.failReport(id, request.get("errorMessage"));
        return ResponseEntity.ok(ApiResponse.success(toReportDTO(report)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新报告信息")
    public ResponseEntity<ApiResponse<ReportDTO>> updateReport(
            @PathVariable Long id,
            @RequestBody UpdateReportRequest request) {
        Report report = reportApplicationService.updateReport(
                id,
                request.getName(),
                request.getType(),
                request.getFormat(),
                request.getDescription()
        );
        return ResponseEntity.ok(ApiResponse.success(toReportDTO(report)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报告")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        reportApplicationService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取报告")
    public ResponseEntity<ApiResponse<ReportDTO>> getReportById(@PathVariable Long id) {
        return reportApplicationService.findReportById(id)
                .map(report -> ResponseEntity.ok(ApiResponse.success(toReportDTO(report))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{generatedBy}")
    @Operation(summary = "根据生成用户获取报告")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReportsByGeneratedBy(@PathVariable Long generatedBy) {
        List<Report> reports = reportApplicationService.findReportsByGeneratedBy(generatedBy);
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/generated")
    @Operation(summary = "获取所有已生成的报告")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getAllGeneratedReports() {
        List<Report> reports = reportApplicationService.findAllGeneratedReports();
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "按类型获取报告")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReportsByType(@PathVariable String type) {
        List<Report> reports = reportApplicationService.findReportsByType(type);
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "按状态获取报告")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReportsByStatus(@PathVariable String status) {
        List<Report> reports = reportApplicationService.findReportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/date-range")
    @Operation(summary = "按日期范围获取报告")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReportsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Report> reports = reportApplicationService.findReportsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/search")
    @Operation(summary = "按名称搜索报告")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> searchReportsByName(@RequestParam String name) {
        List<Report> reports = reportApplicationService.searchReportsByName(name);
        return ResponseEntity.ok(ApiResponse.success(reports.stream().map(this::toReportDTO).collect(Collectors.toList())));
    }

    @GetMapping("/count/user/{generatedBy}")
    @Operation(summary = "统计生成用户的报告数量")
    public ResponseEntity<ApiResponse<Long>> countReportsByGeneratedBy(@PathVariable Long generatedBy) {
        long count = reportApplicationService.countReportsByGeneratedBy(generatedBy);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "按状态统计报告数量")
    public ResponseEntity<ApiResponse<Long>> countReportsByStatus(@PathVariable String status) {
        long count = reportApplicationService.countReportsByStatus(status);
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
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
