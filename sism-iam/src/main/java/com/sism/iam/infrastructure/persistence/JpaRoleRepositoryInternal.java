package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaRoleRepositoryInternal extends JpaRepository<Role, Long> {
    @Override
    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "permissions")
    Page<Role> findAll(Pageable pageable);

    Optional<Role> findByRoleCode(String roleCode);
    Optional<Role> findByRoleName(String roleName);
    // Query roles by user ID - Spring Data JPA automatically creates query from method name "findByUsers_Id"
    List<Role> findByUsers_Id(Long userId);
    @Query("""
            SELECT r.id, COUNT(DISTINCT p.id)
            FROM Role r
            LEFT JOIN r.permissions p
            WHERE r.id IN :roleIds
            GROUP BY r.id
            """)
    List<Object[]> countPermissionsByRoleIds(List<Long> roleIds);
    boolean existsByRoleCode(String roleCode);
}
