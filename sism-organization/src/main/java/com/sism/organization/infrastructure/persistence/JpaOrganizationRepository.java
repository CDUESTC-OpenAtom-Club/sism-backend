package com.sism.organization.infrastructure.persistence;

import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaOrganizationRepository implements OrganizationRepository {

    private final JpaOrganizationRepositoryInternal jpaRepository;

    @Override
    public Optional<SysOrg> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<SysOrg> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(ids);
    }

    @Override
    public List<SysOrg> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<SysOrg> findByParentOrgId(Long parentOrgId) {
        return jpaRepository.findByParentOrgId(parentOrgId);
    }

    @Override
    public List<SysOrg> findByType(OrgType type) {
        return jpaRepository.findByType(type);
    }

    @Override
    public List<SysOrg> findByTypes(List<OrgType> types) {
        return jpaRepository.findByTypeIn(types);
    }

    @Override
    public List<SysOrg> findByTypesAndIsActive(List<OrgType> types, Boolean isActive) {
        return jpaRepository.findByTypeInAndIsActive(types, isActive);
    }

    @Override
    public List<SysOrg> findByIsActive(Boolean isActive) {
        return jpaRepository.findByIsActive(isActive);
    }

    @Override
    public List<SysOrg> findByLevel(Integer level) {
        return jpaRepository.findByLevel(level);
    }

    @Override
    public List<SysOrg> findByNameContaining(String name) {
        return jpaRepository.findByNameContaining(name);
    }

    @Override
    public Page<SysOrg> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public SysOrg save(SysOrg org) {
        return jpaRepository.save(org);
    }

    @Override
    public void delete(SysOrg org) {
        jpaRepository.delete(org);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public List<SysOrg> findTopLevelOrgs() {
        return jpaRepository.findByParentOrgIdIsNull();
    }
}
