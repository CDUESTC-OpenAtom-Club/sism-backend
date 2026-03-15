package com.sism.organization.application;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.enums.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationApplicationService {

    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Transactional
    public SysOrg createOrganization(String name, OrgType type) {
        SysOrg org = SysOrg.create(name, type);
        org.validate();
        org = organizationRepository.save(org);
        publishAndSaveEvents(org);
        return org;
    }

    @Transactional
    public SysOrg activateOrganization(SysOrg org) {
        org.activate();
        org = organizationRepository.save(org);
        publishAndSaveEvents(org);
        return org;
    }

    @Transactional
    public SysOrg deactivateOrganization(SysOrg org) {
        org.deactivate();
        org = organizationRepository.save(org);
        publishAndSaveEvents(org);
        return org;
    }

    @Transactional
    public SysOrg renameOrganization(SysOrg org, String newName) {
        org.rename(newName);
        org = organizationRepository.save(org);
        publishAndSaveEvents(org);
        return org;
    }

    @Transactional
    public SysOrg changeOrganizationType(SysOrg org, OrgType newType) {
        org.changeType(newType);
        org = organizationRepository.save(org);
        publishAndSaveEvents(org);
        return org;
    }

    @Transactional
    public SysOrg updateSortOrder(SysOrg org, Integer sortOrder) {
        org.updateSortOrder(sortOrder);
        org = organizationRepository.save(org);
        publishAndSaveEvents(org);
        return org;
    }

    public SysOrg getOrganizationById(Long id) {
        return organizationRepository.findById(id).orElse(null);
    }

    public List<SysOrg> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    public List<SysOrg> getOrganizationTree() {
        List<SysOrg> allOrgs = organizationRepository.findAll();
        return buildTree(allOrgs, null);
    }

    public List<SysOrg> getOrganizationTree(boolean includeUsers, boolean includeDisabled) {
        List<SysOrg> allOrgs = includeDisabled
                ? organizationRepository.findAll()
                : organizationRepository.findByIsActive(true);
        return buildTree(allOrgs, null);
    }

    public List<User> getUsersByOrganizationId(Long orgId) {
        return userRepository.findByOrgId(orgId);
    }

    private List<SysOrg> buildTree(List<SysOrg> allOrgs, Long parentId) {
        return allOrgs.stream()
                .filter(org -> {
                    Long orgParentId = org.getParentOrg() != null ? org.getParentOrg().getId() : null;
                    if (parentId == null) return orgParentId == null;
                    return parentId.equals(orgParentId);
                })
                .peek(org -> {
                    List<SysOrg> children = buildTree(allOrgs, org.getId());
                    org.getChildren().clear();
                    org.getChildren().addAll(children);
                })
                .collect(Collectors.toList());
    }

    private void publishAndSaveEvents(com.sism.shared.domain.model.base.AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();

        for (DomainEvent event : events) {
            eventStore.save(event);
        }

        eventPublisher.publishAll(events);

        aggregate.clearEvents();
    }
}
