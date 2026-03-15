package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserRepository implements UserRepository {

    private final JpaUserRepositoryInternal jpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return jpaRepository.findByPhone(phone);
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
    public List<User> findByIsActive(Boolean isActive) {
        // Convert isActive boolean to UserStatus
        User.UserStatus status = isActive ? User.UserStatus.ACTIVE : User.UserStatus.INACTIVE;
        return jpaRepository.findByStatus(status);
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

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.findByEmail(email).isPresent();
    }
}
