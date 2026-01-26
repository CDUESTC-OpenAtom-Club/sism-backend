package com.sism.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Refresh Token 实体类
 * 用于安全存储用户的刷新令牌
 * 
 * 安全特性:
 * - 存储 Token 的 SHA-256 哈希值，不存储原始 Token
 * - 支持 Token 撤销（revoked_at）
 * - 记录设备信息和 IP 地址用于安全审计
 * 
 * Requirements: 1.2.2 实现 Refresh Token 机制
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
    @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /**
     * Token 的 SHA-256 哈希值
     * 不存储原始 Token，提高安全性
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Token 过期时间
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Token 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Token 撤销时间
     * 非空表示 Token 已被撤销
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * 设备信息（User-Agent）
     * 用于安全审计和多设备管理
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /**
     * 客户端 IP 地址
     * 支持 IPv4 和 IPv6
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * 创建新的 Refresh Token 实体
     * 
     * @param user 关联的用户
     * @param tokenHash Token 的 SHA-256 哈希值
     * @param expiresAt 过期时间
     * @param deviceInfo 设备信息
     * @param ipAddress IP 地址
     */
    public RefreshToken(AppUser user, String tokenHash, LocalDateTime expiresAt, 
                        String deviceInfo, String ipAddress) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 检查 Token 是否有效
     * Token 有效条件: 未过期且未被撤销
     * 
     * @return true 如果 Token 有效
     */
    public boolean isValid() {
        return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * 检查 Token 是否已过期
     * 
     * @return true 如果 Token 已过期
     */
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 检查 Token 是否已被撤销
     * 
     * @return true 如果 Token 已被撤销
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * 撤销此 Token
     */
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
