package com.sism.iam.infrastructure.persistence;

import com.sism.iam.domain.user.PasswordResetToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface JpaPasswordResetTokenRepositoryInternal extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findTopByEmailOrderByCreatedAtDesc(String email);

    @Query("""
            SELECT t
            FROM PasswordResetToken t
            WHERE t.email = :email
              AND t.used = false
              AND t.expiresAt > :now
            ORDER BY t.createdAt DESC
            """)
    Optional<PasswordResetToken> findFirstActiveByEmail(String email, LocalDateTime now);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime createdAfter);

    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime createdAfter);

    boolean existsByTokenAndEmail(String token, String email);

    @Transactional
    @Modifying
    @Query("""
            UPDATE PasswordResetToken t
            SET t.used = true
            WHERE t.userId = :userId
              AND t.used = false
            """)
    void markAllTokensUsedForUser(Long userId);
}
