package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.strategy.application.StrategyApplicationService;
import com.sism.strategy.domain.Indicator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/indicators")
@RequiredArgsConstructor
@Tag(name = "Indicators", description = "Indicator management endpoints")
public class IndicatorController {

    private final StrategyApplicationService strategyApplicationService;

    @GetMapping
    @Operation(summary = "Get all indicators with pagination")
    public ResponseEntity<ApiResponse<PageResult<IndicatorResponse>>> listIndicators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long cycleId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Indicator> indicatorPage;

        if (status != null) {
            // This would need to be implemented in service
            List<Indicator> indicators = strategyApplicationService.getAllIndicators().stream()
                    .filter(i -> i.getStatus() != null && i.getStatus().toString().equals(status))
                    .toList();

            // Convert list to page (simplified)
            int start = Math.min((int)pageable.getOffset(), indicators.size());
            int end = Math.min(start + pageable.getPageSize(), indicators.size());
            indicatorPage = new org.springframework.data.domain.PageImpl<>(
                    indicators.subList(start, end), pageable, indicators.size());
        } else {
            List<Indicator> allIndicators = strategyApplicationService.getAllIndicators();
            int start = Math.min((int)pageable.getOffset(), allIndicators.size());
            int end = Math.min(start + pageable.getPageSize(), allIndicators.size());
            indicatorPage = new org.springframework.data.domain.PageImpl<>(
                    allIndicators.subList(start, end), pageable, allIndicators.size());
        }
        PageResult<IndicatorResponse> result = PageResult.of(
                indicatorPage.map(this::toIndicatorResponse)
                        .stream()
                        .toList(),
                (int) indicatorPage.getTotalElements(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get indicator by ID")
    public ResponseEntity<ApiResponse<IndicatorResponse>> getIndicatorById(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(indicator)));
    }

    @PostMapping
    @Operation(summary = "Create a new indicator")
    public ResponseEntity<ApiResponse<IndicatorResponse>> createIndicator(
            @Valid @RequestBody CreateIndicatorRequest request) {
        // Note: This would need proper implementation with SysOrg lookup
        // For now, we'll create a simplified version
        Indicator created = strategyApplicationService.createIndicator(
                request.getDescription() != null ? request.getDescription() : request.getIndicatorName(),
                null,  // ownerOrg would need to be looked up
                null   // targetOrg would need to be looked up
        );
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(created)));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit indicator for review")
    public ResponseEntity<ApiResponse<IndicatorResponse>> submitForReview(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        Indicator submitted = strategyApplicationService.submitIndicatorForReview(indicator);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(submitted)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve indicator")
    public ResponseEntity<ApiResponse<IndicatorResponse>> approveIndicator(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        Indicator approved = strategyApplicationService.approveIndicator(indicator);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(approved)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject indicator")
    public ResponseEntity<ApiResponse<IndicatorResponse>> rejectIndicator(
            @PathVariable Long id,
            @RequestBody RejectRequest request) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        // The current service doesn't accept a reason, we'll use a simple version
        Indicator rejected = strategyApplicationService.rejectIndicator(indicator);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(rejected)));
    }

    @PostMapping("/{id}/distribute")
    @Operation(summary = "Distribute indicator to target organization")
    public ResponseEntity<ApiResponse<IndicatorResponse>> distributeIndicator(@PathVariable Long id) {
        Indicator distributed = strategyApplicationService.distributeIndicator(id);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(distributed)));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw distributed indicator")
    public ResponseEntity<ApiResponse<IndicatorResponse>> withdrawIndicator(
            @PathVariable Long id,
            @RequestBody WithdrawRequest request) {
        Indicator withdrawn = strategyApplicationService.withdrawIndicator(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(withdrawn)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search indicators by keyword")
    public ResponseEntity<ApiResponse<List<IndicatorResponse>>> searchIndicators(
            @RequestParam String keyword) {
        List<Indicator> result = strategyApplicationService.searchIndicators(keyword);
        List<IndicatorResponse> responses = result.stream()
                .map(this::toIndicatorResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get indicators by task ID")
    public ResponseEntity<ApiResponse<List<IndicatorResponse>>> getIndicatorsByTaskId(@PathVariable Long taskId) {
        List<Indicator> indicators = strategyApplicationService.getIndicatorsByTaskId(taskId);
        List<IndicatorResponse> responses = indicators.stream()
                .map(this::toIndicatorResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}/distribution-status")
    @Operation(summary = "Get indicator distribution status")
    public ResponseEntity<ApiResponse<String>> getDistributionStatus(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(indicator.getDistributionStatus().toString()));
    }

    @PostMapping("/{id}/breakdown")
    @Operation(summary = "Break down indicator into child indicators")
    public ResponseEntity<ApiResponse<IndicatorResponse>> breakdownIndicator(@PathVariable Long id) {
        Indicator brokenDown = strategyApplicationService.breakdownIndicator(id);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(brokenDown)));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate indicator")
    public ResponseEntity<ApiResponse<IndicatorResponse>> activateIndicator(@PathVariable Long id) {
        Indicator activated = strategyApplicationService.activateIndicator(id);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(activated)));
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate indicator")
    public ResponseEntity<ApiResponse<IndicatorResponse>> terminateIndicator(
            @PathVariable Long id,
            @RequestBody TerminateRequest request) {
        Indicator terminated = strategyApplicationService.terminateIndicator(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(terminated)));
    }

    // ==================== Helper Methods ====================

    private IndicatorResponse toIndicatorResponse(Indicator indicator) {
        IndicatorResponse response = new IndicatorResponse();
        response.setId(indicator.getId());
        response.setIndicatorDesc(indicator.getIndicatorDesc());
        response.setIndicatorName(indicator.getIndicatorDesc()); // Using desc as name for now
        response.setWeightPercent(indicator.getWeightPercent());
        response.setStatus(indicator.getStatus() != null ? indicator.getStatus().toString() : null);
        response.setLevel(indicator.getLevel() != null ? indicator.getLevel().toString() : null);
        response.setProgress(indicator.getProgress());
        response.setCreatedAt(indicator.getCreatedAt());
        response.setUpdatedAt(indicator.getUpdatedAt());
        if (indicator.getOwnerOrg() != null) {
            response.setOwnerOrgId(indicator.getOwnerOrg().getId());
        }
        if (indicator.getTargetOrg() != null) {
            response.setTargetOrgId(indicator.getTargetOrg().getId());
        }
        return response;
    }

    // ==================== Request/Response DTOs ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorResponse {
        private Long id;
        private String indicatorName;
        private String indicatorCode;
        private String indicatorDesc;
        private Long cycleId;
        private Long ownerOrgId;
        private Long targetOrgId;
        private String departmentName;
        private BigDecimal targetValue;
        private String unit;
        private String status;
        private String dimension;
        private BigDecimal weightPercent;
        private String level;
        private Integer progress;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }

    @Data
    public static class CreateIndicatorRequest {
        @NotBlank(message = "Indicator name is required")
        private String indicatorName;

        @NotBlank(message = "Indicator code is required")
        private String indicatorCode;

        private String description;

        @NotNull(message = "Cycle ID is required")
        private Long cycleId;

        @NotNull(message = "Department ID is required")
        private Long departmentId;

        @NotNull(message = "Target value is required")
        @DecimalMin(value = "0", message = "Target value must be positive")
        @DecimalMax(value = "100", message = "Target value cannot exceed 100")
        private BigDecimal targetValue;

        private String unit;
        private String dimension; // FINANCIAL, OPERATION, etc.
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }

    @Data
    public static class WithdrawRequest {
        private String reason;
    }

    @Data
    public static class TerminateRequest {
        private String reason;
    }
}
