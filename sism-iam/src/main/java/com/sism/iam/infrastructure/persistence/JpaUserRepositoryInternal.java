package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaUserRepositoryInternal extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    List<User> findByOrgId(Long orgId);
    List<User> findByRoleId(Long roleId);
    List<User> findByIsActive(Boolean isActive);
    boolean existsByUsername(String username);
}
