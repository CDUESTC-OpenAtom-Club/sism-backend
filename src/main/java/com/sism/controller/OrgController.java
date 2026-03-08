package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.enums.OrgType;
import com.sism.service.OrgService;
import com.sism.util.CacheUtils;
import com.sism.vo.OrgTreeVO;
import com.sism.vo.SysOrgVO;
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
 * Organization Controller (Primary API)
 * 
 * This is the primary, well-structured API for organization operations.
 * Provides complete query operations for organizations and hierarchy with:
 * - Strongly-typed request/response objects (SysOrgVO, OrgTreeVO)
 * - ETag-based HTTP caching for optimal performance
 * - Consistent error handling and validation
 * - RESTful endpoint design
 * 
 * Requirements: 8.2, 8.3, 8.4
 * Cache: ETag-based caching for hierarchy endpoints (5 minutes TTL)
 * 
 * Note: OrganizationController (/api/organizations) is deprecated and will be removed.
 *       All new integrations should use this controller.
 */
@Slf4j
@RestController
@RequestMapping("/orgs")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Primary organization management API with strongly-typed responses and ETag caching")
public class OrgController {

    private final OrgService orgService;

    /**
     * Get all active organizations
     * GET /api/orgs
     * Requirements: 8.2
     * Cache: ETag-based caching
     * **Validates: Requirements 4.2.1**
     */
    @GetMapping
    @Operation(summary = "Get all organizations", description = "Retrieve all active organizations with ETag caching")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organizations retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "304", description = "Not Modified - use cached data")
    })
    public ResponseEntity<ApiResponse<List<SysOrgVO>>> getAllOrgs(
            @Parameter(description = "Filter by organization type") 
            @RequestParam(required = false) OrgType type,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        List<SysOrgVO> orgs = orgService.getOrgsByType(type);
        ApiResponse<List<SysOrgVO>> response = ApiResponse.success(orgs);
        
        // Use ETag-based caching
        return CacheUtils.buildETagResponse(response, ifNoneMatch);
    }

    /**
     * Get organization hierarchy tree
     * GET /api/orgs/hierarchy
     * GET /api/orgs/tree (alias for backward compatibility)
     * Requirements: 8.3
     * Cache: ETag-based caching (5 minutes TTL)
     * **Validates: Requirements 4.2.1**
     */
    @GetMapping({"/hierarchy", "/tree"})
    @Operation(summary = "Get organization hierarchy", 
               description = "Retrieve organization hierarchy tree structure with ETag caching")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization hierarchy retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "304", description = "Not Modified - use cached data")
    })
    public ResponseEntity<ApiResponse<List<OrgTreeVO>>> getOrgHierarchy(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        List<OrgTreeVO> hierarchy = orgService.getOrgHierarchy();
        ApiResponse<List<OrgTreeVO>> response = ApiResponse.success(hierarchy);
        
        // Use ETag-based caching
        return CacheUtils.buildETagResponse(response, ifNoneMatch);
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
     * Get organization by ID
     * GET /api/orgs/{orgId}
     * Requirements: 8.2
     */
    @GetMapping("/{orgId}")
    @Operation(summary = "Get organization by ID", 
               description = "Retrieve a specific organization by its ID")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<ApiResponse<SysOrgVO>> getOrgById(
            @Parameter(description = "Organization ID") @PathVariable Long orgId) {
        SysOrgVO org = orgService.getOrgsByType(null).stream()
                .filter(o -> o.getId().equals(orgId))
                .findFirst()
                .orElseThrow(() -> new com.sism.exception.ResourceNotFoundException("Organization", orgId));
        return ResponseEntity.ok(ApiResponse.success(org));
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
