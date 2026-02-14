package com.sism.repository;

import com.sism.entity.SysOrg;
import com.sism.enums.OrgType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SysOrg entity
 * Provides data access methods for organization management
 */
@Repository
public interface OrgRepository extends JpaRepository<SysOrg, Long> {

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
     * Find all organizations by type and active status
     */
    List<SysOrg> findByTypeAndIsActiveTrue(OrgType type);

    /**
     * Check if organization exists by name
     */
    boolean existsByName(String name);

    /**
     * Find organizations ordered by sort order
     */
    List<SysOrg> findAllByOrderBySortOrderAsc();

    /**
     * Find organizations by type ordered by sort order
     */
    List<SysOrg> findByTypeOrderBySortOrderAsc(OrgType type);
}
