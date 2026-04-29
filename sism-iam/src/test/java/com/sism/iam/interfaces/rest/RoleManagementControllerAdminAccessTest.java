package com.sism.iam.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.shared.application.dto.CurrentUser;
import com.sism.iam.application.service.RoleManagementService;
import com.sism.iam.domain.access.Role;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleManagementController admin org access tests")
class RoleManagementControllerAdminAccessTest {

    @Mock
    private RoleManagementService roleManagementService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private CurrentUser currentUser;

    @Test
    @DisplayName("listRoles should allow admin org reporter account")
    void listRolesShouldAllowAdminOrgReporterAccount() {
        RoleManagementController controller = new RoleManagementController(roleManagementService, organizationRepository);
        SysOrg adminOrg = SysOrg.create("战略发展部", OrgType.admin);
        adminOrg.setId(35L);
        Role role = new Role();
        role.setId(1L);
        role.setRoleCode("ROLE_REPORTER");
        role.setRoleName("填报人");

        when(currentUser.getOrgId()).thenReturn(35L);
        when(organizationRepository.findById(35L)).thenReturn(Optional.of(adminOrg));
        when(roleManagementService.findRoles(PageRequest.of(0, 100))).thenReturn(new PageImpl<>(java.util.List.of(role), PageRequest.of(0, 100), 1));
        when(roleManagementService.countUsersByRoleIds(Set.of(1L))).thenReturn(Map.of(1L, 1L));
        when(roleManagementService.countPermissionsByRoleIds(Set.of(1L))).thenReturn(Map.of(1L, 0L));

        ResponseEntity<ApiResponse<PageResult<RoleManagementController.RoleResponse>>> response =
                controller.listRoles(currentUser, 0, 100);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getData().getItems().size());
    }

    @Test
    @DisplayName("listRoles should deny non-admin org users")
    void listRolesShouldDenyNonAdminOrgUsers() {
        RoleManagementController controller = new RoleManagementController(roleManagementService, organizationRepository);
        SysOrg nonAdminOrg = SysOrg.create("教务处", OrgType.functional);
        nonAdminOrg.setId(44L);

        when(currentUser.getOrgId()).thenReturn(44L);
        when(organizationRepository.findById(44L)).thenReturn(Optional.of(nonAdminOrg));

        ResponseEntity<ApiResponse<PageResult<RoleManagementController.RoleResponse>>> response =
                controller.listRoles(currentUser, 0, 100);

        assertEquals(403, response.getStatusCodeValue());
        verify(roleManagementService, never()).findRoles(PageRequest.of(0, 100));
    }
}
