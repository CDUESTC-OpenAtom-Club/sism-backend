package com.sism.execution.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.execution.application.ReportApplicationService;
import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ReportController - 计划报告API控制器
 * 提供计划报告管理相关的REST API端点
 */
@RestController("executionReportController")
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Plan Reports", description = "Plan report management endpoints")
public class ReportController {

    private final ReportApplicationService reportApplicationService;

    // ==================== Report CRUD Operations ====================

    @PostMapping
    @Operation(summary = "创建月度报告（草稿）", description = "创建一个新的月度进度报告，初始状态为草稿")
    public ResponseEntity<ApiResponse<PlanReportResponse>> createReport(
            @Valid @RequestBody CreatePlanReportRequest request) {
        PlanReport report = reportApplicationService.createReport(
                request.getReportMonth(),
                request.getReportOrgId(),
                request.getReportOrgType(),
                request.getPlanId(),
                request.getCreatedBy()
        );
        return ResponseEntity.ok(ApiResponse.success(PlanReportResponse.fromEntity(report)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新报告内容", description = "更新报告的内容、摘要、进度、问题和下一步计划等信息")
    public ResponseEntity<ApiResponse<PlanReportResponse>> updateReport(
            @Parameter(description = "报告ID") @PathVariable Long id,
            @Valid @RequestBody UpdatePlanReportRequest request) {
        PlanReport report = reportApplicationService.updateReport(
                id,
                request.getTitle(),
                request.getIndicatorId(),
                request.getContent(),
                request.getSummary(),
                request.getProgress(),
                request.getIssues(),
                request.getNextPlan(),
                request.getMilestoneNote(),
                request.getOperatorUserId()
        );
        return ResponseEntity.ok(ApiResponse.success(PlanReportResponse.fromEntity(report)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询报告详情", description = "获取指定报告的完整信息")
    public ResponseEntity<ApiResponse<PlanReportResponse>> getReportById(
            @Parameter(description = "报告ID") @PathVariable Long id) {
        PlanReport report = reportApplicationService.findReportById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        return ResponseEntity.ok(ApiResponse.success(PlanReportResponse.fromEntity(report)));
    }

    // ==================== Report Action Operations ====================

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交报告", description = "将草稿状态的报告提交审批")
    public ResponseEntity<ApiResponse<PlanReportResponse>> submitReport(
            @Parameter(description = "报告ID") @PathVariable Long id,
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        PlanReport report = reportApplicationService.submitReport(id, userId);
        return ResponseEntity.ok(ApiResponse.success(PlanReportResponse.fromEntity(report)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过报告", description = "审批通过已提交的报告")
    public ResponseEntity<ApiResponse<PlanReportResponse>> approveReport(
            @Parameter(description = "报告ID") @PathVariable Long id,
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        PlanReport report = reportApplicationService.approveReport(id, userId);
        return ResponseEntity.ok(ApiResponse.success(PlanReportResponse.fromEntity(report)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "驳回报告", description = "驳回已提交的报告，并提供驳回理由")
    public ResponseEntity<ApiResponse<PlanReportResponse>> rejectReport(
            @Parameter(description = "报告ID") @PathVariable Long id,
            @Valid @RequestBody RejectPlanReportRequest request) {
        PlanReport report = reportApplicationService.rejectReport(id, request.getUserId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success(PlanReportResponse.fromEntity(report)));
    }

    // ==================== Report Query Operations ====================

    @GetMapping
    @Operation(summary = "分页查询所有报告", description = "获取所有有效的报告，支持分页")
    public ResponseEntity<ApiResponse<PageResult<PlanReportSimpleResponse>>> getAllReports(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        Page<PlanReport> reportPage = reportApplicationService.findAllActiveReports(page, size);
        List<PlanReportSimpleResponse> responses = reportPage.getContent().stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        PageResult<PlanReportSimpleResponse> pageResult = PageResult.of(
                responses,
                reportPage.getTotalElements(),
                reportPage.getNumber(),
                reportPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }

    @GetMapping("/search")
    @Operation(summary = "多条件查询报告", description = "根据多种条件组合查询报告，支持分页")
    public ResponseEntity<ApiResponse<PageResult<PlanReportSimpleResponse>>> searchReports(
            @Valid PlanReportQueryRequest queryRequest) {
        Page<PlanReport> reportPage = reportApplicationService.findReportsByConditions(queryRequest);
        List<PlanReportSimpleResponse> responses = reportPage.getContent().stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        PageResult<PlanReportSimpleResponse> pageResult = PageResult.of(
                responses,
                reportPage.getTotalElements(),
                reportPage.getNumber(),
                reportPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }

    @GetMapping("/org/{orgId}")
    @Operation(summary = "根据组织ID查询报告（列表）", description = "获取指定组织的所有报告列表")
    public ResponseEntity<ApiResponse<List<PlanReportSimpleResponse>>> getReportsByOrgId(
            @Parameter(description = "组织ID") @PathVariable Long orgId) {
        List<PlanReport> reports = reportApplicationService.findReportsByOrgId(orgId);
        List<PlanReportSimpleResponse> responses = reports.stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/org/{orgId}/page")
    @Operation(summary = "根据组织ID分页查询报告", description = "获取指定组织的报告，支持分页")
    public ResponseEntity<ApiResponse<PageResult<PlanReportSimpleResponse>>> getReportsByOrgIdPaginated(
            @Parameter(description = "组织ID") @PathVariable Long orgId,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        Page<PlanReport> reportPage = reportApplicationService.findReportsByOrgId(orgId, page, size);
        List<PlanReportSimpleResponse> responses = reportPage.getContent().stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        PageResult<PlanReportSimpleResponse> pageResult = PageResult.of(
                responses,
                reportPage.getTotalElements(),
                reportPage.getNumber(),
                reportPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }

    @GetMapping("/month/{month}")
    @Operation(summary = "根据月份查询报告", description = "获取指定月份的所有报告")
    public ResponseEntity<ApiResponse<List<PlanReportSimpleResponse>>> getReportsByMonth(
            @Parameter(description = "月份，格式：yyyy-MM") @PathVariable String month) {
        List<PlanReport> reports = reportApplicationService.findReportsByMonth(month);
        List<PlanReportSimpleResponse> responses = reports.stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "根据状态查询报告（列表）", description = "获取指定状态的所有报告列表")
    public ResponseEntity<ApiResponse<List<PlanReportSimpleResponse>>> getReportsByStatus(
            @Parameter(description = "报告状态：DRAFT/SUBMITTED/APPROVED/REJECTED") @PathVariable String status) {
        List<PlanReport> reports = reportApplicationService.findReportsByStatus(status);
        List<PlanReportSimpleResponse> responses = reports.stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}/page")
    @Operation(summary = "根据状态分页查询报告", description = "获取指定状态的报告，支持分页")
    public ResponseEntity<ApiResponse<PageResult<PlanReportSimpleResponse>>> getReportsByStatusPaginated(
            @Parameter(description = "报告状态：DRAFT/SUBMITTED/APPROVED/REJECTED") @PathVariable String status,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        Page<PlanReport> reportPage = reportApplicationService.findReportsByStatus(status, page, size);
        List<PlanReportSimpleResponse> responses = reportPage.getContent().stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        PageResult<PlanReportSimpleResponse> pageResult = PageResult.of(
                responses,
                reportPage.getTotalElements(),
                reportPage.getNumber(),
                reportPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }

    @GetMapping("/pending")
    @Operation(summary = "查询待审批的报告", description = "获取所有状态为SUBMITTED的待审批报告")
    public ResponseEntity<ApiResponse<List<PlanReportSimpleResponse>>> getPendingReports() {
        List<PlanReport> reports = reportApplicationService.findPendingReports();
        List<PlanReportSimpleResponse> responses = reports.stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/org-type/{orgType}")
    @Operation(summary = "根据组织类型查询报告", description = "获取指定组织类型的所有报告")
    public ResponseEntity<ApiResponse<List<PlanReportSimpleResponse>>> getReportsByOrgType(
            @Parameter(description = "组织类型：ADMIN/FUNCTIONAL/ACADEMIC") @PathVariable ReportOrgType orgType) {
        List<PlanReport> reports = reportApplicationService.findReportsByOrgType(orgType);
        List<PlanReportSimpleResponse> responses = reports.stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/plan/{planId}")
    @Operation(summary = "根据计划ID查询报告", description = "获取关联到指定计划的所有报告")
    public ResponseEntity<ApiResponse<List<PlanReportSimpleResponse>>> getReportsByPlanId(
            @Parameter(description = "计划ID") @PathVariable Long planId) {
        List<PlanReport> reports = reportApplicationService.findReportsByPlanId(planId);
        List<PlanReportSimpleResponse> responses = reports.stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "统计指定状态的报告数量", description = "获取指定状态的报告总数")
    public ResponseEntity<ApiResponse<Long>> countReportsByStatus(
            @Parameter(description = "报告状态") @PathVariable String status) {
        long count = reportApplicationService.countReportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/month/{month}/org/{orgId}")
    @Operation(summary = "根据月份和组织ID查询报告", description = "获取指定月份和组织的所有报告")
    public ResponseEntity<ApiResponse<List<PlanReportSimpleResponse>>> getReportsByMonthAndOrgId(
            @Parameter(description = "月份，格式：yyyy-MM") @PathVariable String month,
            @Parameter(description = "组织ID") @PathVariable Long orgId) {
        List<PlanReport> reports = reportApplicationService.findReportsByMonthAndOrgId(month, orgId);
        List<PlanReportSimpleResponse> responses = reports.stream()
                .map(PlanReportSimpleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
