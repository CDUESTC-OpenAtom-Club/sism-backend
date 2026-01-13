package com.sism.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * User entity
 * Associated with an organization
 */
@Getter
@Setter
@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "sso_id", length = 100)
    private String ssoId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
