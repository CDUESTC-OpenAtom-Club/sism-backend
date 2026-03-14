package com.sism.iam.application.service;

import com.sism.iam.domain.Role;
import com.sism.iam.domain.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * RoleService - 角色服务
 * 处理角色管理相关业务逻辑
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    /**
     * 创建角色
     */
    @Transactional
    public Role createRole(String roleCode, String roleName, String description) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Role code is required");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role name is required");
        }

        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setDescription(description);
        role.setActive(true);

        return role;
    }

    /**
     * 给角色添加权限
     */
    @Transactional
    public Role addPermissionsToRole(Role role, Set<Permission> permissions) {
        if (role.getPermissions() == null) {
            role.setPermissions(new HashSet<>());
        }
        role.getPermissions().addAll(permissions);
        return role;
    }

    /**
     * 从角色移除权限
     */
    @Transactional
    public Role removePermissionsFromRole(Role role, Set<Permission> permissions) {
        if (role.getPermissions() != null) {
            role.getPermissions().removeAll(permissions);
        }
        return role;
    }

    /**
     * 激活角色
     */
    @Transactional
    public void activateRole(Role role) {
        role.setActive(true);
    }

    /**
     * 停用角色
     */
    @Transactional
    public void deactivateRole(Role role) {
        role.setActive(false);
    }
}
