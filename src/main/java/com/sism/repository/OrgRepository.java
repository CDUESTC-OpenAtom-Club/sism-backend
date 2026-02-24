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
 *
 * @deprecated Use {@link SysOrgRepository} instead. This interface is kept for backward compatibility
 * and will be removed in a future version. All functionality has been moved to SysOrgRepository.
 */
@Deprecated(since = "1.0", forRemoval = true)
@Repository
public interface OrgRepository extends JpaRepository<SysOrg, Long> {

    /**
     * @deprecated Use {@link SysOrgRepository#findByName(String)} instead
     */
    @Deprecated(forRemoval = true)
    Optional<SysOrg> findByName(String name);

    /**
     * @deprecated Use {@link SysOrgRepository#findByType(OrgType)} instead
     */
    @Deprecated(forRemoval = true)
    List<SysOrg> findByType(OrgType type);

    /**
     * @deprecated Use {@link SysOrgRepository#findByIsActiveTrue()} instead
     */
    @Deprecated(forRemoval = true)
    List<SysOrg> findByIsActiveTrue();

    /**
     * @deprecated Use {@link SysOrgRepository#findByTypeAndIsActiveTrue(OrgType)} instead
     */
    @Deprecated(forRemoval = true)
    List<SysOrg> findByTypeAndIsActiveTrue(OrgType type);

    /**
     * @deprecated Use {@link SysOrgRepository#existsByName(String)} instead
     */
    @Deprecated(forRemoval = true)
    boolean existsByName(String name);

    /**
     * @deprecated Use {@link SysOrgRepository#findAllByOrderBySortOrderAsc()} instead
     */
    @Deprecated(forRemoval = true)
    List<SysOrg> findAllByOrderBySortOrderAsc();

    /**
     * @deprecated Use {@link SysOrgRepository#findByTypeAndIsActiveTrue(OrgType)} and sort manually instead
     */
    @Deprecated(forRemoval = true)
    List<SysOrg> findByTypeOrderBySortOrderAsc(OrgType type);
}
