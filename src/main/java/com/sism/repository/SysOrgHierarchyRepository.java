package com.sism.repository;

import com.sism.entity.SysOrgHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SysOrgHierarchy entity
 * Manages organization hierarchy relationships
 */
@Repository
public interface SysOrgHierarchyRepository extends JpaRepository<SysOrgHierarchy, Long> {

    /**
     * Find hierarchy by organization ID
     */
    Optional<SysOrgHierarchy> findByOrgId(Long orgId);

    /**
     * Find all child organizations of a parent
     */
    List<SysOrgHierarchy> findByParentOrgId(Long parentOrgId);

    /**
     * Find top-level organizations (no parent)
     */
    @Query("SELECT h FROM SysOrgHierarchy h WHERE h.parentOrgId IS NULL")
    List<SysOrgHierarchy> findTopLevelOrganizations();

    /**
     * Find organizations at a specific level
     */
    List<SysOrgHierarchy> findByLevel(Integer level);

    /**
     * Find parent organization of an organization
     */
    @Query("SELECT h FROM SysOrgHierarchy h WHERE h.orgId = :orgId AND h.level = 1")
    Optional<SysOrgHierarchy> findParentOrg(@Param("orgId") Long orgId);

    /**
     * Check if organization has children
     */
    boolean existsByParentOrgId(Long parentOrgId);

    /**
     * Get the parent org ID for a given org
     */
    @Query("SELECT h.parentOrgId FROM SysOrgHierarchy h WHERE h.orgId = :orgId")
    Long getParentOrgId(@Param("orgId") Long orgId);
}
