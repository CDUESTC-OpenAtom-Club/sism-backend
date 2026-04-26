package com.sism.iam.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.common.PageResult;
import com.sism.iam.application.service.PaginationPolicy;
import com.sism.iam.application.service.RoleManagementService;
import com.sism.iam.domain.Permission;
import com.sism.iam.domain.Role;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.repository.OrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RoleManagementController - 角色权限管理控制器
 * 提供角色和权限管理的REST API端点
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Validated
@Tag(name = "角色权限管理", description = "角色和权限管理接口")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;
    private final OrganizationRepository organizationRepository;

    // ========== 角色查询 ==========

    @GetMapping
    @Operation(summary = "分页查询角色列表")
    public ResponseEntity<ApiResponse<PageResult<RoleResponse>>> listRoles(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        ResponseEntity<ApiResponse<PageResult<RoleResponse>>> denied = denyIfNoAdminOrgAccess(currentUser);
        if (denied != null) {
            return denied;
        }
        Page<Role> rolePage = roleManagementService.findRoles(PaginationPolicy.toPageRequest(page, pageSize));
        Set<Long> roleIds = rolePage.getContent().stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
        Map<Long, Long> userCounts = roleManagementService.countUsersByRoleIds(roleIds);
        Map<Long, Long> permissionCounts = roleManagementService.countPermissionsByRoleIds(roleIds);
        PageResult<RoleResponse> result = PageResult.of(
                rolePage.getContent().stream()
                        .map(role -> convertToResponse(
                                role,
                                userCounts.getOrDefault(role.getId(), 0L),
                                permissionCounts.getOrDefault(role.getId(), 0L)))
                        .collect(Collectors.toList()),
                rolePage.getTotalElements(),
                rolePage.getNumber(),
                rolePage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询角色详情")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        ResponseEntity<ApiResponse<RoleResponse>> denied = denyIfNoAdminOrgAccess(currentUser);
        if (denied != null) {
            return denied;
        }
        Optional<Role> roleOpt = roleManagementService.findRoleById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Role role = roleOpt.get();
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(
                role,
                roleManagementService.countUsersByRoleId(id),
                roleManagementService.countPermissionsByRoleId(id)
        )));
    }

    // ========== 角色创建 ==========

    @PostMapping
    @Operation(summary = "创建新角色")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateRoleRequest request
    ) {
        ResponseEntity<ApiResponse<RoleResponse>> denied = denyIfNoAdminOrgAccess(currentUser);
        if (denied != null) {
            return denied;
        }
        try {
            Role role = roleManagementService.createRole(
                    request.getRoleCode(),
                    request.getRoleName(),
                    request.getDescription()
            );
            return ResponseEntity.ok(ApiResponse.success(convertToResponse(role, 0, 0)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "角色参数不合法"));
        }
    }

    // ========== 角色更新 ==========

    @PutMapping("/{id}")
    @Operation(summary = "更新角色信息")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        ResponseEntity<ApiResponse<RoleResponse>> denied = denyIfNoAdminOrgAccess(currentUser);
        if (denied != null) {
            return denied;
        }
        Optional<Role> roleOpt = roleManagementService.findRoleById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Role role = roleManagementService.updateRole(
                    id,
                    request.getRoleCode(),
                    request.getRoleName(),
                    request.getDescription()
            );
            return ResponseEntity.ok(ApiResponse.success(convertToResponse(
                    role,
                    roleManagementService.countUsersByRoleId(role.getId()),
                    roleManagementService.countPermissionsByRoleId(role.getId())
            )));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "角色参数不合法"));
        }
    }

    // ========== 角色删除 ==========

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        ResponseEntity<ApiResponse<Void>> denied = denyIfNoAdminOrgAccess(currentUser);
        if (denied != null) {
            return denied;
        }
        Optional<Role> roleOpt = roleManagementService.findRoleById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            roleManagementService.deleteRole(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "当前角色状态不允许删除"));
        }
    }

    // ========== 权限管理 ==========

    @PostMapping("/{id}/permissions")
    @Operation(summary = "给角色分配权限")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermissions(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody AssignPermissionsRequest request
    ) {
        ResponseEntity<ApiResponse<RoleResponse>> denied = denyIfNoAdminOrgAccess(currentUser);
        if (denied != null) {
            return denied;
        }
        Optional<Role> roleOpt = roleManagementService.findRoleById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Set<Long> permissionIds = request.getPermissionIds() == null
                ? Set.of()
                : request.getPermissionIds().stream()
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        try {
            Role role = roleManagementService.assignPermissions(id, permissionIds);
            return ResponseEntity.ok(ApiResponse.success(convertToResponse(
                    role,
                    roleManagementService.countUsersByRoleId(role.getId()),
                    roleManagementService.countPermissionsByRoleId(role.getId())
            )));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "角色权限参数不合法"));
        }
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取角色的权限列表")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getRolePermissions(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id
    ) {
        ResponseEntity<ApiResponse<List<PermissionResponse>>> denied = denyIfNoAdminOrgAccess(currentUser);
        if (denied != null) {
            return denied;
        }
        Optional<Role> roleOpt = roleManagementService.findRoleById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<PermissionResponse> permissions = roleManagementService.getRolePermissions(id).stream()
                .map(this::convertToPermissionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @Operation(summary = "从角色移除权限")
    public ResponseEntity<ApiResponse<Void>> removePermission(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @PathVariable Long permissionId
    ) {
        ResponseEntity<ApiResponse<Void>> denied = denyIfNoAdminOrgAccess(currentUser);
        if (denied != null) {
            return denied;
        }
        Optional<Role> roleOpt = roleManagementService.findRoleById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            roleManagementService.removePermission(id, permissionId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "角色权限参数不合法"));
        }
    }

    // ========== 内部辅助方法 ==========

    private boolean hasAdminOrgAccess(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getOrgId() == null) {
            return false;
        }

        return organizationRepository.findById(currentUser.getOrgId())
                .map(org -> org.getType() == OrgType.admin)
                .orElse(false);
    }

    private <T> ResponseEntity<ApiResponse<T>> denyIfNoAdminOrgAccess(CurrentUser currentUser) {
        if (hasAdminOrgAccess(currentUser)) {
            return null;
        }

        return ResponseEntity.status(403).body(ApiResponse.error(403, "无权限访问"));
    }

    private RoleResponse convertToResponse(Role role, long userCount, long permissionCount) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setIsEnabled(role.getIsEnabled());
        response.setPermissionCount(Math.toIntExact(permissionCount));
        response.setUserCount(Math.toIntExact(userCount));
        response.setCreateTime(role.getCreatedAt());

        return response;
    }

    private PermissionResponse convertToPermissionResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setPermissionCode(permission.getPermissionCode());
        response.setPermissionName(permission.getPermissionName());
        response.setPermType(permission.getPermType());
        response.setEnabled(permission.getIsEnabled());

        return response;
    }

    // ========== 请求和响应 DTO ==========

    @lombok.Data
    public static class CreateRoleRequest {
        @NotBlank(message = "roleCode is required")
        @Size(max = 64, message = "roleCode must not exceed 64 characters")
        @Pattern(regexp = "[A-Za-z0-9_:-]+", message = "roleCode contains unsupported characters")
        private String roleCode;

        @NotBlank(message = "roleName is required")
        @Size(max = 64, message = "roleName must not exceed 64 characters")
        private String roleName;

        @Size(max = 255, message = "description must not exceed 255 characters")
        private String description;
    }

    @lombok.Data
    public static class UpdateRoleRequest {
        @Size(min = 2, max = 64, message = "roleCode length must be between 2 and 64 characters")
        @Pattern(regexp = "[A-Za-z0-9_:-]+", message = "roleCode contains unsupported characters")
        private String roleCode;

        @Size(min = 2, max = 64, message = "roleName length must be between 2 and 64 characters")
        private String roleName;

        @Size(max = 255, message = "description must not exceed 255 characters")
        private String description;
    }

    @lombok.Data
    public static class RoleResponse {
        private Long id;
        private String roleCode;
        private String roleName;
        private String description;
        private Boolean isEnabled;  // 新版本改为 isEnabled
        private Integer userCount;
        private Integer permissionCount;
        private LocalDateTime createTime;
    }

    @lombok.Data
    public static class PermissionResponse {
        private Long id;
        private String permissionCode;
        private String permissionName;
        private String permType;  // 新增：权限类型
        private Boolean enabled;   // 新增：是否启用
    }

    @lombok.Data
    public static class AssignPermissionsRequest {
        @NotEmpty(message = "permissionIds is required")
        private List<@NotNull(message = "permissionId must not be null") Long> permissionIds;
    }
}
