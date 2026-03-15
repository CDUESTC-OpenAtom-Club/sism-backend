package com.sism.organization.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.domain.User;
import com.sism.enums.OrgType;
import com.sism.organization.application.OrganizationApplicationService;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.interfaces.dto.OrgRequest;
import com.sism.organization.interfaces.dto.OrgResponse;
import com.sism.organization.interfaces.dto.OrgMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final OrgMapper orgMapper;

    @PostMapping
    @Operation(summary = "Create a new organization")
    public ResponseEntity<ApiResponse<OrgResponse>> createOrganization(
            @Valid @RequestBody OrgRequest request) {
        SysOrg created = organizationApplicationService.createOrganization(request.getName(), request.getType());
        OrgResponse response = orgMapper.toResponse(created);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all organizations")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getAllOrganizations() {
        List<SysOrg> orgs = organizationApplicationService.getAllOrganizations();
        List<OrgResponse> responses = orgMapper.toResponseList(orgs);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<ApiResponse<OrgResponse>> getOrganizationById(
            @Parameter(description = "Organization ID") @PathVariable Long id) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        OrgResponse response = orgMapper.toResponse(org);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get organization tree")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getOrganizationTree(
            @Parameter(description = "Include users in response") @RequestParam(defaultValue = "false") boolean includeUsers,
            @Parameter(description = "Include disabled organizations") @RequestParam(defaultValue = "false") boolean includeDisabled) {
        List<SysOrg> tree = organizationApplicationService.getOrganizationTree(includeUsers, includeDisabled);
        List<OrgResponse> responses = orgMapper.toResponseList(tree);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "Get users by organization ID")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByOrganizationId(
            @Parameter(description = "Organization ID") @PathVariable Long id) {
        List<User> users = organizationApplicationService.getUsersByOrganizationId(id);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate an organization")
    public ResponseEntity<ApiResponse<OrgResponse>> activateOrganization(
            @Parameter(description = "Organization ID") @PathVariable Long id) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        SysOrg activated = organizationApplicationService.activateOrganization(org);
        OrgResponse response = orgMapper.toResponse(activated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate an organization")
    public ResponseEntity<ApiResponse<OrgResponse>> deactivateOrganization(
            @Parameter(description = "Organization ID") @PathVariable Long id) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        SysOrg deactivated = organizationApplicationService.deactivateOrganization(org);
        OrgResponse response = orgMapper.toResponse(deactivated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/name")
    @Operation(summary = "Rename an organization")
    public ResponseEntity<ApiResponse<OrgResponse>> renameOrganization(
            @Parameter(description = "Organization ID") @PathVariable Long id,
            @Parameter(description = "New organization name") @RequestParam String newName) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        SysOrg renamed = organizationApplicationService.renameOrganization(org, newName);
        OrgResponse response = orgMapper.toResponse(renamed);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/type")
    @Operation(summary = "Change organization type")
    public ResponseEntity<ApiResponse<OrgResponse>> changeOrganizationType(
            @Parameter(description = "Organization ID") @PathVariable Long id,
            @Parameter(description = "New organization type") @RequestParam OrgType newType) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        SysOrg updated = organizationApplicationService.changeOrganizationType(org, newType);
        OrgResponse response = orgMapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/sort-order")
    @Operation(summary = "Update organization sort order")
    public ResponseEntity<ApiResponse<OrgResponse>> updateSortOrder(
            @Parameter(description = "Organization ID") @PathVariable Long id,
            @Parameter(description = "New sort order") @RequestParam Integer sortOrder) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        SysOrg updated = organizationApplicationService.updateSortOrder(org, sortOrder);
        OrgResponse response = orgMapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
