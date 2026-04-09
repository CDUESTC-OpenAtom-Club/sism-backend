package com.sism.organization.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.enums.OrgType;
import com.sism.organization.application.OrganizationApplicationService;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.interfaces.dto.OrgRequest;
import com.sism.organization.interfaces.dto.OrgResponse;
import com.sism.organization.interfaces.dto.OrgMapper;
import com.sism.organization.interfaces.dto.OrgUserResponse;
import com.sism.shared.domain.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/organizations", "/api/v1/orgs"})
@RequiredArgsConstructor
@Validated
@Tag(name = "组织管理", description = "组织管理相关接口")
public class OrganizationController {

    private final OrganizationApplicationService organizationApplicationService;
    private final OrgMapper orgMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建新组织")
    public ResponseEntity<ApiResponse<OrgResponse>> createOrganization(
            @Valid @RequestBody OrgRequest request) {
        // Convert from shared OrgType to domain OrgType
        com.sism.organization.domain.OrgType domainType =
            com.sism.organization.domain.OrgType.fromSharedOrgType(request.getType());
        SysOrg created = organizationApplicationService.createOrganization(
                request.getName(),
                domainType,
                request.getParentOrgId(),
                request.getSortOrder()
        );
        OrgResponse response = orgMapper.toResponse(created);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "获取所有组织")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getAllOrganizations() {
        List<SysOrg> orgs = organizationApplicationService.getAllOrganizations();
        List<OrgResponse> responses = orgMapper.toResponseList(orgs);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping(params = {"pageNum", "pageSize"})
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "获取所有组织（分页）")
    public ResponseEntity<ApiResponse<PageResult<OrgResponse>>> getAllOrganizationsPage(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        return ResponseEntity.ok(ApiResponse.success(toOrgPageResult(
                organizationApplicationService.getAllOrganizations(pageNum, pageSize)
        )));
    }

    @GetMapping(params = {"page", "size"})
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "获取所有组织（分页，别名）")
    public ResponseEntity<ApiResponse<PageResult<OrgResponse>>> getAllOrganizationsPageAlias(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(toOrgPageResult(
                organizationApplicationService.getAllOrganizations(page, size)
        )));
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "获取所有部门")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getAllDepartments(
            @Parameter(description = "是否包含已禁用的组织") @RequestParam(defaultValue = "false") boolean includeDisabled) {
        List<SysOrg> orgs = organizationApplicationService.getDepartmentOrganizations(includeDisabled);
        List<OrgResponse> responses = orgMapper.toResponseList(orgs);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "根据ID获取组织")
    public ResponseEntity<ApiResponse<OrgResponse>> getOrganizationById(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        SysOrg org = requireOrganization(id);
        OrgResponse response = orgMapper.toResponse(org);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "获取组织树", description = "includeUsers 为兼容保留参数，已停用，当前响应结构不会返回用户详情")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getOrganizationTree(
            @Parameter(description = "兼容保留参数，已停用；传入 true 也不会返回用户详情") @RequestParam(defaultValue = "false") boolean includeUsers,
            @Parameter(description = "是否包含已禁用的组织") @RequestParam(defaultValue = "false") boolean includeDisabled) {
        List<SysOrg> tree = organizationApplicationService.getOrganizationTree(includeUsers, includeDisabled);
        List<OrgResponse> responses = orgMapper.toResponseList(tree);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "根据组织ID获取用户列表")
    public ResponseEntity<ApiResponse<List<OrgUserResponse>>> getUsersByOrganizationId(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                organizationApplicationService.getUsersByOrganizationId(id)
        ));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "激活组织")
    public ResponseEntity<ApiResponse<OrgResponse>> activateOrganization(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        SysOrg org = requireOrganization(id);
        SysOrg activated = organizationApplicationService.activateOrganization(org);
        OrgResponse response = orgMapper.toResponse(activated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "停用组织")
    public ResponseEntity<ApiResponse<OrgResponse>> deactivateOrganization(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        SysOrg org = requireOrganization(id);
        SysOrg deactivated = organizationApplicationService.deactivateOrganization(org);
        OrgResponse response = orgMapper.toResponse(deactivated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/name")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "重命名组织")
    public ResponseEntity<ApiResponse<OrgResponse>> renameOrganization(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @Parameter(description = "新组织名称") @RequestParam String newName) {
        SysOrg org = requireOrganization(id);
        SysOrg renamed = organizationApplicationService.renameOrganization(org, newName);
        OrgResponse response = orgMapper.toResponse(renamed);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/type")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "修改组织类型")
    public ResponseEntity<ApiResponse<OrgResponse>> changeOrganizationType(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @Parameter(description = "新组织类型") @RequestParam OrgType newType) {
        SysOrg org = requireOrganization(id);
        // Convert from shared OrgType to domain OrgType
        com.sism.organization.domain.OrgType domainType =
            com.sism.organization.domain.OrgType.fromSharedOrgType(newType);
        SysOrg updated = organizationApplicationService.changeOrganizationType(org, domainType);
        OrgResponse response = orgMapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/sort-order")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新组织排序顺序")
    public ResponseEntity<ApiResponse<OrgResponse>> updateSortOrder(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @Parameter(description = "新排序顺序") @RequestParam Integer sortOrder) {
        SysOrg org = requireOrganization(id);
        SysOrg updated = organizationApplicationService.updateSortOrder(org, sortOrder);
        OrgResponse response = orgMapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/parent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新父组织")
    public ResponseEntity<ApiResponse<OrgResponse>> updateParentOrganization(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @Parameter(description = "父组织ID，可为空表示设为顶级组织") @RequestParam(required = false) Long parentOrgId) {
        SysOrg org = requireOrganization(id);
        SysOrg updated = organizationApplicationService.updateParentOrganization(org, parentOrgId);
        OrgResponse response = orgMapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private SysOrg requireOrganization(Long id) {
        SysOrg org = organizationApplicationService.getOrganizationById(id);
        if (org == null) {
            throw new ResourceNotFoundException("Organization", id);
        }
        return org;
    }

    private PageResult<OrgResponse> toOrgPageResult(PageResult<SysOrg> pageResult) {
        return new PageResult<>(
                orgMapper.toResponseList(pageResult.getItems()),
                pageResult.getTotal(),
                pageResult.getPage(),
                pageResult.getPageSize()
        );
    }
}
