package com.sism.repository;

import com.sism.entity.SysOrg;
import com.sism.enums.OrgType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * System Organization Repository
 * Primary repository for organization-related data access
 */
@Repository
public interface SysOrgRepository extends JpaRepository<SysOrg, Long> {

    /**
     * Find organization by name
     */
    Optional<SysOrg> findByName(String name);

    /**
     * Find all organizations by type
     */
    List<SysOrg> findByType(OrgType type);

    /**
     * Find all active organizations
     */
    List<SysOrg> findByIsActiveTrue();

    /**
     * Find all active organizations by type
     */
    List<SysOrg> findByTypeAndIsActiveTrue(OrgType type);

    /**
     * Find all organizations ordered by sort order
     */
    List<SysOrg> findAllByOrderBySortOrderAsc();

    /**
     * Find all active organizations ordered by sort order
     */
    List<SysOrg> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * Find organizations by type ordered by sort order
     */
    List<SysOrg> findByTypeOrderBySortOrderAsc(OrgType type);

    /**
     * Check if organization name exists
     */
    boolean existsByName(String name);

    /**
     * Check if organization name exists excluding specific id
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM SysOrg o WHERE o.name = :name AND o.id <> :id")
    boolean existsByNameAndIdNot(String name, Long id);
}
