package com.sism.organization.application;

import com.sism.exception.ConflictException;
import com.sism.shared.domain.exception.ResourceNotFoundException;
import com.sism.common.PageResult;
import com.sism.organization.application.port.OrganizationUserQueryPort;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.event.OrgCreatedEvent;
import com.sism.organization.domain.OrganizationRepository;
import com.sism.organization.interfaces.dto.OrgUserResponse;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationApplicationService Tests")
class OrganizationApplicationServiceTest {

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private EventStore eventStore;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationUserQueryPort organizationUserQueryPort;

    @Mock
    private OrganizationReferenceCheckService organizationReferenceCheckService;

    @InjectMocks
    private OrganizationApplicationService organizationApplicationService;

    @Test
    @DisplayName("Should publish created event after organization id is assigned")
    void shouldPublishCreatedEventAfterOrganizationIdAssigned() {
        SysOrg persisted = SysOrg.create("Created Org", OrgType.functional);
        persisted.setId(99L);

        when(organizationRepository.save(any(SysOrg.class))).thenReturn(persisted);

        SysOrg created = organizationApplicationService.createOrganization("Created Org", OrgType.functional);

        assertEquals(99L, created.getId());
        verify(eventStore).save(argThat(event ->
                event instanceof OrgCreatedEvent orgCreatedEvent && Long.valueOf(99L).equals(orgCreatedEvent.getOrgId())));
        verify(eventPublisher).publishAll(argThat(events ->
                events.stream()
                        .filter(OrgCreatedEvent.class::isInstance)
                        .map(OrgCreatedEvent.class::cast)
                        .map(OrgCreatedEvent::getOrgId)
                        .anyMatch(Long.valueOf(99L)::equals)));
    }

    @Test
    @DisplayName("Should resolve organization users through port without direct IAM repository dependency")
    void shouldResolveOrganizationUsersThroughPort() {
        when(organizationUserQueryPort.findUsersByOrgId(42L)).thenReturn(List.of(
                OrgUserResponse.builder()
                        .id(1L)
                        .username("alice")
                        .realName("Alice Zhang")
                        .isActive(true)
                        .build()
        ));

        List<OrgUserResponse> users = organizationApplicationService.getUsersByOrganizationId(42L);

        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).getUsername());
        verify(organizationUserQueryPort).findUsersByOrgId(42L);
    }

    @Test
    @DisplayName("Should build organization tree using parentOrgId without relying on getParentOrg")
    void shouldBuildOrganizationTreeUsingParentOrgId() {
        SysOrg root = SysOrg.create("Root Org", OrgType.functional);
        root.setId(1L);
        root.setSortOrder(0);

        SysOrg laterChild = SysOrg.create("Later Child", OrgType.academic);
        laterChild.setId(2L);
        laterChild.setParentOrgId(1L);
        laterChild.setSortOrder(5);

        SysOrg earlierChild = SysOrg.create("Earlier Child", OrgType.academic);
        earlierChild.setId(3L);
        earlierChild.setParentOrgId(1L);
        earlierChild.setSortOrder(1);

        when(organizationRepository.findByIsActive(true)).thenReturn(List.of(root, laterChild, earlierChild));

        List<SysOrg> tree = assertDoesNotThrow(() -> organizationApplicationService.getOrganizationTree());

        assertEquals(1, tree.size());
        assertEquals(1L, tree.get(0).getId());
        assertEquals(2, tree.get(0).getChildren().size());
        assertEquals(3L, tree.get(0).getChildren().get(0).getId());
        assertEquals(2L, tree.get(0).getChildren().get(1).getId());
    }

    @Test
    @DisplayName("Should update parent organization and level")
    void shouldUpdateParentOrganizationAndLevel() {
        SysOrg parent = SysOrg.create("Parent", OrgType.functional);
        parent.setId(10L);

        SysOrg child = SysOrg.create("Child", OrgType.academic);
        child.setId(11L);

        when(organizationRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(organizationRepository.save(any(SysOrg.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SysOrg updated = organizationApplicationService.updateParentOrganization(child, 10L);

        assertEquals(10L, updated.getParentOrgId());
        assertEquals(2, updated.getLevel());
        verify(organizationRepository).save(child);
    }

    @Test
    @DisplayName("Should reject circular organization hierarchy")
    void shouldRejectCircularOrganizationHierarchy() {
        SysOrg root = SysOrg.create("Root", OrgType.functional);
        root.setId(1L);
        SysOrg child = SysOrg.create("Child", OrgType.academic);
        child.setId(2L);
        child.setParentOrgId(1L);

        when(organizationRepository.findById(2L)).thenReturn(Optional.of(child));
        when(organizationRepository.findAll()).thenReturn(List.of(root, child));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> organizationApplicationService.updateParentOrganization(root, 2L)
        );

        assertEquals("Organization hierarchy cannot contain cycles", exception.getMessage());
    }

    @Test
    @DisplayName("Should return not found when parent organization is missing")
    void shouldReturnNotFoundWhenParentOrganizationIsMissing() {
        SysOrg child = SysOrg.create("Child", OrgType.academic);
        child.setId(11L);

        when(organizationRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> organizationApplicationService.updateParentOrganization(child, 10L)
        );

        assertEquals("Parent organization with id '10' not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject negative sort order values")
    void shouldRejectNegativeSortOrderValues() {
        SysOrg org = SysOrg.create("Org", OrgType.functional);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationApplicationService.updateSortOrder(org, -1)
        );

        assertEquals("Sort order must be a non-negative integer", exception.getMessage());
    }

    @Test
    @DisplayName("Should paginate organizations with 1-based page numbers")
    void shouldPaginateOrganizationsWithOneBasedPageNumbers() {
        SysOrg first = SysOrg.create("A Org", OrgType.functional);
        first.setId(1L);
        first.setSortOrder(0);

        SysOrg second = SysOrg.create("B Org", OrgType.academic);
        second.setId(2L);
        second.setSortOrder(1);

        SysOrg third = SysOrg.create("C Org", OrgType.functional);
        third.setId(3L);
        third.setSortOrder(2);

        when(organizationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 3));

        PageResult<SysOrg> result = organizationApplicationService.getAllOrganizations(1, 2);

        assertEquals(1, result.getPage());
        assertEquals(2, result.getPageSize());
        assertEquals(3, result.getTotal());
        assertEquals(2, result.getItems().size());
        assertEquals(1L, result.getItems().get(0).getId());
        assertEquals(2L, result.getItems().get(1).getId());
    }

    @Test
    @DisplayName("Should cap organization page size at 100")
    void shouldCapOrganizationPageSizeAt100() {
        SysOrg org = SysOrg.create("A Org", OrgType.functional);
        org.setId(1L);
        org.setSortOrder(0);

        when(organizationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(org), PageRequest.of(0, 100), 1));

        PageResult<SysOrg> result = organizationApplicationService.getAllOrganizations(1, 500);

        assertEquals(100, result.getPageSize());
        verify(organizationRepository).findAll(argThat(pageable -> pageable.getPageSize() == 100));
    }

    @Test
    @DisplayName("Should return optional organization when id exists")
    void shouldReturnOptionalOrganizationWhenIdExists() {
        SysOrg org = SysOrg.create("Org", OrgType.functional);
        org.setId(88L);

        when(organizationRepository.findById(88L)).thenReturn(Optional.of(org));

        assertTrue(organizationApplicationService.getOrganizationById(88L).isPresent());
        assertEquals(88L, organizationApplicationService.getOrganizationById(88L).orElseThrow().getId());
    }

    @Test
    @DisplayName("Should return only functional and academic organizations for departments")
    void shouldReturnOnlyFunctionalAndAcademicOrganizationsForDepartments() {
        SysOrg functional = SysOrg.create("Functional Org", OrgType.functional);
        functional.setId(10L);
        functional.setSortOrder(2);
        functional.setIsActive(true);

        SysOrg academic = SysOrg.create("Academic Org", OrgType.academic);
        academic.setId(11L);
        academic.setSortOrder(1);
        academic.setIsActive(true);

        SysOrg admin = SysOrg.create("Admin Org", OrgType.admin);
        admin.setId(12L);
        admin.setSortOrder(0);

        when(organizationRepository.findByTypesAndIsActive(List.of(
                OrgType.functional,
                OrgType.academic
        ), true)).thenReturn(List.of(functional, academic));

        List<SysOrg> departments = organizationApplicationService.getDepartmentOrganizations();

        assertEquals(2, departments.size());
        assertEquals(11L, departments.get(0).getId());
        assertEquals(10L, departments.get(1).getId());
        assertTrue(departments.stream().noneMatch(org -> org.getId().equals(admin.getId())));
    }

    @Test
    @DisplayName("Should filter inactive departments unless includeDisabled is requested")
    void shouldFilterInactiveDepartmentsUnlessIncludeDisabledIsRequested() {
        SysOrg activeDepartment = SysOrg.create("Active Dept", OrgType.functional);
        activeDepartment.setId(20L);
        activeDepartment.setSortOrder(0);
        activeDepartment.setIsActive(true);

        SysOrg inactiveDepartment = SysOrg.create("Inactive Dept", OrgType.academic);
        inactiveDepartment.setId(21L);
        inactiveDepartment.setSortOrder(1);
        inactiveDepartment.setIsActive(false);

        when(organizationRepository.findByTypesAndIsActive(List.of(
                OrgType.functional,
                OrgType.academic
        ), true)).thenReturn(List.of(activeDepartment));
        when(organizationRepository.findByTypes(List.of(
                OrgType.functional,
                OrgType.academic
        ))).thenReturn(List.of(activeDepartment, inactiveDepartment));

        List<SysOrg> activeOnly = organizationApplicationService.getDepartmentOrganizations();
        List<SysOrg> includingDisabled = organizationApplicationService.getDepartmentOrganizations(true);

        assertEquals(1, activeOnly.size());
        assertEquals(20L, activeOnly.get(0).getId());
        assertEquals(2, includingDisabled.size());
    }

    @Test
    @DisplayName("Should reject deactivation when organization still has active children")
    void shouldRejectDeactivationWhenOrganizationStillHasActiveChildren() {
        SysOrg parent = SysOrg.create("Parent", OrgType.functional);
        parent.setId(50L);

        SysOrg activeChild = SysOrg.create("Child", OrgType.academic);
        activeChild.setId(51L);
        activeChild.setParentOrgId(50L);
        activeChild.setIsActive(true);
        activeChild.setIsDeleted(false);

        when(organizationRepository.findByParentOrgId(50L)).thenReturn(List.of(activeChild));
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> organizationApplicationService.deactivateOrganization(parent)
        );

        assertEquals("Cannot deactivate organization with active child organizations", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject deactivation when organization still has assigned users")
    void shouldRejectDeactivationWhenOrganizationStillHasAssignedUsers() {
        SysOrg parent = SysOrg.create("Parent", OrgType.functional);
        parent.setId(50L);

        when(organizationRepository.findByParentOrgId(50L)).thenReturn(List.of());
        when(organizationUserQueryPort.findUsersByOrgId(50L)).thenReturn(List.of(
                OrgUserResponse.builder().id(1L).username("alice").build()
        ));
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> organizationApplicationService.deactivateOrganization(parent)
        );

        assertEquals("Cannot deactivate organization with assigned users", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject deactivation when organization still has active external references")
    void shouldRejectDeactivationWhenOrganizationStillHasActiveExternalReferences() {
        SysOrg org = SysOrg.create("Parent", OrgType.functional);
        org.setId(50L);

        when(organizationRepository.findByParentOrgId(50L)).thenReturn(List.of());
        when(organizationUserQueryPort.findUsersByOrgId(50L)).thenReturn(List.of());
        when(organizationReferenceCheckService.hasActiveReferences(50L)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> organizationApplicationService.deactivateOrganization(org)
        );

        assertEquals("Cannot deactivate organization with active plan, indicator, or task references", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject unsupported includeUsers tree request")
    void shouldRejectUnsupportedIncludeUsersTreeRequest() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationApplicationService.getOrganizationTree(true, false)
        );

        assertEquals("includeUsers is no longer supported for organization tree responses", exception.getMessage());
    }

    @Test
    @DisplayName("Should include disabled organizations in tree only when requested")
    void shouldIncludeDisabledOrganizationsInTreeOnlyWhenRequested() {
        SysOrg activeRoot = SysOrg.create("Active Root", OrgType.functional);
        activeRoot.setId(1L);
        activeRoot.setSortOrder(0);
        activeRoot.setIsActive(true);

        SysOrg disabledRoot = SysOrg.create("Disabled Root", OrgType.academic);
        disabledRoot.setId(2L);
        disabledRoot.setSortOrder(1);
        disabledRoot.setIsActive(false);

        when(organizationRepository.findByIsActive(true)).thenReturn(List.of(activeRoot));
        when(organizationRepository.findAll()).thenReturn(List.of(activeRoot, disabledRoot));

        List<SysOrg> treeWithoutDisabled = organizationApplicationService.getOrganizationTree(false, false);
        List<SysOrg> treeWithDisabled = organizationApplicationService.getOrganizationTree(true);

        assertEquals(1, treeWithoutDisabled.size());
        assertEquals(2, treeWithDisabled.size());
    }

    @Test
    @DisplayName("Should reject organization tree deeper than supported limit")
    void shouldRejectOrganizationTreeDeeperThanSupportedLimit() {
        List<SysOrg> deepHierarchy = new java.util.ArrayList<>();
        for (long i = 1; i <= 34; i++) {
            SysOrg org = SysOrg.create("Org " + i, OrgType.functional);
            org.setId(i);
            org.setSortOrder(0);
            if (i > 1) {
                org.setParentOrgId(i - 1);
            }
            deepHierarchy.add(org);
        }

        when(organizationRepository.findByIsActive(true)).thenReturn(deepHierarchy);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> organizationApplicationService.getOrganizationTree(false, false)
        );

        assertEquals("Organization hierarchy depth exceeds supported limit", exception.getMessage());
    }
}
