package com.sism.organization.infrastructure.persistence;

import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaOrganizationRepositoryInternal extends JpaRepository<SysOrg, Long> {
    List<SysOrg> findByParentOrgId(Long parentOrgId);
    List<SysOrg> findByType(OrgType type);

    List<SysOrg> findByTypeIn(List<OrgType> types);
    List<SysOrg> findByTypeInAndIsActive(List<OrgType> types, Boolean isActive);
    List<SysOrg> findByIsActive(Boolean isActive);
    List<SysOrg> findByLevel(Integer level);
    List<SysOrg> findByNameContaining(String name);
    List<SysOrg> findByParentOrgIdIsNull();
}
