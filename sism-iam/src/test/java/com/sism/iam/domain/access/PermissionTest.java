package com.sism.iam.domain.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Permission domain entity
 * Tests fine-grained permission model
 */
@DisplayName("Permission Entity Tests")
class PermissionTest {

    private Permission permission;

    @BeforeEach
    void setUp() {
        permission = new Permission();
    }

    @Test
    @DisplayName("Should create Permission with required fields")
    void shouldCreatePermissionWithRequiredFields() {
        permission.setId(1L);
        permission.setPermissionCode("VIEW_INDICATORS");
        permission.setPermissionName("View Indicators");
        permission.setPermType("MENU");
        permission.setIsEnabled(true);

        assertNotNull(permission);
        assertEquals("VIEW_INDICATORS", permission.getPermissionCode());
        assertEquals("View Indicators", permission.getPermissionName());
        assertEquals("MENU", permission.getPermType());
        assertTrue(permission.getIsEnabled());
    }

    @Test
    @DisplayName("Should support MENU type")
    void shouldSupportMenuType() {
        permission.setPermissionCode("INDICATORS_MENU");
        permission.setPermissionName("Indicators Menu");
        permission.setPermType("MENU");
        permission.setRoutePath("/indicators");
        permission.setPageKey("INDICATORS");

        assertEquals("MENU", permission.getPermType());
        assertEquals("/indicators", permission.getRoutePath());
        assertEquals("INDICATORS", permission.getPageKey());
    }

    @Test
    @DisplayName("Should support ACTION type")
    void shouldSupportActionType() {
        permission.setPermissionCode("EDIT_INDICATOR");
        permission.setPermissionName("Edit Indicator");
        permission.setPermType("ACTION");
        permission.setActionKey("EDIT");

        assertEquals("ACTION", permission.getPermType());
        assertEquals("EDIT", permission.getActionKey());
    }

    @Test
    @DisplayName("Should track enabled status")
    void shouldTrackEnabledStatus() {
        permission.setPermissionCode("TEST");
        permission.setPermissionName("Test");
        permission.setPermType("ACTION");

        permission.setIsEnabled(true);
        assertTrue(permission.getIsEnabled());

        permission.setIsEnabled(false);
        assertFalse(permission.getIsEnabled());
    }

    @Test
    @DisplayName("Should support sort order")
    void shouldSupportSortOrder() {
        permission.setPermissionCode("PERM1");
        permission.setPermissionName("Permission 1");
        permission.setPermType("MENU");
        permission.setSortOrder(10);

        assertEquals(10, permission.getSortOrder());
    }

}
