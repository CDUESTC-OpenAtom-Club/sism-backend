package com.sism.shared.domain.user;

import java.util.List;
import java.util.Optional;

/**
 * Shared user lookup contract for cross-context queries.
 */
public interface UserProvider {

    Optional<UserIdentity> findIdentity(Long userId);

    List<UserIdentity> findActiveIdentitiesByRole(Long roleId);

    List<Long> getUserRoleIds(Long userId);

    List<String> getUserPermissionCodes(Long userId);

    Optional<Long> getUserOrgId(Long userId);
}
