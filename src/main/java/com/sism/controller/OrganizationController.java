package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.service.SysOrgService;
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
 */
@Slf4j
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrganizationController {

    private final SysOrgService sysOrgService;

    /**
     * Get organization tree (flat list for now, as sys_org is flat structure)
     * GET /api/organizations/tree
     */
    @GetMapping("/tree")
    @Operation(summary = "Get organization tree", description = "Retrieve organization structure")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrganizationTree() {
        log.info("Fetching organization tree");
        
        try {
            List<SysOrgVO> allOrgs = sysOrgService.getAllOrganizations();
            
            // Convert to simple map structure
            List<Map<String, Object>> tree = allOrgs.stream()
                .map(org -> {
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", org.getId());
                    node.put("name", org.getName());
                    node.put("type", org.getType());
                    node.put("isActive", org.getIsActive() != null ? org.getIsActive() : true);
                    node.put("sortOrder", org.getSortOrder());
                    return node;
                })
                .collect(Collectors.toList());
            
            log.info("Organization tree built with {} nodes", tree.size());
            return ResponseEntity.ok(ApiResponse.success(tree));
        } catch (Exception e) {
            log.error("Error fetching organization tree", e);
            throw e;
        }
    }

    /**
     * Get all organizations (flat list)
     * GET /api/organizations
     */
    @GetMapping
    @Operation(summary = "Get all organizations", description = "Retrieve all organizations as flat list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllOrganizations() {
        log.info("Fetching all organizations");
        
        try {
            List<SysOrgVO> orgs = sysOrgService.getAllOrganizations();
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
            
            log.info("Retrieved {} organizations", orgMaps.size());
            return ResponseEntity.ok(ApiResponse.success(orgMaps));
        } catch (Exception e) {
            log.error("Error fetching organizations", e);
            throw e;
        }
    }

    /**
     * Get organization by ID
     * GET /api/organizations/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID", description = "Retrieve organization details by ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrganizationById(@PathVariable Long id) {
        log.info("Fetching organization with ID: {}", id);
        
        try {
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
}
