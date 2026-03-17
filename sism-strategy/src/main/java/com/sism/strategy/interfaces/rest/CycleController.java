package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.strategy.application.CycleApplicationService;
import com.sism.strategy.domain.Cycle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cycles")
@RequiredArgsConstructor
@Tag(name = "Cycles", description = "Cycle management endpoints")
public class CycleController {

    private final CycleApplicationService cycleApplicationService;

    @GetMapping
    @Operation(summary = "Get all cycles with pagination")
    public ResponseEntity<ApiResponse<Page<Cycle>>> getAllCycles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer year) {
        if (status != null || year != null) {
            List<Cycle> cycles;
            if (status != null && year != null) {
                cycles = cycleApplicationService.getCyclesByStatusAndYear(status, year);
            } else if (status != null) {
                cycles = cycleApplicationService.getCyclesByStatus(status);
            } else {
                cycles = cycleApplicationService.getCyclesByYear(year);
            }
            return ResponseEntity.ok(ApiResponse.success(toPage(cycles, page, size)));
        }
        Page<Cycle> result = cycleApplicationService.getAllCycles(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/list")
    @Operation(summary = "Get all cycles list")
    public ResponseEntity<ApiResponse<List<Cycle>>> getAllCyclesList() {
        List<Cycle> cycles = cycleApplicationService.getAllCyclesList();
        return ResponseEntity.ok(ApiResponse.success(cycles));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cycle by ID")
    public ResponseEntity<ApiResponse<Cycle>> getCycleById(@PathVariable Long id) {
        Cycle cycle = cycleApplicationService.getCycleById(id);
        if (cycle == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Cycle not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @PostMapping
    @Operation(summary = "Create a new cycle")
    public ResponseEntity<ApiResponse<Cycle>> createCycle(@RequestBody CreateCycleRequest request) {
        Cycle cycle = cycleApplicationService.createCycle(
                request.getName(),
                request.getYear(),
                LocalDate.parse(request.getStartDate()),
                LocalDate.parse(request.getEndDate())
        );
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate cycle")
    public ResponseEntity<ApiResponse<Cycle>> activateCycle(@PathVariable Long id) {
        Cycle cycle = cycleApplicationService.activateCycle(id);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate cycle")
    public ResponseEntity<ApiResponse<Cycle>> deactivateCycle(@PathVariable Long id) {
        Cycle cycle = cycleApplicationService.deactivateCycle(id);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete cycle")
    public ResponseEntity<ApiResponse<Void>> deleteCycle(@PathVariable Long id) {
        cycleApplicationService.deleteCycle(id);
        return ResponseEntity.ok(ApiResponse.success("Cycle deleted successfully", null));
    }

    // ==================== Request DTOs ====================

    public static class CreateCycleRequest {
        private String name;
        private Integer year;
        private String startDate;
        private String endDate;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
    }

    private Page<Cycle> toPage(List<Cycle> cycles, int page, int size) {
        int start = Math.min(page * size, cycles.size());
        int end = Math.min(start + size, cycles.size());
        return new PageImpl<>(cycles.subList(start, end), PageRequest.of(page, size), cycles.size());
    }
}
