package com.sism.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * System User entity
 * Associated with an organization
 * Renamed from SysUser on 2026-02-10
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private SysOrg org;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "sso_id", length = 100)
    private String ssoId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
