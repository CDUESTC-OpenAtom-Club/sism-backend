package com.sism.repository;

import com.sism.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AppUser entity
 * Provides data access methods for user management
 */
@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Find user by username
     */
    Optional<AppUser> findByUsername(String username);

    /**
     * Find user by SSO ID
     */
    Optional<AppUser> findBySsoId(String ssoId);

    /**
     * Find all users by organization ID
     */
    List<AppUser> findByOrg_OrgId(Long orgId);

    /**
     * Find all active users
     */
    List<AppUser> findByIsActiveTrue();

    /**
     * Find all active users by organization ID
     */
    List<AppUser> findByOrg_OrgIdAndIsActiveTrue(Long orgId);

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
    @Query("SELECT u FROM AppUser u WHERE u.realName LIKE %:realName%")
    List<AppUser> findByRealNameContaining(@Param("realName") String realName);

    /**
     * Find active users by organization type
     */
    @Query("SELECT u FROM AppUser u WHERE u.org.orgType = :orgType AND u.isActive = true")
    List<AppUser> findActiveUsersByOrgType(@Param("orgType") com.sism.enums.OrgType orgType);
}
