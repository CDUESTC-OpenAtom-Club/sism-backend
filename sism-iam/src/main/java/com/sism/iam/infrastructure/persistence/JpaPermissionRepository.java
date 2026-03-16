package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.Permission;
import com.sism.iam.domain.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class JpaPermissionRepository implements PermissionRepository {

    private final JpaPermissionRepositoryInternal jpaRepository;

    @Override
    public Optional<Permission> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Set<Permission> findByIds(Set<Long> ids) {
        return Set.copyOf(jpaRepository.findByIdIn(ids));
    }

    @Override
    public Optional<Permission> findByPermissionCode(String permCode) {
        return jpaRepository.findByPermissionCode(permCode);
    }

    @Override
    public List<Permission> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Permission save(Permission permission) {
        return jpaRepository.save(permission);
    }

    @Override
    public void delete(Permission permission) {
        jpaRepository.delete(permission);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByPermissionCode(String permissionCode) {
        return jpaRepository.existsByPermissionCode(permissionCode);
    }
}
