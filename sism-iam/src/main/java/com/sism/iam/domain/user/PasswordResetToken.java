package com.sism.iam.domain.user;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "password_reset_tokens")
@Access(AccessType.FIELD)
public class PasswordResetToken extends AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, length = 6)
    private String token;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Override
    public void validate() {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (token == null || !token.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("Reset token must be 6 digits");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration time is required");
        }
    }
}
