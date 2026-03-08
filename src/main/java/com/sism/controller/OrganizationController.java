package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.service.OrgService;
import com.sism.service.SysOrgService;
import com.sism.vo.OrgTreeVO;
import com.sism.vo.SysOrgVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Organization Controller
 * Handles organization hierarchy and structure endpoints
 * 
 * @deprecated This controller is deprecated and will be removed in the next major version.
 *             Please use {@link OrgController} instead for all organization operations.
 *             Migration guide:
 *             - GET /api/organizations/tree -> GET /api/orgs/tree or GET /api/orgs/hierarchy
 *             - GET /api/organizations -> GET /api/orgs
 *             - GET /api/organizations/{id} -> Use GET /api/orgs and filter client-side, or use GET /api/orgs/{orgId}/hierarchy
 * 
 * TODO: Remove this controller in version 2.0.0
 */
@Deprecated
@Slf4j
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations (Deprecated)", description = "DEPRECATED: Use /api/orgs endpoints instead. This API will be removed in the next major version.")
public class OrganizationController {

    private final SysOrgService sysOrgService;
    private final OrgService orgService;

    /**
     * Get organization tree (flat list for now, as sys_org is flat structure)
     * GET /api/organizations/tree
     * 
     * @deprecated Use {@link OrgController#getOrgHierarchy(String)} instead at GET /api/orgs/tree or GET /api/orgs/hierarchy
     *             The new endpoint provides proper hierarchical structure with ETag caching support.
     */
    @Deprecated
    @GetMapping("/tree")
    @Operation(
        summary = "Get organization tree (DEPRECATED)", 
        description = "DEPRECATED: Use GET /api/orgs/tree or GET /api/orgs/hierarchy instead. " +
                      "This endpoint will be removed in the next major version. " +
                      "The new endpoint provides proper hierarchical structure with ETag caching support.",
        deprecated = true
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrganizationTree() {
        log.warn("DEPRECATED: /api/organizations/tree endpoint called. Please migrate to /api/orgs/tree or /api/orgs/hierarchy");
        
        try {
            // Delegate to OrgController's hierarchy endpoint
            List<OrgTreeVO> hierarchy = orgService.getOrgHierarchy();
            
            // Convert to legacy format for backward compatibility
            List<Map<String, Object>> tree = hierarchy.stream()
                .map(this::convertOrgTreeToLegacyFormat)
                .collect(Collectors.toList());
            
            log.info("Organization tree built with {} nodes (via deprecated endpoint)", tree.size());
            return ResponseEntity.ok(ApiResponse.success(tree));
        } catch (Exception e) {
            log.error("Error fetching organization tree", e);
            throw e;
        }
    }

    /**
     * Get all organizations (flat list)
     * GET /api/organizations
     * 
     * @deprecated Use {@link OrgController#getAllOrgs(com.sism.enums.OrgType, String)} instead at GET /api/orgs
     *             The new endpoint provides type filtering and ETag caching support.
     */
    @Deprecated
    @GetMapping
    @Operation(
        summary = "Get all organizations (DEPRECATED)", 
        description = "DEPRECATED: Use GET /api/orgs instead. " +
                      "This endpoint will be removed in the next major version. " +
                      "The new endpoint provides type filtering and ETag caching support.",
        deprecated = true
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllOrganizations() {
        log.warn("DEPRECATED: /api/organizations endpoint called. Please migrate to /api/orgs");
        
        try {
            // Delegate to OrgController's getAllOrgs endpoint
            List<SysOrgVO> orgs = orgService.getOrgsByType(null);
            
            // Convert to legacy format for backward compatibility
            List<Map<String, Object>> orgMaps = orgs.stream()
                .map(org -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", org.getId());
                    map.put("name", org.getName());
                    map.put("type", org.getType());
                    map.put("isActive", org.getIsActive() != null ? org.getIsActive() : true);
                    map.put("sortOrder", org.getSortOrder());
                    map.put("createdAt", org.getCreatedAt());
                    map.put("updatedAt", org.getUpdatedAt());
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("Retrieved {} organizations (via deprecated endpoint)", orgMaps.size());
            return ResponseEntity.ok(ApiResponse.success(orgMaps));
        } catch (Exception e) {
            log.error("Error fetching organizations", e);
            throw e;
        }
    }

    /**
     * Get organization by ID
     * GET /api/organizations/{id}
     * 
     * @deprecated Use {@link OrgController#getAllOrgs(com.sism.enums.OrgType, String)} and filter client-side,
     *             or use {@link OrgController#getOrgHierarchyFrom(Long)} at GET /api/orgs/{orgId}/hierarchy
     *             for hierarchical data starting from a specific organization.
     */
    @Deprecated
    @GetMapping("/{id}")
    @Operation(
        summary = "Get organization by ID (DEPRECATED)", 
        description = "DEPRECATED: Use GET /api/orgs and filter client-side, or use GET /api/orgs/{orgId}/hierarchy. " +
                      "This endpoint will be removed in the next major version.",
        deprecated = true
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizationById(@PathVariable Long id) {
        log.warn("DEPRECATED: /api/organizations/{} endpoint called. Please migrate to /api/orgs", id);
        
        try {
            // Delegate to legacy service for backward compatibility
            SysOrgVO org = sysOrgService.getOrganizationById(id);
            
            Map<String, Object> orgMap = new HashMap<>();
            orgMap.put("id", org.getId());
            orgMap.put("name", org.getName());
            orgMap.put("type", org.getType());
            orgMap.put("isActive", org.getIsActive() != null ? org.getIsActive() : true);
            orgMap.put("sortOrder", org.getSortOrder());
            orgMap.put("createdAt", org.getCreatedAt());
            orgMap.put("updatedAt", org.getUpdatedAt());
            
            return ResponseEntity.ok(ApiResponse.success(orgMap));
        } catch (Exception e) {
            log.error("Error fetching organization with ID: {}", id, e);
            throw e;
        }
    }

    /**
     * Helper method to convert OrgTreeVO to legacy Map format
     * Flattens the hierarchical structure for backward compatibility
     */
    private Map<String, Object> convertOrgTreeToLegacyFormat(OrgTreeVO orgTree) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", orgTree.getOrgId());
        node.put("name", orgTree.getOrgName());
        node.put("type", orgTree.getOrgType());
        node.put("isActive", true); // OrgTreeVO doesn't have isActive, default to true
        node.put("sortOrder", orgTree.getSortOrder());
        return node;
    }
}
