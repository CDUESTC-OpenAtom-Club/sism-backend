package com.sism.organization.interfaces.dto;

import com.sism.organization.domain.SysOrg;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between SysOrg entity and DTOs
 */
@Component
public class OrgMapper {

    /**
     * Convert SysOrg entity to OrgResponse DTO
     */
    public OrgResponse toResponse(SysOrg org) {
        if (org == null) {
            return null;
        }

        List<OrgResponse> childrenResponses = org.getChildren() != null
                ? org.getChildren().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList())
                : new ArrayList<>();

        return OrgResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .type(org.getType().toSharedOrgType())
                .isActive(org.getIsActive())
                .sortOrder(org.getSortOrder())
                .parentOrgId(org.getParentOrgId())
                .level(org.getLevel())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .children(childrenResponses)
                .orgCode(org.getOrgCode())
                .orgType(org.getOrgType())
                .build();
    }

    /**
     * Convert list of SysOrg entities to list of OrgResponse DTOs
     */
    public List<OrgResponse> toResponseList(List<SysOrg> orgs) {
        if (orgs == null) {
            return new ArrayList<>();
        }
        return orgs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert OrgRequest DTO to SysOrg entity
     * Note: This is a simple conversion for creating new organizations
     */
    public SysOrg toEntity(OrgRequest request) {
        if (request == null) {
            return null;
        }
        // Convert from shared OrgType to domain OrgType
        com.sism.organization.domain.OrgType domainType =
            com.sism.organization.domain.OrgType.fromSharedOrgType(
                request.getType()
            );
        return SysOrg.create(request.getName(), domainType);
    }

    /**
     * Update SysOrg entity from OrgRequest DTO
     */
    public void updateEntityFromRequest(SysOrg org, OrgRequest request) {
        if (org == null || request == null) {
            return;
        }
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            org.rename(request.getName());
        }
        if (request.getType() != null) {
            // Convert from shared OrgType to domain OrgType
            com.sism.organization.domain.OrgType domainType =
                com.sism.organization.domain.OrgType.fromSharedOrgType(
                    request.getType()
                );
            org.changeType(domainType);
        }
        if (request.getSortOrder() != null) {
            org.updateSortOrder(request.getSortOrder());
        }
        // Note: parentOrgId update requires separate handling to fetch parent entity
    }

    public OrgUserResponse toUserResponse(com.sism.iam.domain.User user) {
        return OrgUserResponse.fromUser(user);
    }

    public List<OrgUserResponse> toUserResponseList(List<com.sism.iam.domain.User> users) {
        if (users == null) {
            return new ArrayList<>();
        }
        return users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }
}
