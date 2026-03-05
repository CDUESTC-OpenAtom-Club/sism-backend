package com.sism.service;

import com.sism.dto.UserCreateRequest;
import com.sism.dto.UserUpdateRequest;
import com.sism.entity.SysOrg;
import com.sism.entity.SysRole;
import com.sism.entity.SysUser;
import com.sism.entity.SysUserRole;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.SysOrgRepository;
import com.sism.repository.SysRoleRepository;
import com.sism.repository.SysUserRepository;
import com.sism.repository.SysUserRoleRepository;
import com.sism.vo.UserManagementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * User Management Service
 * Handles all user management operations including CRUD, status toggle, password reset
 *
 * @author SISM Development Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final SysUserRepository userRepository;
    private final SysOrgRepository orgRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    // ========== Query Operations ==========

    /**
     * Search users with pagination and filters
     */
    @Transactional(readOnly = true)
    public Page<UserManagementVO> searchUsers(
            String username,
            String realName,
            Long orgId,
            Boolean isActive,
            Pageable pageable) {

        log.info("Searching users - username: {}, realName: {}, orgId: {}, isActive: {}",
                username, realName, orgId, isActive);

        Page<SysUser> users = userRepository.findAll(pageable);

        return users.map(this::toUserManagementVO);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserManagementVO getUserById(Long userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return toUserManagementVO(user);
    }

    // ========== CRUD Operations ==========

    /**
     * Create new user
     */
    @Transactional
    public UserManagementVO createUser(UserCreateRequest request) {
        log.info("Creating user: {}", request.getUsername());

        // Validate username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在: " + request.getUsername());
        }

        // Validate organization
        SysOrg org = orgRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrgId()));

        if (!org.getIsActive()) {
            throw new BusinessException("所选组织已被禁用");
        }

        // Validate roles
        for (Long roleId : request.getRoleIds()) {
            if (!roleRepository.existsById(roleId)) {
                throw new ResourceNotFoundException("Role", roleId);
            }
        }

        // Create user entity
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setOrg(org);
        user.setIsActive(true);

        user = userRepository.save(user);
        log.info("User created with ID: {}", user.getId());

        // Assign roles
        assignRolesToUser(user.getId(), request.getRoleIds());

        return toUserManagementVO(user);
    }

    /**
     * Update user
     */
    @Transactional
    public UserManagementVO updateUser(Long userId, UserUpdateRequest request) {
        log.info("Updating user ID: {}", userId);

        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Validate username uniqueness (exclude current user)
        // Note: username is not updatable in this implementation

        // Validate organization
        SysOrg org = orgRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", request.getOrgId()));

        if (!org.getIsActive()) {
            throw new BusinessException("所选组织已被禁用");
        }

        // Validate roles
        for (Long roleId : request.getRoleIds()) {
            if (!roleRepository.existsById(roleId)) {
                throw new ResourceNotFoundException("Role", roleId);
            }
        }

        // Update user fields
        user.setRealName(request.getRealName());
        user.setOrg(org);

        user = userRepository.save(user);
        log.info("User updated: {}", userId);

        // Update roles
        assignRolesToUser(userId, request.getRoleIds());

        return toUserManagementVO(user);
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user ID: {}", userId);

        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Delete user roles first
        userRoleRepository.deleteByUserId(userId);

        // Delete user
        userRepository.delete(user);

        log.info("User deleted: {}", userId);
    }

    /**
     * Toggle user status (active/disabled)
     */
    @Transactional
    public void toggleUserStatus(Long userId, Boolean isActive) {
        log.info("Toggling user {} status to: {}", userId, isActive);

        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setIsActive(isActive);
        userRepository.save(user);

        log.info("User {} status updated to: {}", userId, isActive);
    }

    /**
     * Reset user password
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        log.info("Resetting password for user ID: {}", userId);

        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset for user ID: {}", userId);
    }

    // ========== Helper Methods ==========

    /**
     * Assign roles to user
     */
    private void assignRolesToUser(Long userId, List<Long> roleIds) {
        // Delete existing roles
        userRoleRepository.deleteByUserId(userId);

        // Assign new roles
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setCreatedAt(OffsetDateTime.now());
            userRoleRepository.save(userRole);
        }

        log.info("Assigned {} roles to user {}", roleIds.size(), userId);
    }

    /**
     * Convert SysUser entity to UserManagementVO
     */
    private UserManagementVO toUserManagementVO(SysUser user) {
        // Get user roles
        List<SysUserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        List<UserManagementVO.RoleSummary> roles = userRoles.stream()
                .map(ur -> {
                    SysRole role = roleRepository.findById(ur.getRoleId()).orElse(null);
                    if (role != null) {
                        return UserManagementVO.RoleSummary.builder()
                                .roleId(role.getId())
                                .roleCode(role.getRoleCode())
                                .roleName(role.getRoleName())
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return UserManagementVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .orgId(user.getOrg().getId())
                .orgName(user.getOrg().getName())
                .orgType(user.getOrg().getType())
                .roles(roles)
                .status(user.getIsActive() ? "active" : "disabled")
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(null) // TODO: Add lastLoginAt field if needed
                .build();
    }
}
