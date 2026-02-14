package com.sism.service;

import com.sism.entity.SysOrg;
import com.sism.enums.OrgType;
import com.sism.exception.BusinessException;
import com.sism.repository.SysOrgRepository;
import com.sism.vo.SysOrgVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * System Organization Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOrgService {

    private final SysOrgRepository sysOrgRepository;

    /**
     * Get all organizations
     */
    public List<SysOrgVO> getAllOrganizations() {
        return sysOrgRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active organizations
     */
    public List<SysOrgVO> getActiveOrganizations() {
        return sysOrgRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * Get organizations by type
     */
    public List<SysOrgVO> getOrganizationsByType(OrgType type) {
        return sysOrgRepository.findByTypeAndIsActiveTrue(type)
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * Get organization by ID
     */
    public SysOrgVO getOrganizationById(Long id) {
        SysOrg org = sysOrgRepository.findById(id)
                .orElseThrow(() -> new BusinessException("组织不存在: " + id));
        return convertToVO(org);
    }

    /**
     * Get organization by name
     */
    public SysOrgVO getOrganizationByName(String name) {
        SysOrg org = sysOrgRepository.findByName(name)
                .orElseThrow(() -> new BusinessException("组织不存在: " + name));
        return convertToVO(org);
    }

    /**
     * Create organization
     */
    @Transactional
    public SysOrgVO createOrganization(String name, OrgType type, Integer sortOrder) {
        // Check if name already exists
        if (sysOrgRepository.existsByName(name)) {
            throw new BusinessException("组织名称已存在: " + name);
        }

        SysOrg org = new SysOrg();
        org.setName(name);
        org.setType(type);
        org.setSortOrder(sortOrder != null ? sortOrder : 0);
        org.setIsActive(true);

        SysOrg saved = sysOrgRepository.save(org);
        log.info("Created organization: id={}, name={}, type={}", saved.getId(), saved.getName(), saved.getType());

        return convertToVO(saved);
    }

    /**
     * Update organization
     */
    @Transactional
    public SysOrgVO updateOrganization(Long id, String name, OrgType type, Integer sortOrder, Boolean isActive) {
        SysOrg org = sysOrgRepository.findById(id)
                .orElseThrow(() -> new BusinessException("组织不存在: " + id));

        // Check if name already exists (excluding current org)
        if (name != null && !name.equals(org.getName())) {
            if (sysOrgRepository.existsByNameAndIdNot(name, id)) {
                throw new BusinessException("组织名称已存在: " + name);
            }
            org.setName(name);
        }

        if (type != null) {
            org.setType(type);
        }

        if (sortOrder != null) {
            org.setSortOrder(sortOrder);
        }

        if (isActive != null) {
            org.setIsActive(isActive);
        }

        SysOrg updated = sysOrgRepository.save(org);
        log.info("Updated organization: id={}, name={}, type={}", updated.getId(), updated.getName(), updated.getType());

        return convertToVO(updated);
    }

    /**
     * Delete organization (soft delete by setting isActive to false)
     */
    @Transactional
    public void deleteOrganization(Long id) {
        SysOrg org = sysOrgRepository.findById(id)
                .orElseThrow(() -> new BusinessException("组织不存在: " + id));

        org.setIsActive(false);
        sysOrgRepository.save(org);

        log.info("Deleted (soft) organization: id={}, name={}", id, org.getName());
    }

    /**
     * Convert entity to VO
     */
    private SysOrgVO convertToVO(SysOrg org) {
        SysOrgVO vo = new SysOrgVO();
        vo.setId(org.getId());
        vo.setName(org.getName());
        vo.setType(org.getType());
        vo.setTypeDisplay(getTypeDisplay(org.getType()));
        vo.setIsActive(org.getIsActive());
        vo.setSortOrder(org.getSortOrder());
        vo.setCreatedAt(org.getCreatedAt());
        vo.setUpdatedAt(org.getUpdatedAt());
        return vo;
    }

    /**
     * Get type display name
     */
    private String getTypeDisplay(OrgType type) {
        return switch (type) {
            case STRATEGY_DEPT -> "战略发展部";
            case FUNCTIONAL_DEPT, FUNCTION_DEPT -> "职能部门";
            case COLLEGE -> "二级学院";
            case SCHOOL -> "学校";
            case DIVISION -> "部门";
            case OTHER -> "其他";
        };
    }
}
