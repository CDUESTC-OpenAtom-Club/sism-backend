package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaUserRepositoryInternal extends JpaRepository<User, Long> {
    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);

    List<User> findByOrgId(Long orgId);

    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.roles allRoles JOIN u.roles filterRole WHERE filterRole.id = :roleId")
    List<User> findByRoleId(Long roleId);

    @Query(value = "SELECT role_id FROM sys_user_role WHERE user_id = :userId ORDER BY role_id", nativeQuery = true)
    List<Long> findRoleIdsByUserId(Long userId);

    @Query(value = """
            SELECT r.role_code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = :userId
            ORDER BY ur.role_id
            """, nativeQuery = true)
    List<String> findRoleCodesByUserId(Long userId);

    @Query(value = """
            SELECT DISTINCT p.perm_code
            FROM sys_user_role ur
            JOIN sys_role_permission rp ON rp.role_id = ur.role_id
            JOIN sys_permission p ON p.id = rp.perm_id
            WHERE ur.user_id = :userId
              AND COALESCE(p.is_enabled, true) = true
            ORDER BY p.perm_code
            """, nativeQuery = true)
    List<String> findPermissionCodesByUserId(Long userId);

    List<User> findByIsActive(Boolean isActive);
    boolean existsByUsername(String username);
}
