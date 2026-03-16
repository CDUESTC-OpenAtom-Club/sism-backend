package com.sism.iam.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.iam.application.service.RoleService;
import com.sism.iam.domain.Permission;
import com.sism.iam.domain.Role;
import com.sism.iam.domain.repository.PermissionRepository;
import com.sism.iam.domain.repository.RoleRepository;
import com.sism.iam.domain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RoleManagementController - 角色权限管理控制器
 * 提供角色和权限管理的REST API端点
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Role and permission management endpoints")
public class RoleManagementController {

    private final RoleService roleService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    // ========== 角色查询 ==========

    @GetMapping
    @Operation(summary = "分页查询角色列表")
    public ResponseEntity<ApiResponse<PageResult<RoleResponse>>> listRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Role> rolePage = roleRepository.findAll(pageable);
        PageResult<RoleResponse> result = PageResult.of(
                rolePage.getContent().stream().map(this::convertToResponse).collect(Collectors.toList()),
                rolePage.getTotalElements(),
                rolePage.getNumber(),
                rolePage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询角色详情")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {
        Optional<Role> roleOpt = roleRepository.findById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(roleOpt.get())));
    }

    // ========== 角色创建 ==========

    @PostMapping
    @Operation(summary = "创建新角色")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request
    ) {
        // 检查角色代码是否已存在
        if (roleRepository.findByRoleCode(request.getRoleCode()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role code already exists"));
        }
        if (roleRepository.findByRoleName(request.getRoleName()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role name already exists"));
        }

        Role role = roleService.createRole(
                request.getRoleCode(),
                request.getRoleName(),
                request.getDescription()
        );
        role = roleRepository.save(role);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(role)));
    }

    // ========== 角色更新 ==========

    @PutMapping("/{id}")
    @Operation(summary = "更新角色信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        Optional<Role> roleOpt = roleRepository.findById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Role role = roleOpt.get();

        // 检查角色代码是否已被其他角色使用
        if (request.getRoleCode() != null && !request.getRoleCode().equals(role.getRoleCode())) {
            if (roleRepository.findByRoleCode(request.getRoleCode()).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Role code already exists"));
            }
            role.setRoleCode(request.getRoleCode());
        }

        // 检查角色名称是否已被其他角色使用
        if (request.getRoleName() != null && !request.getRoleName().equals(role.getRoleName())) {
            if (roleRepository.findByRoleName(request.getRoleName()).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Role name already exists"));
            }
            role.setRoleName(request.getRoleName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        role = roleRepository.save(role);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(role)));
    }

    // ========== 角色删除 ==========

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        Optional<Role> roleOpt = roleRepository.findById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 检查角色是否仍在使用（是否有用户关联）
        List<Role> userRoles = roleRepository.findByUserId(id);
        if (!userRoles.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role is still in use by users"));
        }

        roleRepository.delete(roleOpt.get());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ========== 权限管理 ==========

    @PostMapping("/{id}/permissions")
    @Operation(summary = "给角色分配权限")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermissions(
            @PathVariable Long id,
            @RequestBody AssignPermissionsRequest request
    ) {
        Optional<Role> roleOpt = roleRepository.findById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Role role = roleOpt.get();

        // 从 PermissionRepository 实际查询权限对象
        Set<Long> permissionIds = Set.copyOf(request.getPermissionIds());
        Set<Permission> permissions = permissionRepository.findByIds(permissionIds);

        // 验证所有请求的权限都存在
        if (permissions.size() != permissionIds.size()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("One or more permissions not found"));
        }

        role = roleService.addPermissionsToRole(role, permissions);
        role = roleRepository.save(role);

        return ResponseEntity.ok(ApiResponse.success(convertToResponse(role)));
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取角色的权限列表")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getRolePermissions(
            @PathVariable Long id
    ) {
        Optional<Role> roleOpt = roleRepository.findById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<PermissionResponse> permissions = roleOpt.get().getPermissions().stream()
                .map(this::convertToPermissionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @Operation(summary = "从角色移除权限")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removePermission(
            @PathVariable Long id,
            @PathVariable Long permissionId
    ) {
        Optional<Role> roleOpt = roleRepository.findById(id);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 验证权限存在
        Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
        if (permissionOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Permission not found"));
        }

        Role role = roleOpt.get();
        roleService.removePermissionsFromRole(role, Set.of(permissionOpt.get()));
        roleRepository.save(role);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ========== 内部辅助方法 ==========

    private RoleResponse convertToResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setIsEnabled(role.getIsEnabled());
        response.setPermissionCount(role.getPermissions().size());
        response.setUserCount(userRepository.findByRoleId(role.getId()).size());
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
        @lombok.NonNull
        private String roleCode;
        @lombok.NonNull
        private String roleName;
        private String description;
    }

    @lombok.Data
    public static class UpdateRoleRequest {
        private String roleCode;
        private String roleName;
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
        private List<Long> permissionIds;
    }
}
