package com.sism.organization.application;

import com.sism.organization.application.port.OrganizationUserQueryPort;
import com.sism.common.PageResult;
import com.sism.exception.ConflictException;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.event.OrgCreatedEvent;
import com.sism.organization.domain.OrganizationRepository;
import com.sism.shared.domain.exception.ResourceNotFoundException;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationApplicationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TREE_DEPTH = 32;
    private static final Comparator<SysOrg> ORG_SORT_COMPARATOR = Comparator
            .comparing(SysOrg::getSortOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(SysOrg::getName, Comparator.nullsLast(String::compareToIgnoreCase))
            .thenComparing(SysOrg::getId, Comparator.nullsLast(Long::compareTo));

    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUserQueryPort organizationUserQueryPort;
    private final OrganizationReferenceCheckService organizationReferenceCheckService;

    @Transactional
    public SysOrg createOrganization(String name, OrgType type) {
        SysOrg org = SysOrg.create(name, type);
        org.validate();
        org = organizationRepository.save(org);
        org.registerCreatedEvent();
        publishAndSaveEvents(org);
        return org;
    }

    @Transactional
    public SysOrg createOrganization(String name, OrgType type, Long parentOrgId, Integer sortOrder) {
        SysOrg org = SysOrg.create(name, type);
        if (sortOrder != null) {
            org.updateSortOrder(sortOrder);
        }
        if (parentOrgId != null) {
            SysOrg parentOrg = organizationRepository.findById(parentOrgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent organization", parentOrgId));
            org.updateParent(parentOrg);
        }
        org.validate();
        org = organizationRepository.save(org);
        org.registerCreatedEvent();
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
        validateCanDeactivate(org);
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

    @Transactional
    public SysOrg updateParentOrganization(SysOrg org, Long parentOrgId) {
        SysOrg parentOrg = null;
        if (parentOrgId != null) {
            parentOrg = organizationRepository.findById(parentOrgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent organization", parentOrgId));
            validateNoCircularHierarchy(org, parentOrg);
        }
        org.updateParent(parentOrg);
        org.validate();
        org = organizationRepository.save(org);
        publishAndSaveEvents(org);
        return org;
    }

    public java.util.Optional<SysOrg> getOrganizationById(Long id) {
        return organizationRepository.findById(id);
    }

    public List<SysOrg> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    public PageResult<SysOrg> getAllOrganizations(int pageNum, int pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int zeroBasedPage = safePageNum - 1;

        Sort sort = Sort.by(
                Sort.Order.asc("sortOrder"),
                Sort.Order.by("name").ignoreCase(),
                Sort.Order.asc("id")
        );

        Page<SysOrg> page = organizationRepository.findAll(PageRequest.of(zeroBasedPage, safePageSize, sort));

        int totalPages = page.getTotalPages();
        boolean isFirst = zeroBasedPage == 0;
        boolean isLast = totalPages <= 1 || zeroBasedPage >= totalPages - 1;

        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                safePageNum,
                safePageSize,
                totalPages,
                isFirst,
                isLast
        );
    }

    public List<SysOrg> getDepartmentOrganizations() {
        return getDepartmentOrganizations(false);
    }

    public List<SysOrg> getDepartmentOrganizations(boolean includeDisabled) {
        List<OrgType> departmentTypes = List.of(OrgType.functional, OrgType.academic);
        List<SysOrg> departments = includeDisabled
                ? organizationRepository.findByTypes(departmentTypes)
                : organizationRepository.findByTypesAndIsActive(departmentTypes, true);
        return sortOrganizations(departments);
    }

    public List<SysOrg> getOrganizationTree() {
        return getOrganizationTree(false);
    }

    public List<SysOrg> getOrganizationTree(boolean includeDisabled) {
        List<SysOrg> allOrgs = includeDisabled
                ? organizationRepository.findAll()
                : organizationRepository.findByIsActive(true);
        return buildTree(allOrgs);
    }

    public List<SysOrg> getOrganizationTree(boolean includeUsers, boolean includeDisabled) {
        if (includeUsers) {
            throw new IllegalArgumentException("includeUsers is no longer supported for organization tree responses");
        }
        return getOrganizationTree(includeDisabled);
    }

    public List<com.sism.organization.interfaces.dto.OrgUserResponse> getUsersByOrganizationId(Long orgId) {
        return organizationUserQueryPort.findUsersByOrgId(orgId);
    }

    private List<SysOrg> buildTree(List<SysOrg> allOrgs) {
        Map<Long, List<SysOrg>> childrenByParentId = new LinkedHashMap<>();
        List<SysOrg> roots = new ArrayList<>();

        for (SysOrg org : allOrgs) {
            Long parentOrgId = org.getParentOrgId();
            if (parentOrgId == null) {
                roots.add(org);
            } else {
                childrenByParentId.computeIfAbsent(parentOrgId, ignored -> new ArrayList<>()).add(org);
            }
        }

        sortOrganizationsInPlace(roots);
        populateChildren(roots, childrenByParentId, 1);
        return roots;
    }

    private void populateChildren(List<SysOrg> parents, Map<Long, List<SysOrg>> childrenByParentId, int depth) {
        if (depth > MAX_TREE_DEPTH) {
            throw new ConflictException("Organization hierarchy depth exceeds supported limit");
        }
        for (SysOrg parent : parents) {
            List<SysOrg> children = new ArrayList<>(childrenByParentId.getOrDefault(parent.getId(), List.of()));
            sortOrganizationsInPlace(children);
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().clear();
            parent.getChildren().addAll(children);
            populateChildren(children, childrenByParentId, depth + 1);
        }
    }

    private void publishAndSaveEvents(com.sism.shared.domain.model.base.AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();

        for (DomainEvent event : events) {
            eventStore.save(event);
        }

        eventPublisher.publishAll(events);

        aggregate.clearEvents();
    }

    private void validateNoCircularHierarchy(SysOrg org, SysOrg candidateParent) {
        if (org.getId() == null || candidateParent == null) {
            return;
        }

        Map<Long, Long> parentByOrgId = new LinkedHashMap<>();
        for (SysOrg existingOrg : organizationRepository.findAll()) {
            parentByOrgId.put(existingOrg.getId(), existingOrg.getParentOrgId());
        }

        Long currentParentId = candidateParent.getId();
        while (currentParentId != null) {
            if (currentParentId.equals(org.getId())) {
                throw new ConflictException("Organization hierarchy cannot contain cycles");
            }
            currentParentId = parentByOrgId.get(currentParentId);
        }
    }

    private void validateCanDeactivate(SysOrg org) {
        Long orgId = org.getId();
        if (orgId == null) {
            return;
        }

        boolean hasActiveChildren = organizationRepository.findByParentOrgId(orgId).stream()
                .anyMatch(child -> Boolean.TRUE.equals(child.getIsActive()) && Boolean.FALSE.equals(child.getIsDeleted()));
        if (hasActiveChildren) {
            throw new ConflictException("Cannot deactivate organization with active child organizations");
        }

        if (!organizationUserQueryPort.findUsersByOrgId(orgId).isEmpty()) {
            throw new ConflictException("Cannot deactivate organization with assigned users");
        }

        if (organizationReferenceCheckService.hasActiveReferences(orgId)) {
            throw new ConflictException("Cannot deactivate organization with active plan, indicator, or task references");
        }
    }

    private List<SysOrg> sortOrganizations(List<SysOrg> orgs) {
        return orgs.stream()
                .sorted(ORG_SORT_COMPARATOR)
                .collect(Collectors.toList());
    }

    private void sortOrganizationsInPlace(List<SysOrg> orgs) {
        orgs.sort(ORG_SORT_COMPARATOR);
    }

    private int normalizePageNum(int pageNum) {
        return Math.max(pageNum, 1);
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }
}
