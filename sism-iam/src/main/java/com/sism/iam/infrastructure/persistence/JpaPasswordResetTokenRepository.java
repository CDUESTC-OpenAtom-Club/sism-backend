package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.user.PasswordResetToken;
import com.sism.iam.domain.user.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final JpaPasswordResetTokenRepositoryInternal jpaRepository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return jpaRepository.save(token);
    }

    @Override
    public Optional<PasswordResetToken> findLatestByEmail(String email) {
        return jpaRepository.findTopByEmailOrderByCreatedAtDesc(email);
    }

    @Override
    public Optional<PasswordResetToken> findLatestActiveByEmail(String email, LocalDateTime now) {
        return jpaRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, now);
    }

    @Override
    public long countByEmailCreatedAfter(String email, LocalDateTime createdAfter) {
        return jpaRepository.countByEmailAndCreatedAtAfter(email, createdAfter);
    }

    @Override
    public long countByIpAddressCreatedAfter(String ipAddress, LocalDateTime createdAfter) {
        return jpaRepository.countByIpAddressAndCreatedAtAfter(ipAddress, createdAfter);
    }

    @Override
    public boolean existsByTokenAndEmail(String token, String email) {
        return jpaRepository.existsByTokenAndEmail(token, email);
    }

    @Override
    public void markAllTokensUsedForUser(Long userId) {
        jpaRepository.markAllTokensUsedForUser(userId);
    }
}
