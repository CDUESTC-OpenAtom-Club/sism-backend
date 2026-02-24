package com.sism.repository;

import com.sism.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * System User Repository
 *
 * @deprecated Use {@link UserRepository} instead. This interface is kept for backward compatibility
 * and will be removed in a future version. All functionality has been moved to UserRepository.
 */
@Deprecated(since = "1.0", forRemoval = true)
@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    /**
     * @deprecated Use {@link UserRepository#findByUsername(String)} instead
     */
    @Deprecated(forRemoval = true)
    Optional<SysUser> findByUsername(String username);
}
