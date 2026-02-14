package com.sism.service;

import com.sism.entity.SysOrg;
import com.sism.enums.OrgType;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.SysOrgRepository;
import com.sism.vo.OrgTreeVO;
import com.sism.vo.SysOrgVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for organization management
 * Provides methods for querying organizations with flat structure
 * 
 * Requirements: 8.2, 8.3
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgService {

    private final SysOrgRepository orgRepository;

    /**
     * Get organization by ID
     * 
     * @param orgId organization ID
     * @return organization entity
     * @throws ResourceNotFoundException if organization not found
     */
    public SysOrg getOrgById(Long orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));
    }

    /**
     * Get all active organizations
     * 
     * @return list of active organizations
     */
    public List<SysOrgVO> getAllActiveOrgs() {
        return orgRepository.findByIsActiveTrue().stream()
                .map(this::toOrgVO)
                .collect(Collectors.toList());
    }

    /**
     * Get organizations by type
     * Requirements: 8.2 - Display department list categorized by organization type
     * 
     * @param orgType organization type filter (optional)
     * @return list of organizations matching the type
     */
    public List<SysOrgVO> getOrgsByType(OrgType orgType) {
        List<SysOrg> orgs;
        if (orgType != null) {
            orgs = orgRepository.findByTypeAndIsActiveTrue(orgType);
        } else {
            orgs = orgRepository.findByIsActiveTrueOrderBySortOrderAsc();
        }
        return orgs.stream()
                .map(this::toOrgVO)
                .collect(Collectors.toList());
    }

    /**
     * Check if organization name already exists
     * 
     * @param orgName organization name to check
     * @return true if name exists, false otherwise
     */
    public boolean isOrgNameExists(String orgName) {
        return orgRepository.existsByName(orgName);
    }

    /**
     * Check if organization name exists excluding a specific org ID
     * Used for update operations
     * 
     * @param orgName organization name to check
     * @param excludeOrgId organization ID to exclude from check
     * @return true if name exists for other organizations
     */
    public boolean isOrgNameExistsExcluding(String orgName, Long excludeOrgId) {
        return orgRepository.existsByNameAndIdNot(orgName, excludeOrgId);
    }

    /**
     * Convert SysOrg entity to SysOrgVO
     */
    private SysOrgVO toOrgVO(SysOrg org) {
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

    /**
     * Get organization hierarchy tree (flat structure, no actual hierarchy)
     * Returns all organizations as a flat list wrapped in tree structure
     * 
     * @return list of organization tree nodes
     */
    public List<OrgTreeVO> getOrgHierarchy() {
        return orgRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toOrgTreeVO)
                .collect(Collectors.toList());
    }

    /**
     * Get organization hierarchy starting from a specific organization
     * Since structure is flat, just returns the single organization
     * 
     * @param orgId organization ID
     * @return organization tree node
     */
    public OrgTreeVO getOrgHierarchyFrom(Long orgId) {
        SysOrg org = getOrgById(orgId);
        return toOrgTreeVO(org);
    }

    /**
     * Get all descendant organization IDs
     * Since structure is flat (no hierarchy), returns empty list
     * 
     * @param orgId organization ID
     * @return empty list (no descendants in flat structure)
     */
    public List<Long> getDescendantOrgIds(Long orgId) {
        // Verify org exists
        getOrgById(orgId);
        // Return empty list since there's no hierarchy
        return List.of();
    }

    /**
     * Convert SysOrg entity to OrgTreeVO
     */
    private OrgTreeVO toOrgTreeVO(SysOrg org) {
        OrgTreeVO vo = new OrgTreeVO();
        vo.setOrgId(org.getId());
        vo.setOrgName(org.getName());
        vo.setOrgType(org.getType());
        vo.setParentOrgId(null); // Flat structure, no parent
        vo.setSortOrder(org.getSortOrder());
        vo.setChildren(List.of()); // Flat structure, no children
        return vo;
    }
}
