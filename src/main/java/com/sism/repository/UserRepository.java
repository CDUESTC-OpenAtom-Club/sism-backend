package com.sism.repository;

import com.sism.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SysUser entity
 * Provides data access methods for user management
 */
@Repository
public interface UserRepository extends JpaRepository<SysUser, Long> {

    /**
     * Find user by username
     */
    Optional<SysUser> findByUsername(String username);

    /**
     * Find user by SSO ID
     */
    Optional<SysUser> findBySsoId(String ssoId);

    /**
     * Find all users by organization ID
     */
    List<SysUser> findByOrg_Id(Long orgId);

    /**
     * Find all active users
     */
    List<SysUser> findByIsActiveTrue();

    /**
     * Find all active users by organization ID
     */
    List<SysUser> findByOrg_IdAndIsActiveTrue(Long orgId);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if SSO ID exists
     */
    boolean existsBySsoId(String ssoId);

    /**
     * Find users by real name (partial match)
     */
    @Query("SELECT u FROM SysUser u WHERE u.realName LIKE %:realName%")
    List<SysUser> findByRealNameContaining(@Param("realName") String realName);

    /**
     * Find active users by organization type
     */
    @Query("SELECT u FROM SysUser u WHERE u.org.type = :orgType AND u.isActive = true")
    List<SysUser> findActiveUsersByOrgType(@Param("orgType") com.sism.enums.OrgType orgType);
}
