package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.Role;
import com.sism.iam.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaRoleRepository implements RoleRepository {

    private final JpaRoleRepositoryInternal jpaRepository;

    @Override
    public Optional<Role> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<Role> findByRoleCode(String roleCode) {
        return jpaRepository.findByRoleCode(roleCode);
    }

    @Override
    public Optional<Role> findByRoleName(String roleName) {
        return jpaRepository.findByRoleName(roleName);
    }

    @Override
    public List<Role> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public Role save(Role role) {
        return jpaRepository.save(role);
    }

    @Override
    public void delete(Role role) {
        jpaRepository.delete(role);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        return jpaRepository.existsByRoleCode(roleCode);
    }

    public Page<Role> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }
}
