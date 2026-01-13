package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.enums.OrgType;
import com.sism.service.OrgService;
import com.sism.vo.OrgTreeVO;
import com.sism.vo.OrgVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Organization Controller
 * Provides query operations for organizations and hierarchy
 * 
 * Requirements: 8.2, 8.3
 */
@Slf4j
@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrgController {

    private final OrgService orgService;

    /**
     * Get all active organizations
     * GET /api/orgs
     * Requirements: 8.2
     */
    @GetMapping
    @Operation(summary = "Get all organizations", description = "Retrieve all active organizations")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organizations retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<OrgVO>>> getAllOrgs(
            @Parameter(description = "Filter by organization type") 
            @RequestParam(required = false) OrgType type) {
        List<OrgVO> orgs = orgService.getOrgsByType(type);
        return ResponseEntity.ok(ApiResponse.success(orgs));
    }

    /**
     * Get organization hierarchy tree
     * GET /api/orgs/hierarchy
     * Requirements: 8.3
     */
    @GetMapping("/hierarchy")
    @Operation(summary = "Get organization hierarchy", 
               description = "Retrieve organization hierarchy tree structure")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization hierarchy retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<OrgTreeVO>>> getOrgHierarchy() {
        List<OrgTreeVO> hierarchy = orgService.getOrgHierarchy();
        return ResponseEntity.ok(ApiResponse.success(hierarchy));
    }

    /**
     * Get organization hierarchy starting from a specific organization
     * GET /api/orgs/{orgId}/hierarchy
     */
    @GetMapping("/{orgId}/hierarchy")
    @Operation(summary = "Get organization subtree", 
               description = "Retrieve organization hierarchy starting from a specific organization")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization subtree retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<ApiResponse<OrgTreeVO>> getOrgHierarchyFrom(
            @Parameter(description = "Organization ID") @PathVariable Long orgId) {
        OrgTreeVO hierarchy = orgService.getOrgHierarchyFrom(orgId);
        return ResponseEntity.ok(ApiResponse.success(hierarchy));
    }

    /**
     * Get all descendant organization IDs
     * GET /api/orgs/{orgId}/descendants
     * Requirements: 8.4
     */
    @GetMapping("/{orgId}/descendants")
    @Operation(summary = "Get descendant organization IDs", 
               description = "Retrieve all descendant organization IDs for filtering")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Descendant organization IDs retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<ApiResponse<List<Long>>> getDescendantOrgIds(
            @Parameter(description = "Organization ID") @PathVariable Long orgId) {
        List<Long> descendantIds = orgService.getDescendantOrgIds(orgId);
        return ResponseEntity.ok(ApiResponse.success(descendantIds));
    }
}
