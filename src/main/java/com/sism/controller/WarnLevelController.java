package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.WarnLevelCreateRequest;
import com.sism.dto.WarnLevelUpdateRequest;
import com.sism.service.WarnLevelService;
import com.sism.vo.WarnLevelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Warning Level Controller for SISM (Strategic Indicator Management System).
 * 
 * <p>This controller manages warning level definitions which establish thresholds
 * for progress monitoring and alert generation. Warning levels enable proactive
 * identification of indicators that require attention.
 * 
 * <h2>Warning Level System</h2>
 * <p>Warning levels define severity thresholds based on indicator progress:
 * <ul>
 *   <li><b>NORMAL</b>: Progress >= 80% (Green status)</li>
 *   <li><b>WARNING</b>: Progress 50-79% (Yellow status)</li>
 *   <li><b>CRITICAL</b>: Progress < 50% (Red status)</li>
 * </ul>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Configurable warning thresholds</li>
 *   <li>Severity level management (INFO, WARNING, CRITICAL)</li>
 *   <li>Active/inactive status control</li>
 *   <li>Code-based lookup for quick access</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Dashboard alert generation</li>
 *   <li>Automated notification triggers</li>
 *   <li>Progress monitoring rules</li>
 *   <li>Escalation workflows</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li>GET /api/warn-levels - List all warning levels</li>
 *   <li>GET /api/warn-levels/{id} - Get warning level details</li>
 *   <li>GET /api/warn-levels/code/{levelCode} - Get by code</li>
 *   <li>GET /api/warn-levels/active - List active levels</li>
 *   <li>POST /api/warn-levels - Create new warning level</li>
 *   <li>PUT /api/warn-levels/{id} - Update warning level</li>
 *   <li>DELETE /api/warn-levels/{id} - Delete warning level</li>
 * </ul>
 * 
 * @author SISM Development Team
 * @version 1.0
 * @since 1.0
 * @see com.sism.service.WarnLevelService
 * @see com.sism.entity.WarnLevel
 */
@Slf4j
@RestController
@RequestMapping("/warn-levels")
@RequiredArgsConstructor
@Tag(name = "Warning Levels", description = "Warning level management endpoints")
public class WarnLevelController {

    private final WarnLevelService warnLevelService;

    /**
     * Get all warning levels
     * GET /api/warn-levels
     */
    @GetMapping
    @Operation(summary = "Get all warning levels", description = "Retrieve all warning levels")
    public ResponseEntity<ApiResponse<List<WarnLevelVO>>> getAllWarnLevels() {
        List<WarnLevelVO> warnLevels = warnLevelService.getAllWarnLevels();
        return ResponseEntity.ok(ApiResponse.success(warnLevels));
    }

    /**
     * Get warning level by ID
     * GET /api/warn-levels/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get warning level by ID", description = "Retrieve a specific warning level")
    public ResponseEntity<ApiResponse<WarnLevelVO>> getWarnLevelById(
            @Parameter(description = "Warning level ID") @PathVariable Long id) {
        WarnLevelVO warnLevel = warnLevelService.getWarnLevelById(id);
        return ResponseEntity.ok(ApiResponse.success(warnLevel));
    }

    /**
     * Get warning level by code
     * GET /api/warn-levels/code/{levelCode}
     */
    @GetMapping("/code/{levelCode}")
    @Operation(summary = "Get warning level by code", description = "Retrieve a warning level by its code")
    public ResponseEntity<ApiResponse<WarnLevelVO>> getWarnLevelByCode(
            @Parameter(description = "Warning level code") @PathVariable String levelCode) {
        WarnLevelVO warnLevel = warnLevelService.getWarnLevelByCode(levelCode);
        return ResponseEntity.ok(ApiResponse.success(warnLevel));
    }

    /**
     * Get active warning levels
     * GET /api/warn-levels/active
     */
    @GetMapping("/active")
    @Operation(summary = "Get active warning levels", description = "Retrieve all active warning levels")
    public ResponseEntity<ApiResponse<List<WarnLevelVO>>> getActiveWarnLevels() {
        List<WarnLevelVO> warnLevels = warnLevelService.getActiveWarnLevels();
        return ResponseEntity.ok(ApiResponse.success(warnLevels));
    }

    /**
     * Create a new warning level
     * POST /api/warn-levels
     */
    @PostMapping
    @Operation(summary = "Create warning level", description = "Create a new warning level")
    public ResponseEntity<ApiResponse<WarnLevelVO>> createWarnLevel(
            @Valid @RequestBody WarnLevelCreateRequest request) {
        log.info("Creating warning level: {}", request.getLevelCode());
        WarnLevelVO warnLevel = warnLevelService.createWarnLevel(request);
        return ResponseEntity.ok(ApiResponse.success("Warning level created successfully", warnLevel));
    }

    /**
     * Update an existing warning level
     * PUT /api/warn-levels/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update warning level", description = "Update an existing warning level")
    public ResponseEntity<ApiResponse<WarnLevelVO>> updateWarnLevel(
            @Parameter(description = "Warning level ID") @PathVariable Long id,
            @Valid @RequestBody WarnLevelUpdateRequest request) {
        log.info("Updating warning level: {}", id);
        WarnLevelVO warnLevel = warnLevelService.updateWarnLevel(id, request);
        return ResponseEntity.ok(ApiResponse.success("Warning level updated successfully", warnLevel));
    }

    /**
     * Delete a warning level
     * DELETE /api/warn-levels/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete warning level", description = "Delete a warning level")
    public ResponseEntity<ApiResponse<Void>> deleteWarnLevel(
            @Parameter(description = "Warning level ID") @PathVariable Long id) {
        log.info("Deleting warning level: {}", id);
        warnLevelService.deleteWarnLevel(id);
        return ResponseEntity.ok(ApiResponse.success("Warning level deleted successfully", null));
    }
}
