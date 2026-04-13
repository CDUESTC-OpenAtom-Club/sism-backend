package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.strategy.application.CycleApplicationService;
import com.sism.strategy.domain.Cycle;
import com.sism.strategy.interfaces.dto.CreateCycleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cycles")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "考核周期", description = "考核周期管理接口")
public class CycleController {

    private final CycleApplicationService cycleApplicationService;

    @GetMapping
    @Operation(summary = "分页获取所有考核周期")
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
    @Operation(summary = "获取所有考核周期列表")
    public ResponseEntity<ApiResponse<List<Cycle>>> getAllCyclesList() {
        List<Cycle> cycles = cycleApplicationService.getAllCyclesList();
        return ResponseEntity.ok(ApiResponse.success(cycles));
    }

    @GetMapping("/years")
    @Operation(summary = "获取年份选择器的所有可用年份")
    public ResponseEntity<ApiResponse<List<Integer>>> getAvailableYears() {
        List<Integer> years = cycleApplicationService.getAvailableYears();
        return ResponseEntity.ok(ApiResponse.success(years));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "根据ID获取考核周期")
    public ResponseEntity<ApiResponse<Cycle>> getCycleById(@PathVariable Long id) {
        Cycle cycle;
        try {
            cycle = cycleApplicationService.getCycleById(id);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "Cycle not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "创建新考核周期")
    public ResponseEntity<ApiResponse<Cycle>> createCycle(@Valid @RequestBody CreateCycleRequest request) {
        Cycle cycle = cycleApplicationService.createCycle(
                request.getName(),
                request.getYear(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @PostMapping("/{id:[0-9]+}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "激活考核周期")
    public ResponseEntity<ApiResponse<Cycle>> activateCycle(@PathVariable Long id) {
        Cycle cycle = cycleApplicationService.activateCycle(id);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @PostMapping("/{id:[0-9]+}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "停用考核周期")
    public ResponseEntity<ApiResponse<Cycle>> deactivateCycle(@PathVariable Long id) {
        Cycle cycle = cycleApplicationService.deactivateCycle(id);
        return ResponseEntity.ok(ApiResponse.success(cycle));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "删除考核周期")
    public ResponseEntity<ApiResponse<Void>> deleteCycle(@PathVariable Long id) {
        cycleApplicationService.deleteCycle(id);
        return ResponseEntity.ok(ApiResponse.success("Cycle deleted successfully", null));
    }

    private Page<Cycle> toPage(List<Cycle> cycles, int page, int size) {
        int start = Math.min(page * size, cycles.size());
        int end = Math.min(start + size, cycles.size());
        return new PageImpl<>(cycles.subList(start, end), PageRequest.of(page, size), cycles.size());
    }
}
