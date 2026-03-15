package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaRoleRepositoryInternal extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleCode(String roleCode);
    Optional<Role> findByRoleName(String roleName);
    List<Role> findByUserId(Long userId);
    boolean existsByRoleCode(String roleCode);
}
