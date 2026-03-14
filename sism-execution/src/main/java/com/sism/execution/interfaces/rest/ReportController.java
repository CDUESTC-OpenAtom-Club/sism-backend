package com.sism.execution.interfaces.rest;

import com.sism.execution.application.ReportApplicationService;
import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ReportController - 计划报告API控制器
 * 提供计划报告管理相关的REST API端点
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Plan Reports", description = "Plan report management endpoints")
public class ReportController {

    private final ReportApplicationService reportApplicationService;

    // ==================== Report CRUD ====================

    @PostMapping
    @Operation(summary = "创建月度报告（草稿）")
    public ResponseEntity<ApiResponse<PlanReport>> createReport(@RequestBody CreateReportRequest request) {
        PlanReport report = reportApplicationService.createReport(
                request.getReportMonth(),
                request.getReportOrgId(),
                request.getReportOrgName(),
                request.getReportOrgType(),
                request.getPlanId()
        );
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新报告内容")
    public ResponseEntity<ApiResponse<PlanReport>> updateReport(
            @PathVariable Long id,
            @RequestBody UpdateReportRequest request) {
        PlanReport report = reportApplicationService.updateReport(
                id,
                request.getContent(),
                request.getSummary(),
                request.getProgress(),
                request.getIssues(),
                request.getNextPlan()
        );
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报告")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        reportApplicationService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询报告")
    public ResponseEntity<ApiResponse<PlanReport>> getReportById(@PathVariable Long id) {
        return reportApplicationService.findReportById(report -> ResponseEntity.ok(ApiResponse.success(report)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Report Actions ====================

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交报告")
    public ResponseEntity<ApiResponse<PlanReport>> submitReport(
            @PathVariable Long id,
            @RequestBody SubmitReportRequest request) {
        PlanReport report = reportApplicationService.submitReport(id, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过报告")
    public ResponseEntity<ApiResponse<PlanReport>> approveReport(
            @PathVariable Long id,
            @RequestBody ApproveReportRequest request) {
        PlanReport report = reportApplicationService.approveReport(id, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "驳回报告")
    public ResponseEntity<ApiResponse<PlanReport>> rejectReport(
            @PathVariable Long id,
            @RequestBody RejectReportRequest request) {
        PlanReport report = reportApplicationService.rejectReport(id, request.getUserId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // ==================== Report Queries ====================

    @GetMapping("/org/{orgId}")
    @Operation(summary = "根据组织ID查询报告")
    public ResponseEntity<ApiResponse<List<PlanReport>>> getReportsByOrgId(@PathVariable Long orgId) {
        List<PlanReport> reports = reportApplicationService.findReportsByOrgId(orgId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/month/{month}")
    @Operation(summary = "根据月份查询报告")
    public ResponseEntity<ApiResponse<List<PlanReport>>> getReportsByMonth(@PathVariable String month) {
        List<PlanReport> reports = reportApplicationService.findReportsByMonth(month);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "根据状态查询报告")
    public ResponseEntity<ApiResponse<List<PlanReport>>> getReportsByStatus(@PathVariable String status) {
        List<PlanReport> reports = reportApplicationService.findReportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/pending")
    @Operation(summary = "查询待审批的报告")
    public ResponseEntity<ApiResponse<List<PlanReport>>> getPendingReports() {
        List<PlanReport> reports = reportApplicationService.findPendingReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/org-type/{orgType}")
    @Operation(summary = "根据组织类型查询报告")
    public ResponseEntity<ApiResponse<List<PlanReport>>> getReportsByOrgType(@PathVariable ReportOrgType orgType) {
        List<PlanReport> reports = reportApplicationService.findReportsByOrgType(orgType);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    // ==================== Request DTOs ====================

    @lombok.Data
    public static class CreateReportRequest {
        private String reportMonth;
        private Long reportOrgId;
        private String reportOrgName;
        private ReportOrgType reportOrgType;
        private Long planId;
    }

    @lombok.Data
    public static class UpdateReportRequest {
        private String content;
        private String summary;
        private Integer progress;
        private String issues;
        private String nextPlan;
    }

    @lombok.Data
    public static class SubmitReportRequest {
        private Long userId;
    }

    @lombok.Data
    public static class ApproveReportRequest {
        private Long userId;
    }

    @lombok.Data
    public static class RejectReportRequest {
        private Long userId;
        private String reason;
    }
}
