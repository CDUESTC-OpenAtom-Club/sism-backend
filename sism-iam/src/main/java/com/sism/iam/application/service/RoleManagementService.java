package com.sism.iam.application.service;

import com.sism.iam.domain.Permission;
import com.sism.iam.domain.Role;
import com.sism.iam.domain.repository.PermissionRepository;
import com.sism.iam.domain.repository.RoleRepository;
import com.sism.iam.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RoleManagementService - 角色管理应用服务
 * 将角色查询、校验和持久化收口到应用层，避免 Controller 直连仓储。
 */
@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final RoleService roleService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public Page<Role> findRoles(Pageable pageable) {
        return roleRepository.findAll(pageable);
    }

    public Optional<Role> findRoleById(Long id) {
        return roleRepository.findById(id);
    }

    public long countUsersByRoleId(Long roleId) {
        return userRepository.findByRoleId(roleId).size();
    }

    @Transactional(readOnly = true)
    public int countPermissionsByRoleId(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        return role.getPermissions() == null ? 0 : role.getPermissions().size();
    }

    public java.util.Map<Long, Long> countUsersByRoleIds(Set<Long> roleIds) {
        return userRepository.countUsersByRoleIds(roleIds);
    }

    public boolean roleCodeExists(String roleCode) {
        return roleCode != null && roleRepository.findByRoleCode(roleCode).isPresent();
    }

    public boolean roleNameExists(String roleName) {
        return roleName != null && roleRepository.findByRoleName(roleName).isPresent();
    }

    @Transactional
    public Role createRole(String roleCode, String roleName, String description) {
        if (roleCodeExists(roleCode)) {
            throw new IllegalArgumentException("Role code already exists");
        }
        if (roleNameExists(roleName)) {
            throw new IllegalArgumentException("Role name already exists");
        }
        return roleService.createRole(roleCode, roleName, description);
    }

    @Transactional
    public Role updateRole(Long id, String roleCode, String roleName, String description) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        if (roleCode != null && !roleCode.equals(role.getRoleCode()) && roleCodeExists(roleCode)) {
            throw new IllegalArgumentException("Role code already exists");
        }
        if (roleName != null && !roleName.equals(role.getRoleName()) && roleNameExists(roleName)) {
            throw new IllegalArgumentException("Role name already exists");
        }

        if (roleCode != null) {
            role.setRoleCode(roleCode);
        }
        if (roleName != null) {
            role.setRoleName(roleName);
        }
        if (description != null) {
            role.setDescription(description);
        }

        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        if (!userRepository.findByRoleId(id).isEmpty()) {
            throw new IllegalStateException("Role is still in use by users");
        }
        roleRepository.delete(role);
    }

    @Transactional
    public Role assignPermissions(Long id, Set<Long> permissionIds) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        Set<Permission> permissions = permissionRepository.findByIds(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("One or more permissions not found");
        }

        role = roleService.addPermissionsToRole(role, permissions);
        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<Permission> getRolePermissions(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        return role.getPermissions() == null ? List.of() : List.copyOf(role.getPermissions());
    }

    @Transactional
    public void removePermission(Long id, Long permissionId) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));
        roleService.removePermissionsFromRole(role, Set.of(permission));
        roleRepository.save(role);
    }
}
