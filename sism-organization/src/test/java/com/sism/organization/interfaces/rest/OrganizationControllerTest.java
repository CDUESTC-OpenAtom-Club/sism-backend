package com.sism.organization.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.organization.application.OrganizationApplicationService;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.interfaces.dto.OrgMapper;
import com.sism.organization.interfaces.dto.OrgResponse;
import com.sism.organization.interfaces.dto.RenameOrgRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationController Tests")
class OrganizationControllerTest {

    @Mock
    private OrganizationApplicationService organizationApplicationService;

    @Mock
    private OrgMapper orgMapper;

    @InjectMocks
    private OrganizationController organizationController;

    @Test
    @DisplayName("Should preserve first and last flags from service page result")
    void shouldPreserveFirstAndLastFlagsFromServicePageResult() {
        SysOrg org = SysOrg.create("A Org", OrgType.functional);
        org.setId(1L);

        OrgResponse response = OrgResponse.builder().id(1L).name("A Org").build();

        when(organizationApplicationService.getAllOrganizations(1, 10))
                .thenReturn(new PageResult<>(List.of(org), 1, 1, 10, 1, true, true));
        when(orgMapper.toResponseList(List.of(org))).thenReturn(List.of(response));

        ResponseEntity<ApiResponse<PageResult<OrgResponse>>> entity =
                organizationController.getAllOrganizationsPage(1, 10);

        PageResult<OrgResponse> result = entity.getBody().getData();
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertFalse(result.getItems().isEmpty());
    }

    @Test
    @DisplayName("Should rename organization using request body")
    void shouldRenameOrganizationUsingRequestBody() {
        SysOrg org = SysOrg.create("Old Org", OrgType.functional);
        org.setId(1L);
        SysOrg renamed = SysOrg.create("New Org", OrgType.functional);
        renamed.setId(1L);

        OrgResponse response = OrgResponse.builder().id(1L).name("New Org").build();

        when(organizationApplicationService.getOrganizationById(1L)).thenReturn(Optional.of(org));
        when(organizationApplicationService.renameOrganization(org, "New Org")).thenReturn(renamed);
        when(orgMapper.toResponse(renamed)).thenReturn(response);

        ResponseEntity<ApiResponse<OrgResponse>> entity =
                organizationController.renameOrganization(1L, new RenameOrgRequest("New Org"));

        assertEquals("New Org", entity.getBody().getData().getName());
    }
}
