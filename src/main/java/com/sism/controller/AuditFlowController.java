package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.AuditFlowCreateRequest;
import com.sism.dto.AuditFlowUpdateRequest;
import com.sism.dto.AuditStepCreateRequest;
import com.sism.enums.AuditEntityType;
import com.sism.service.AuditFlowService;
import com.sism.vo.AuditFlowVO;
import com.sism.vo.AuditStepVO;
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
 * Audit Flow Controller
 * Provides CRUD operations for audit flow definitions
 */
@Slf4j
@RestController
@RequestMapping("/audit-flows")
@RequiredArgsConstructor
@Tag(name = "Audit Flows", description = "Audit flow management endpoints")
public class AuditFlowController {

    private final AuditFlowService auditFlowService;

    /**
     * Get all audit flows
     * GET /api/audit-flows
     */
    @GetMapping
    @Operation(summary = "Get all audit flows", description = "Retrieve all audit flow definitions")
    public ResponseEntity<ApiResponse<List<AuditFlowVO>>> getAllAuditFlows() {
        List<AuditFlowVO> auditFlows = auditFlowService.getAllAuditFlows();
        return ResponseEntity.ok(ApiResponse.success(auditFlows));
    }

    /**
     * Get audit flow by ID
     * GET /api/audit-flows/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get audit flow by ID", description = "Retrieve a specific audit flow with its steps")
    public ResponseEntity<ApiResponse<AuditFlowVO>> getAuditFlowById(
            @Parameter(description = "Audit flow ID") @PathVariable Long id) {
        AuditFlowVO auditFlow = auditFlowService.getAuditFlowById(id);
        return ResponseEntity.ok(ApiResponse.success(auditFlow));
    }

    /**
     * Get audit flow by code
     * GET /api/audit-flows/code/{flowCode}
     */
    @GetMapping("/code/{flowCode}")
    @Operation(summary = "Get audit flow by code", description = "Retrieve an audit flow by its code")
    public ResponseEntity<ApiResponse<AuditFlowVO>> getAuditFlowByCode(
            @Parameter(description = "Audit flow code") @PathVariable String flowCode) {
        AuditFlowVO auditFlow = auditFlowService.getAuditFlowByCode(flowCode);
        return ResponseEntity.ok(ApiResponse.success(auditFlow));
    }

    /**
     * Get audit flows by entity type
     * GET /api/audit-flows/entity-type/{entityType}
     */
    @GetMapping("/entity-type/{entityType}")
    @Operation(summary = "Get audit flows by entity type", description = "Retrieve audit flows for a specific entity type")
    public ResponseEntity<ApiResponse<List<AuditFlowVO>>> getAuditFlowsByEntityType(
            @Parameter(description = "Entity type") @PathVariable AuditEntityType entityType) {
        List<AuditFlowVO> auditFlows = auditFlowService.getAuditFlowsByEntityType(entityType);
        return ResponseEntity.ok(ApiResponse.success(auditFlows));
    }

    /**
     * Create a new audit flow
     * POST /api/audit-flows
     */
    @PostMapping
    @Operation(summary = "Create audit flow", description = "Create a new audit flow definition")
    public ResponseEntity<ApiResponse<AuditFlowVO>> createAuditFlow(
            @Valid @RequestBody AuditFlowCreateRequest request) {
        log.info("Creating audit flow: {}", request.getFlowCode());
        AuditFlowVO auditFlow = auditFlowService.createAuditFlow(request);
        return ResponseEntity.ok(ApiResponse.success("Audit flow created successfully", auditFlow));
    }

    /**
     * Update an existing audit flow
     * PUT /api/audit-flows/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update audit flow", description = "Update an existing audit flow definition")
    public ResponseEntity<ApiResponse<AuditFlowVO>> updateAuditFlow(
            @Parameter(description = "Audit flow ID") @PathVariable Long id,
            @Valid @RequestBody AuditFlowUpdateRequest request) {
        log.info("Updating audit flow: {}", id);
        AuditFlowVO auditFlow = auditFlowService.updateAuditFlow(id, request);
        return ResponseEntity.ok(ApiResponse.success("Audit flow updated successfully", auditFlow));
    }

    /**
     * Delete an audit flow
     * DELETE /api/audit-flows/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete audit flow", description = "Delete an audit flow and all its steps")
    public ResponseEntity<ApiResponse<Void>> deleteAuditFlow(
            @Parameter(description = "Audit flow ID") @PathVariable Long id) {
        log.info("Deleting audit flow: {}", id);
        auditFlowService.deleteAuditFlow(id);
        return ResponseEntity.ok(ApiResponse.success("Audit flow deleted successfully", null));
    }

    /**
     * Add a step to an audit flow
     * POST /api/audit-flows/steps
     */
    @PostMapping("/steps")
    @Operation(summary = "Add audit step", description = "Add a new step to an audit flow")
    public ResponseEntity<ApiResponse<AuditStepVO>> addAuditStep(
            @Valid @RequestBody AuditStepCreateRequest request) {
        log.info("Adding audit step to flow: {}", request.getFlowId());
        AuditStepVO step = auditFlowService.addAuditStep(request);
        return ResponseEntity.ok(ApiResponse.success("Audit step added successfully", step));
    }

    /**
     * Get all steps for an audit flow
     * GET /api/audit-flows/{flowId}/steps
     */
    @GetMapping("/{flowId}/steps")
    @Operation(summary = "Get audit steps", description = "Retrieve all steps for an audit flow")
    public ResponseEntity<ApiResponse<List<AuditStepVO>>> getAuditStepsByFlowId(
            @Parameter(description = "Audit flow ID") @PathVariable Long flowId) {
        List<AuditStepVO> steps = auditFlowService.getAuditStepsByFlowId(flowId);
        return ResponseEntity.ok(ApiResponse.success(steps));
    }
}
