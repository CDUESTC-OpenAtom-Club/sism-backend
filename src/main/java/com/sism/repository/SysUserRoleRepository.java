package com.sism.repository;

import com.sism.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {

    // ========== User Management API Methods ==========

    /**
     * Find all roles for a specific user
     */
    java.util.List<SysUserRole> findByUserId(Long userId);

    /**
     * Delete all roles for a specific user
     */
    void deleteByUserId(Long userId);

    /**
     * Delete specific role for a user
     */
    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * Check if user has specific role
     */
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}
