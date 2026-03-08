package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.enums.OrgType;
import com.sism.exception.ResourceNotFoundException;
import com.sism.service.OrgService;
import com.sism.vo.SysOrgVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Test for OrgController enhancements (Task 3.7)
 * Verifies the new GET /api/orgs/{orgId} endpoint
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrgController Enhancement Tests")
class OrgControllerEnhancementTest {

    @Mock
    private OrgService orgService;

    @InjectMocks
    private OrgController orgController;

    private List<SysOrgVO> mockOrgs;

    @BeforeEach
    void setUp() {
        // Create mock organizations
        SysOrgVO org1 = new SysOrgVO();
        org1.setId(1L);
        org1.setName("战略发展部");
        org1.setType(OrgType.STRATEGY_DEPT);
        org1.setIsActive(true);
        org1.setSortOrder(1);
        org1.setCreatedAt(LocalDateTime.now());
        org1.setUpdatedAt(LocalDateTime.now());

        SysOrgVO org2 = new SysOrgVO();
        org2.setId(2L);
        org2.setName("信息技术部");
        org2.setType(OrgType.FUNCTIONAL_DEPT);
        org2.setIsActive(true);
        org2.setSortOrder(2);
        org2.setCreatedAt(LocalDateTime.now());
        org2.setUpdatedAt(LocalDateTime.now());

        mockOrgs = Arrays.asList(org1, org2);
    }

    @Test
    @DisplayName("GET /api/orgs/{orgId} - Should return organization by ID")
    void testGetOrgById_Success() {
        // Given
        Long orgId = 1L;
        when(orgService.getOrgsByType(null)).thenReturn(mockOrgs);

        // When
        ResponseEntity<ApiResponse<SysOrgVO>> response = orgController.getOrgById(orgId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getId()).isEqualTo(orgId);
        assertThat(response.getBody().getData().getName()).isEqualTo("战略发展部");
        assertThat(response.getBody().getData().getType()).isEqualTo(OrgType.STRATEGY_DEPT);

        verify(orgService, times(1)).getOrgsByType(null);
    }

    @Test
    @DisplayName("GET /api/orgs/{orgId} - Should throw ResourceNotFoundException when org not found")
    void testGetOrgById_NotFound() {
        // Given
        Long nonExistentOrgId = 999L;
        when(orgService.getOrgsByType(null)).thenReturn(mockOrgs);

        // When & Then
        assertThatThrownBy(() -> orgController.getOrgById(nonExistentOrgId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization")
                .hasMessageContaining("999");

        verify(orgService, times(1)).getOrgsByType(null);
    }

    @Test
    @DisplayName("GET /api/orgs/{orgId} - Should return correct organization when multiple exist")
    void testGetOrgById_MultipleOrgs() {
        // Given
        Long orgId = 2L;
        when(orgService.getOrgsByType(null)).thenReturn(mockOrgs);

        // When
        ResponseEntity<ApiResponse<SysOrgVO>> response = orgController.getOrgById(orgId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getId()).isEqualTo(orgId);
        assertThat(response.getBody().getData().getName()).isEqualTo("信息技术部");
        assertThat(response.getBody().getData().getType()).isEqualTo(OrgType.FUNCTIONAL_DEPT);

        verify(orgService, times(1)).getOrgsByType(null);
    }

    @Test
    @DisplayName("Verify OrgController uses strongly-typed VOs, not Map<String, Object>")
    void testStronglyTypedResponses() {
        // This test verifies at compile-time that all methods return strongly-typed VOs
        // If this compiles, it proves we're not using Map<String, Object>
        
        Long orgId = 1L;
        when(orgService.getOrgsByType(null)).thenReturn(mockOrgs);

        ResponseEntity<ApiResponse<SysOrgVO>> response = orgController.getOrgById(orgId);
        
        // The fact that this compiles with SysOrgVO type proves strong typing
        SysOrgVO org = response.getBody().getData();
        assertThat(org).isNotNull();
        assertThat(org.getId()).isNotNull();
        assertThat(org.getName()).isNotNull();
        assertThat(org.getType()).isNotNull();
    }
}
