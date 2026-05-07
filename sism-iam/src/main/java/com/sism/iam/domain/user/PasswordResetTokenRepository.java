package com.sism.iam.domain.user;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findLatestByEmail(String email);

    Optional<PasswordResetToken> findLatestActiveByEmail(String email, LocalDateTime now);

    long countByEmailCreatedAfter(String email, LocalDateTime createdAfter);

    long countByIpAddressCreatedAfter(String ipAddress, LocalDateTime createdAfter);

    boolean existsByTokenAndEmail(String token, String email);

    void markAllTokensUsedForUser(Long userId);
}
