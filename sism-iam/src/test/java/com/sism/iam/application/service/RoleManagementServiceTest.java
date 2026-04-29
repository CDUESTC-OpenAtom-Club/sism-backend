package com.sism.iam.application.service;

import com.sism.iam.domain.access.Role;
import com.sism.iam.domain.access.PermissionRepository;
import com.sism.iam.domain.access.RoleRepository;
import com.sism.iam.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleManagementService Tests")
class RoleManagementServiceTest {

    @Mock
    private RoleService roleService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    private RoleManagementService roleManagementService;

    @BeforeEach
    void setUp() {
        roleManagementService = new RoleManagementService(roleService, roleRepository, userRepository, permissionRepository);
    }

    @Test
    @DisplayName("Should delete unused role")
    void shouldDeleteUnusedRole() {
        Role role = new Role();
        role.setId(1L);
        role.setRoleCode("ADMIN");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.findByRoleId(1L)).thenReturn(List.of());

        roleManagementService.deleteRole(1L);

        verify(roleRepository).delete(role);
    }

    @Test
    @DisplayName("Should reject deleting role in use")
    void shouldRejectDeletingRoleInUse() {
        Role role = new Role();
        role.setId(1L);
        role.setRoleCode("ADMIN");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.findByRoleId(1L)).thenReturn(List.of(new com.sism.iam.domain.user.User()));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> roleManagementService.deleteRole(1L)
        );

        assertEquals("Role is still in use by users", exception.getMessage());
    }

    @Test
    @DisplayName("Should count permissions by role ids")
    void shouldCountPermissionsByRoleIds() {
        when(roleRepository.countPermissionsByRoleIds(anySet()))
                .thenReturn(Map.of(1L, 3L, 2L, 0L));

        Map<Long, Long> counts = roleManagementService.countPermissionsByRoleIds(Set.of(1L, 2L));

        assertEquals(2, counts.size());
        assertEquals(3L, counts.get(1L));
        assertEquals(0L, counts.get(2L));
    }

    @Test
    @DisplayName("Should create role through RoleService")
    void shouldCreateRoleThroughRoleService() {
        Role role = new Role();
        role.setId(1L);
        role.setRoleCode("ADMIN");
        when(roleService.createRole("ADMIN", "Administrator", "desc")).thenReturn(role);

        Role created = roleManagementService.createRole("ADMIN", "Administrator", "desc");

        assertEquals(1L, created.getId());
        verify(roleService).createRole("ADMIN", "Administrator", "desc");
    }

    @Test
    @DisplayName("Should page roles")
    void shouldPageRoles() {
        Role role = new Role();
        role.setId(1L);
        role.setRoleCode("ADMIN");
        when(roleRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(role), PageRequest.of(0, 10), 1));

        Page<Role> page = roleManagementService.findRoles(PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("ADMIN", page.getContent().get(0).getRoleCode());
    }
}
