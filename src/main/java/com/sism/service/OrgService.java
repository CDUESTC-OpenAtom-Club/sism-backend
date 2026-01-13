package com.sism.service;

import com.sism.entity.Org;
import com.sism.enums.OrgType;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.OrgRepository;
import com.sism.vo.OrgTreeVO;
import com.sism.vo.OrgVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for organization management
 * Provides methods for querying organizations and building hierarchy trees
 * 
 * Requirements: 8.2, 8.3
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgService {

    private final OrgRepository orgRepository;

    /**
     * Get organization by ID
     * 
     * @param orgId organization ID
     * @return organization entity
     * @throws ResourceNotFoundException if organization not found
     */
    public Org getOrgById(Long orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));
    }

    /**
     * Get all active organizations
     * 
     * @return list of active organizations
     */
    public List<OrgVO> getAllActiveOrgs() {
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
    public List<OrgVO> getOrgsByType(OrgType orgType) {
        List<Org> orgs;
        if (orgType != null) {
            orgs = orgRepository.findByOrgTypeAndIsActiveTrue(orgType);
        } else {
            orgs = orgRepository.findByIsActiveTrue();
        }
        return orgs.stream()
                .map(this::toOrgVO)
                .collect(Collectors.toList());
    }


    /**
     * Build organization hierarchy tree
     * Requirements: 8.3 - View indicator details with organization hierarchy
     * 
     * @return list of root organizations with nested children
     */
    public List<OrgTreeVO> getOrgHierarchy() {
        // Get all root organizations (no parent)
        List<Org> rootOrgs = orgRepository.findByParentOrgIsNull();
        
        return rootOrgs.stream()
                .filter(Org::getIsActive)
                .map(this::buildOrgTree)
                .collect(Collectors.toList());
    }

    /**
     * Get organization hierarchy starting from a specific organization
     * 
     * @param orgId starting organization ID
     * @return organization tree starting from the specified org
     */
    public OrgTreeVO getOrgHierarchyFrom(Long orgId) {
        Org org = getOrgById(orgId);
        return buildOrgTree(org);
    }

    /**
     * Get all descendant organization IDs for a given organization
     * Useful for filtering data by organizational hierarchy
     * Requirements: 8.4 - Support data filtering by organizational hierarchy
     * 
     * @param orgId parent organization ID
     * @return list of all descendant organization IDs (including the parent)
     */
    public List<Long> getDescendantOrgIds(Long orgId) {
        List<Long> result = new ArrayList<>();
        result.add(orgId);
        collectDescendantIds(orgId, result);
        return result;
    }

    /**
     * Recursively collect descendant organization IDs
     */
    private void collectDescendantIds(Long parentId, List<Long> result) {
        List<Org> children = orgRepository.findByParentOrg_OrgId(parentId);
        for (Org child : children) {
            if (child.getIsActive()) {
                result.add(child.getOrgId());
                collectDescendantIds(child.getOrgId(), result);
            }
        }
    }

    /**
     * Build organization tree recursively
     */
    private OrgTreeVO buildOrgTree(Org org) {
        OrgTreeVO treeVO = new OrgTreeVO();
        treeVO.setOrgId(org.getOrgId());
        treeVO.setOrgName(org.getOrgName());
        treeVO.setOrgType(org.getOrgType());
        treeVO.setParentOrgId(org.getParentOrg() != null ? org.getParentOrg().getOrgId() : null);
        treeVO.setSortOrder(org.getSortOrder());
        
        // Recursively build children
        List<Org> children = orgRepository.findByParentOrg_OrgId(org.getOrgId());
        List<OrgTreeVO> childTrees = children.stream()
                .filter(Org::getIsActive)
                .map(this::buildOrgTree)
                .collect(Collectors.toList());
        treeVO.setChildren(childTrees);
        
        return treeVO;
    }

    /**
     * Convert Org entity to OrgVO
     */
    private OrgVO toOrgVO(Org org) {
        OrgVO vo = new OrgVO();
        vo.setOrgId(org.getOrgId());
        vo.setOrgName(org.getOrgName());
        vo.setOrgType(org.getOrgType());
        vo.setParentOrgId(org.getParentOrg() != null ? org.getParentOrg().getOrgId() : null);
        vo.setParentOrgName(org.getParentOrg() != null ? org.getParentOrg().getOrgName() : null);
        vo.setIsActive(org.getIsActive());
        vo.setSortOrder(org.getSortOrder());
        return vo;
    }
}
