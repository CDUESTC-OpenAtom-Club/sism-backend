package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.service.AssessmentCycleService;
import com.sism.vo.AssessmentCycleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Assessment Cycle Controller
 * Provides REST API endpoints for assessment cycle management
 */
@Slf4j
@RestController
@RequestMapping("/cycles")
@RequiredArgsConstructor
@Tag(name = "Assessment Cycle", description = "Assessment cycle management APIs")
public class AssessmentCycleController {

    private final AssessmentCycleService assessmentCycleService;

    /**
     * Get all assessment cycles
     */
    @GetMapping
    @Operation(summary = "Get all assessment cycles", description = "Retrieve all assessment cycles ordered by year descending")
    public ResponseEntity<ApiResponse<List<AssessmentCycleVO>>> getAllCycles() {
        log.info("GET /cycles - Getting all assessment cycles");
        List<AssessmentCycleVO> cycles = assessmentCycleService.getAllCycles();
        return ResponseEntity.ok(ApiResponse.success(cycles));
    }

    /**
     * Get assessment cycle by ID
     */
    @GetMapping("/{cycleId}")
    @Operation(summary = "Get assessment cycle by ID", description = "Retrieve a specific assessment cycle by its ID")
    public ResponseEntity<ApiResponse<AssessmentCycleVO>> getCycleById(
            @Parameter(description = "Assessment cycle ID") @PathVariable Long cycleId) {
        log.info("GET /cycles/{} - Getting assessment cycle by ID", cycleId);
        AssessmentCycleVO cycle = assessmentCycleService.getCycleById(cycleId);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    /**
     * Get assessment cycle by year
     */
    @GetMapping("/year/{year}")
    @Operation(summary = "Get assessment cycle by year", description = "Retrieve assessment cycle for a specific year")
    public ResponseEntity<ApiResponse<AssessmentCycleVO>> getCycleByYear(
            @Parameter(description = "Year") @PathVariable Integer year) {
        log.info("GET /cycles/year/{} - Getting assessment cycle by year", year);
        AssessmentCycleVO cycle = assessmentCycleService.getCycleByYear(year);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    /**
     * Get active or future assessment cycles
     */
    @GetMapping("/active")
    @Operation(summary = "Get active or future cycles", description = "Retrieve assessment cycles that are currently active or will be active in the future")
    public ResponseEntity<ApiResponse<List<AssessmentCycleVO>>> getActiveOrFutureCycles() {
        log.info("GET /cycles/active - Getting active or future assessment cycles");
        List<AssessmentCycleVO> cycles = assessmentCycleService.getActiveOrFutureCycles();
        return ResponseEntity.ok(ApiResponse.success(cycles));
    }

    /**
     * Get assessment cycle by date
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "Get cycle by date", description = "Retrieve assessment cycle that contains a specific date")
    public ResponseEntity<ApiResponse<AssessmentCycleVO>> getCycleByDate(
            @Parameter(description = "Date in ISO format (yyyy-MM-dd)") @PathVariable String date) {
        log.info("GET /cycles/date/{} - Getting assessment cycle by date", date);
        LocalDate localDate = LocalDate.parse(date);
        AssessmentCycleVO cycle = assessmentCycleService.getCycleByDate(localDate);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }
}
