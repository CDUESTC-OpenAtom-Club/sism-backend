package com.sism.iam.application.service;

import com.sism.iam.domain.Permission;
import com.sism.iam.domain.Role;
import com.sism.iam.domain.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RoleService
 * Tests role management business logic
 */
@DisplayName("RoleService Tests")
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository);
    }

    @Test
    @DisplayName("Should create role with valid parameters")
    void shouldCreateRoleWithValidParameters() {
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role role = roleService.createRole("ADMIN", "Administrator", "Full system access");

        assertNotNull(role);
        assertEquals("ADMIN", role.getRoleCode());
        assertEquals("Administrator", role.getRoleName());
        assertEquals("Full system access", role.getDescription());
        assertTrue(role.getIsEnabled());
    }

    @Test
    @DisplayName("Should create role with null description")
    void shouldCreateRoleWithNullDescription() {
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role role = roleService.createRole("USER", "User", null);

        assertNotNull(role);
        assertEquals("USER", role.getRoleCode());
        assertEquals("User", role.getRoleName());
        assertNull(role.getDescription());
        assertTrue(role.getIsEnabled());
    }

    @Test
    @DisplayName("Should throw exception when create role with blank role code")
    void shouldThrowExceptionWhenCreateRoleWithBlankRoleCode() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> roleService.createRole("", "Administrator", "Description")
        );

        assertEquals("Role code is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when create role with null role code")
    void shouldThrowExceptionWhenCreateRoleWithNullRoleCode() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> roleService.createRole(null, "Administrator", "Description")
        );

        assertEquals("Role code is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when create role with blank role name")
    void shouldThrowExceptionWhenCreateRoleWithBlankRoleName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> roleService.createRole("ADMIN", "", "Description")
        );

        assertEquals("Role name is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when create role with null role name")
    void shouldThrowExceptionWhenCreateRoleWithNullRoleName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> roleService.createRole("ADMIN", null, "Description")
        );

        assertEquals("Role name is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should add permissions to role with empty permissions")
    void shouldAddPermissionsToRoleWithEmptyPermissions() {
        Role role = new Role();
        role.setRoleCode("ADMIN");
        role.setPermissions(null);

        Permission perm1 = new Permission();
        perm1.setId(1L);
        perm1.setPermissionCode("READ");

        Permission perm2 = new Permission();
        perm2.setId(2L);
        perm2.setPermissionCode("WRITE");

        Set<Permission> permissions = new HashSet<>();
        permissions.add(perm1);
        permissions.add(perm2);

        Role result = roleService.addPermissionsToRole(role, permissions);

        assertNotNull(result);
        assertNotNull(result.getPermissions());
        assertEquals(2, result.getPermissions().size());
        assertTrue(result.getPermissions().contains(perm1));
        assertTrue(result.getPermissions().contains(perm2));
    }

    @Test
    @DisplayName("Should add permissions to role with existing permissions")
    void shouldAddPermissionsToRoleWithExistingPermissions() {
        Role role = new Role();
        role.setRoleCode("ADMIN");

        Permission existingPerm = new Permission();
        existingPerm.setId(1L);
        existingPerm.setPermissionCode("READ");

        Set<Permission> existingPermissions = new HashSet<>();
        existingPermissions.add(existingPerm);
        role.setPermissions(existingPermissions);

        Permission newPerm = new Permission();
        newPerm.setId(2L);
        newPerm.setPermissionCode("WRITE");

        Set<Permission> newPermissions = new HashSet<>();
        newPermissions.add(newPerm);

        Role result = roleService.addPermissionsToRole(role, newPermissions);

        assertEquals(2, result.getPermissions().size());
        assertTrue(result.getPermissions().contains(existingPerm));
        assertTrue(result.getPermissions().contains(newPerm));
    }

    @Test
    @DisplayName("Should add empty permissions set to role")
    void shouldAddEmptyPermissionsSetToRole() {
        Role role = new Role();
        role.setRoleCode("ADMIN");
        role.setPermissions(null);

        Set<Permission> emptyPermissions = new HashSet<>();

        Role result = roleService.addPermissionsToRole(role, emptyPermissions);

        assertNotNull(result.getPermissions());
        assertTrue(result.getPermissions().isEmpty());
    }

    @Test
    @DisplayName("Should remove permissions from role")
    void shouldRemovePermissionsFromRole() {
        Role role = new Role();
        role.setRoleCode("ADMIN");

        Permission perm1 = new Permission();
        perm1.setId(1L);
        perm1.setPermissionCode("READ");

        Permission perm2 = new Permission();
        perm2.setId(2L);
        perm2.setPermissionCode("WRITE");

        Permission perm3 = new Permission();
        perm3.setId(3L);
        perm3.setPermissionCode("DELETE");

        Set<Permission> permissions = new HashSet<>();
        permissions.add(perm1);
        permissions.add(perm2);
        permissions.add(perm3);
        role.setPermissions(permissions);

        Set<Permission> toRemove = new HashSet<>();
        toRemove.add(perm2);

        Role result = roleService.removePermissionsFromRole(role, toRemove);

        assertEquals(2, result.getPermissions().size());
        assertTrue(result.getPermissions().contains(perm1));
        assertTrue(result.getPermissions().contains(perm3));
        assertFalse(result.getPermissions().contains(perm2));
    }

    @Test
    @DisplayName("Should remove permissions from role when permissions is null")
    void shouldRemovePermissionsFromRoleWhenPermissionsIsNull() {
        Role role = new Role();
        role.setRoleCode("ADMIN");
        role.setPermissions(null);

        Permission perm1 = new Permission();
        perm1.setId(1L);
        perm1.setPermissionCode("READ");

        Set<Permission> toRemove = new HashSet<>();
        toRemove.add(perm1);

        Role result = roleService.removePermissionsFromRole(role, toRemove);

        // Should not throw exception
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should remove non-existent permissions from role")
    void shouldRemoveNonExistentPermissionsFromRole() {
        Role role = new Role();
        role.setRoleCode("ADMIN");

        Permission perm1 = new Permission();
        perm1.setId(1L);
        perm1.setPermissionCode("READ");

        Set<Permission> permissions = new HashSet<>();
        permissions.add(perm1);
        role.setPermissions(permissions);

        Permission perm2 = new Permission();
        perm2.setId(2L);
        perm2.setPermissionCode("WRITE");

        Set<Permission> toRemove = new HashSet<>();
        toRemove.add(perm2);

        Role result = roleService.removePermissionsFromRole(role, toRemove);

        assertEquals(1, result.getPermissions().size());
        assertTrue(result.getPermissions().contains(perm1));
    }

    @Test
    @DisplayName("Should activate role")
    void shouldActivateRole() {
        Role role = new Role();
        role.setRoleCode("ADMIN");
        role.setIsEnabled(false);

        roleService.activateRole(role);

        assertTrue(role.getIsEnabled());
    }

    @Test
    @DisplayName("Should activate already enabled role")
    void shouldActivateAlreadyEnabledRole() {
        Role role = new Role();
        role.setRoleCode("ADMIN");
        role.setIsEnabled(true);

        roleService.activateRole(role);

        assertTrue(role.getIsEnabled());
    }

    @Test
    @DisplayName("Should deactivate role")
    void shouldDeactivateRole() {
        Role role = new Role();
        role.setRoleCode("ADMIN");
        role.setIsEnabled(true);

        roleService.deactivateRole(role);

        assertFalse(role.getIsEnabled());
    }

    @Test
    @DisplayName("Should deactivate already disabled role")
    void shouldDeactivateAlreadyDisabledRole() {
        Role role = new Role();
        role.setRoleCode("ADMIN");
        role.setIsEnabled(false);

        roleService.deactivateRole(role);

        assertFalse(role.getIsEnabled());
    }
}
