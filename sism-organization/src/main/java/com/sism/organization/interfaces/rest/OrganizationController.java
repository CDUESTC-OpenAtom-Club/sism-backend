package com.sism.organization.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.domain.User;
import com.sism.organization.domain.OrgType;
import com.sism.organization.application.OrganizationApplicationService;
import com.sism.organization.domain.SysOrg;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrganizationController {

    private final OrganizationApplicationService organizationApplicationService;

    @PostMapping
    @Operation(summary = "Create a new organization")
    public ResponseEntity<ApiResponse<SysOrg>> createOrganization(
            @RequestParam String name,
            @RequestParam OrgType type) {
        SysOrg created = organizationApplicationService.createOrganization(name, type);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping
    @Operation(summary = "Get all organizations")
    public ResponseEntity<ApiResponse<List<SysOrg>>> getAllOrganizations() {
        List<SysOrg> orgs = organizationApplicationService.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.success(orgs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<ApiResponse<SysOrg>> getOrganizationById(@PathVariable Long id) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(org));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get organization tree")
    public ResponseEntity<ApiResponse<List<SysOrg>>> getOrganizationTree(
            @RequestParam(defaultValue = "false") boolean includeUsers,
            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        List<SysOrg> tree = organizationApplicationService.getOrganizationTree(includeUsers, includeDisabled);
        return ResponseEntity.ok(ApiResponse.success(tree));
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "Get users by organization ID")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByOrganizationId(@PathVariable Long id) {
        List<User> users = organizationApplicationService.getUsersByOrganizationId(id);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate an organization")
    public ResponseEntity<ApiResponse<SysOrg>> activateOrganization(
            @PathVariable Long id,
            @RequestBody SysOrg org) {
        SysOrg activated = organizationApplicationService.activateOrganization(org);
        return ResponseEntity.ok(ApiResponse.success(activated));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate an organization")
    public ResponseEntity<ApiResponse<SysOrg>> deactivateOrganization(
            @PathVariable Long id,
            @RequestBody SysOrg org) {
        SysOrg deactivated = organizationApplicationService.deactivateOrganization(org);
        return ResponseEntity.ok(ApiResponse.success(deactivated));
    }

    @PutMapping("/{id}/name")
    @Operation(summary = "Rename an organization")
    public ResponseEntity<ApiResponse<SysOrg>> renameOrganization(
            @PathVariable Long id,
            @RequestBody SysOrg org,
            @RequestParam String newName) {
        SysOrg renamed = organizationApplicationService.renameOrganization(org, newName);
        return ResponseEntity.ok(ApiResponse.success(renamed));
    }

    @PutMapping("/{id}/type")
    @Operation(summary = "Change organization type")
    public ResponseEntity<ApiResponse<SysOrg>> changeOrganizationType(
            @PathVariable Long id,
            @RequestBody SysOrg org,
            @RequestParam OrgType newType) {
        SysOrg updated = organizationApplicationService.changeOrganizationType(org, newType);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/sort-order")
    @Operation(summary = "Update organization sort order")
    public ResponseEntity<ApiResponse<SysOrg>> updateSortOrder(
            @PathVariable Long id,
            @RequestBody SysOrg org,
            @RequestParam Integer sortOrder) {
        SysOrg updated = organizationApplicationService.updateSortOrder(org, sortOrder);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}
