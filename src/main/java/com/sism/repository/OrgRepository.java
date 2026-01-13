package com.sism.repository;

import com.sism.entity.Org;
import com.sism.enums.OrgType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Org entity
 * Provides data access methods for organization management
 */
@Repository
public interface OrgRepository extends JpaRepository<Org, Long> {

    /**
     * Find organization by name
     */
    Optional<Org> findByOrgName(String orgName);

    /**
     * Find all organizations by type
     */
    List<Org> findByOrgType(OrgType orgType);

    /**
     * Find all active organizations
     */
    List<Org> findByIsActiveTrue();

    /**
     * Find all organizations by type and active status
     */
    List<Org> findByOrgTypeAndIsActiveTrue(OrgType orgType);

    /**
     * Find child organizations by parent ID
     */
    List<Org> findByParentOrg_OrgId(Long parentOrgId);

    /**
     * Find all root organizations (no parent)
     */
    List<Org> findByParentOrgIsNull();

    /**
     * Check if organization exists by name
     */
    boolean existsByOrgName(String orgName);

    /**
     * Find organizations ordered by sort order
     */
    List<Org> findAllByOrderBySortOrderAsc();

    /**
     * Find organizations by type ordered by sort order
     */
    List<Org> findByOrgTypeOrderBySortOrderAsc(OrgType orgType);
}
