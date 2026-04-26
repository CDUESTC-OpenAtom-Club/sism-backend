package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class JpaUserRepository implements UserRepository {

    private final JpaUserRepositoryInternal jpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<User> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByIdIn(ids);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username);
    }

    @Override
    public List<User> findByOrgId(Long orgId) {
        return jpaRepository.findByOrgId(orgId);
    }

    @Override
    public List<User> findByRoleId(Long roleId) {
        return jpaRepository.findByRoleId(roleId);
    }

    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        return jpaRepository.findRoleIdsByUserId(userId);
    }

    @Override
    public List<String> findRoleCodesByUserId(Long userId) {
        return jpaRepository.findRoleCodesByUserId(userId);
    }

    @Override
    public List<String> findPermissionCodesByUserId(Long userId) {
        return jpaRepository.findPermissionCodesByUserId(userId);
    }

    @Override
    public Map<Long, Long> countUsersByRoleIds(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Object[] row : jpaRepository.countUsersByRoleIds(List.copyOf(roleIds))) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public List<User> findByIsActive(Boolean isActive) {
        return jpaRepository.findByIsActive(isActive);
    }

    @Override
    public User save(User user) {
        return jpaRepository.save(user);
    }

    @Override
    public void delete(User user) {
        jpaRepository.delete(user);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }
}
