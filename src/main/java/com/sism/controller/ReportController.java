package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.dto.ApprovalRequest;
import com.sism.dto.ReportCreateRequest;
import com.sism.dto.ReportUpdateRequest;
import com.sism.entity.SysUser;
import com.sism.enums.ReportStatus;
import com.sism.exception.BusinessException;
import com.sism.repository.UserRepository;
import com.sism.service.ApprovalService;
import com.sism.service.ReportService;
import com.sism.vo.ApprovalRecordVO;
import com.sism.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Report Controller
 * Provides CRUD, submit, withdraw, and approval operations for progress reports
 * 
 * Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4
 */
@Slf4j
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Progress Reports", description = "Progress report management and approval endpoints")
public class ReportController {

    private final ReportService reportService;
    private final ApprovalService approvalService;
    private final UserRepository userRepository;

    /**
     * Get report by ID
     * GET /api/reports/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get report by ID", description = "Retrieve a specific progress report")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<ApiResponse<ReportVO>> getReportById(
            @Parameter(description = "Report ID") @PathVariable Long id) {
        ReportVO report = reportService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Get reports by indicator ID
     * GET /api/reports/indicator/{indicatorId}
     * Requirements: 3.5
     */
    @GetMapping("/indicator/{indicatorId}")
    @Operation(summary = "Get reports by indicator", description = "Retrieve all reports for a specific indicator")
    public ResponseEntity<ApiResponse<List<ReportVO>>> getReportsByIndicatorId(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId) {
        List<ReportVO> reports = reportService.getReportsByIndicatorId(indicatorId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * Get reports by indicator ID with pagination
     * GET /api/reports/indicator/{indicatorId}/page
     */
    @GetMapping("/indicator/{indicatorId}/page")
    @Operation(summary = "Get reports by indicator (paginated)", 
               description = "Retrieve reports for an indicator with pagination")
    public ResponseEntity<ApiResponse<PageResult<ReportVO>>> getReportsByIndicatorIdPaged(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportVO> reportPage = reportService.getReportsByIndicatorId(indicatorId, pageable);
        PageResult<ReportVO> result = new PageResult<>(
            reportPage.getContent(), 
            reportPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get reports by status
     * GET /api/reports/status/{status}
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get reports by status", description = "Retrieve reports with a specific status")
    public ResponseEntity<ApiResponse<List<ReportVO>>> getReportsByStatus(
            @Parameter(description = "Report status") @PathVariable ReportStatus status) {
        List<ReportVO> reports = reportService.getReportsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * Get reports for current user
     * GET /api/reports/my-reports
     */
    @GetMapping("/my-reports")
    @Operation(summary = "Get my reports", description = "Retrieve reports submitted by the current authenticated user")
    public ResponseEntity<ApiResponse<List<ReportVO>>> getMyReports() {
        Long currentUserId = getCurrentUserId();
        List<ReportVO> reports = reportService.getReportsByReporterId(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * Get pending approval reports
     * GET /api/reports/pending-approval
     * Requirements: 4.1
     */
    @GetMapping("/pending-approval")
    @Operation(summary = "Get pending approval reports", description = "Retrieve reports awaiting approval")
    public ResponseEntity<ApiResponse<List<ReportVO>>> getPendingApprovalReports() {
        List<ReportVO> reports = approvalService.getPendingApprovalReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * Get pending approval reports with pagination
     * GET /api/reports/pending-approval/page
     */
    @GetMapping("/pending-approval/page")
    @Operation(summary = "Get pending approval reports (paginated)", 
               description = "Retrieve reports awaiting approval with pagination")
    public ResponseEntity<ApiResponse<PageResult<ReportVO>>> getPendingApprovalReportsPaged(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportVO> reportPage = approvalService.getPendingApprovalReports(pageable);
        PageResult<ReportVO> result = new PageResult<>(
            reportPage.getContent(), 
            reportPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get approval records for a report
     * GET /api/reports/{id}/approval-records
     */
    @GetMapping("/{id}/approval-records")
    @Operation(summary = "Get approval records", description = "Retrieve approval history for a report")
    public ResponseEntity<ApiResponse<List<ApprovalRecordVO>>> getApprovalRecords(
            @Parameter(description = "Report ID") @PathVariable Long id) {
        List<ApprovalRecordVO> records = approvalService.getApprovalRecordsByReportId(id);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    /**
     * Create a new progress report
     * POST /api/reports
     * Requirements: 3.1
     */
    @PostMapping
    @Operation(summary = "Create report", description = "Create a new progress report in DRAFT status")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<ReportVO>> createReport(
            @Valid @RequestBody ReportCreateRequest request) {
        log.info("Creating report for indicator: {}", request.getIndicatorId());
        ReportVO report = reportService.createReport(request);
        return ResponseEntity.ok(ApiResponse.success("Report created successfully", report));
    }

    /**
     * Update an existing progress report
     * PUT /api/reports/{id}
     * Requirements: 3.1 - Only DRAFT and RETURNED status can be updated
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update report", description = "Update a progress report (only DRAFT/RETURNED status)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot update report in current status"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<ApiResponse<ReportVO>> updateReport(
            @Parameter(description = "Report ID") @PathVariable Long id,
            @Valid @RequestBody ReportUpdateRequest request) {
        log.info("Updating report: {}", id);
        ReportVO report = reportService.updateReport(id, request);
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", report));
    }

    /**
     * Submit a progress report
     * POST /api/reports/{id}/submit
     * Requirements: 3.2 - DRAFT → SUBMITTED
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit report", description = "Submit a draft report for approval")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report submitted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot submit report in current status")
    })
    public ResponseEntity<ApiResponse<ReportVO>> submitReport(
            @Parameter(description = "Report ID") @PathVariable Long id) {
        log.info("Submitting report: {}", id);
        ReportVO report = reportService.submitReport(id);
        return ResponseEntity.ok(ApiResponse.success("Report submitted successfully", report));
    }

    /**
     * Withdraw a submitted progress report
     * POST /api/reports/{id}/withdraw
     * Requirements: 3.3 - SUBMITTED → DRAFT
     */
    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw report", description = "Withdraw a submitted report back to draft")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report withdrawn"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot withdraw report in current status")
    })
    public ResponseEntity<ApiResponse<ReportVO>> withdrawReport(
            @Parameter(description = "Report ID") @PathVariable Long id) {
        log.info("Withdrawing report: {}", id);
        ReportVO report = reportService.withdrawReport(id);
        return ResponseEntity.ok(ApiResponse.success("Report withdrawn successfully", report));
    }

    /**
     * Process approval action on a report
     * POST /api/reports/approve
     * Requirements: 4.2, 4.3, 4.4
     */
    @PostMapping("/approve")
    @Operation(summary = "Process approval", description = "Approve, reject, or return a submitted report")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Approval processed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid approval request")
    })
    public ResponseEntity<ApiResponse<ReportVO>> processApproval(
            @Valid @RequestBody ApprovalRequest request) {
        log.info("Processing approval for report: {} with action: {}", request.getReportId(), request.getAction());
        Long approverId = getCurrentUserId();
        ReportVO report = approvalService.processApproval(request, approverId);
        return ResponseEntity.ok(ApiResponse.success("Approval processed successfully", report));
    }

    /**
     * Extract current user ID from security context
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String username) {
                    return userRepository.findByUsername(username)
                            .map(SysUser::getId)
                            .orElseThrow(() -> new BusinessException("User not found"));
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get current user ID: {}", e.getMessage(), e);
        }
        throw new BusinessException("User not authenticated");
    }
}
