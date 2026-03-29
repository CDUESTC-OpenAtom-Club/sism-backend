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
@RequestMapping({"/api/v1/organizations", "/api/v1/orgs"})
@RequiredArgsConstructor
@Tag(name = "组织管理", description = "组织管理相关接口")
public class OrganizationController {

    private final OrganizationApplicationService organizationApplicationService;
    private final OrgMapper orgMapper;

    @PostMapping
    @Operation(summary = "创建新组织")
    public ResponseEntity<ApiResponse<OrgResponse>> createOrganization(
            @Valid @RequestBody OrgRequest request) {
        // Convert from shared OrgType to domain OrgType
        com.sism.organization.domain.OrgType domainType = 
            com.sism.organization.domain.OrgType.fromSharedOrgType(request.getType());
        SysOrg created = organizationApplicationService.createOrganization(request.getName(), domainType);
        OrgResponse response = orgMapper.toResponse(created);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "获取所有组织")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getAllOrganizations() {
        List<SysOrg> orgs = organizationApplicationService.getAllOrganizations();
        List<OrgResponse> responses = orgMapper.toResponseList(orgs);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/departments")
    @Operation(summary = "获取所有部门(旧版别名)")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getAllDepartments() {
        return getAllOrganizations();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取组织")
    public ResponseEntity<ApiResponse<OrgResponse>> getOrganizationById(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        OrgResponse response = orgMapper.toResponse(org);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取组织树")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getOrganizationTree(
            @Parameter(description = "响应中是否包含用户") @RequestParam(defaultValue = "false") boolean includeUsers,
            @Parameter(description = "是否包含已禁用的组织") @RequestParam(defaultValue = "false") boolean includeDisabled) {
        List<SysOrg> tree = organizationApplicationService.getOrganizationTree(includeUsers, includeDisabled);
        List<OrgResponse> responses = orgMapper.toResponseList(tree);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "根据组织ID获取用户列表")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByOrganizationId(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        List<User> users = organizationApplicationService.getUsersByOrganizationId(id);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活组织")
    public ResponseEntity<ApiResponse<OrgResponse>> activateOrganization(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        SysOrg activated = organizationApplicationService.activateOrganization(org);
        OrgResponse response = orgMapper.toResponse(activated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "停用组织")
    public ResponseEntity<ApiResponse<OrgResponse>> deactivateOrganization(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        SysOrg deactivated = organizationApplicationService.deactivateOrganization(org);
        OrgResponse response = orgMapper.toResponse(deactivated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/name")
    @Operation(summary = "重命名组织")
    public ResponseEntity<ApiResponse<OrgResponse>> renameOrganization(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @Parameter(description = "新组织名称") @RequestParam String newName) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        SysOrg renamed = organizationApplicationService.renameOrganization(org, newName);
        OrgResponse response = orgMapper.toResponse(renamed);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/type")
    @Operation(summary = "修改组织类型")
    public ResponseEntity<ApiResponse<OrgResponse>> changeOrganizationType(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @Parameter(description = "新组织类型") @RequestParam OrgType newType) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        // Convert from shared OrgType to domain OrgType
        com.sism.organization.domain.OrgType domainType = 
            com.sism.organization.domain.OrgType.fromSharedOrgType(newType);
        SysOrg updated = organizationApplicationService.changeOrganizationType(org, domainType);
        OrgResponse response = orgMapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/sort-order")
    @Operation(summary = "更新组织排序顺序")
    public ResponseEntity<ApiResponse<OrgResponse>> updateSortOrder(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @Parameter(description = "新排序顺序") @RequestParam Integer sortOrder) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Organization not found"));
        }
        SysOrg updated = organizationApplicationService.updateSortOrder(org, sortOrder);
        OrgResponse response = orgMapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
