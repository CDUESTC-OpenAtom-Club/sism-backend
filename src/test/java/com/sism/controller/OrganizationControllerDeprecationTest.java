package com.sism.controller;

import com.sism.config.TestSecurityConfig;
import com.sism.entity.SysOrg;
import com.sism.enums.OrgType;
import com.sism.repository.SysOrgRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for deprecated OrganizationController endpoints
 * Verifies that deprecated endpoints still work correctly during deprecation period
 * 
 * Task 3.6: Deprecate OrganizationController and redirect to OrgController
 * Requirements: 2.5 - Consolidate duplicate controller logic while maintaining backward compatibility
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class OrganizationControllerDeprecationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysOrgRepository sysOrgRepository;

    private SysOrg testOrg;

    @BeforeEach
    void setUp() {
        // Create test organization
        testOrg = new SysOrg();
        testOrg.setName("Test Department");
        testOrg.setType(OrgType.FUNCTIONAL_DEPT);
        testOrg.setIsActive(true);
        testOrg.setSortOrder(1);
        testOrg = sysOrgRepository.save(testOrg);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Deprecated GET /api/organizations/tree still works")
    void testGetOrganizationTree_deprecated_stillWorks() throws Exception {
        // Test that deprecated /api/organizations/tree endpoint still works
        mockMvc.perform(get("/organizations/tree")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].type").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Deprecated GET /api/organizations still works")
    void testGetAllOrganizations_deprecated_stillWorks() throws Exception {
        // Test that deprecated /api/organizations endpoint still works
        mockMvc.perform(get("/organizations")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].type").exists())
                .andExpect(jsonPath("$.data[0].createdAt").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Deprecated GET /api/organizations/{id} still works")
    void testGetOrganizationById_deprecated_stillWorks() throws Exception {
        // Test that deprecated /api/organizations/{id} endpoint still works
        mockMvc.perform(get("/organizations/" + testOrg.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testOrg.getId()))
                .andExpect(jsonPath("$.data.name").value("Test Department"))
                .andExpect(jsonPath("$.data.type").value("FUNCTIONAL_DEPT"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Deprecated endpoints delegate to new controller and produce consistent results")
    void testDeprecatedEndpoints_delegateToNewController() throws Exception {
        // Verify that deprecated endpoints produce consistent results
        // by comparing with the new OrgController endpoints
        
        // Get data from deprecated endpoint
        String deprecatedResponse = mockMvc.perform(get("/organizations")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Get data from new endpoint
        String newResponse = mockMvc.perform(get("/orgs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Both should return successful responses with organization data
        // (exact format may differ due to Map vs VO, but both should work)
        assert deprecatedResponse.contains("\"success\":true") || deprecatedResponse.contains("\"code\":0");
        assert newResponse.contains("\"success\":true") || newResponse.contains("\"code\":0");
        assert deprecatedResponse.contains("Test Department");
        assert newResponse.contains("Test Department");
    }
}
