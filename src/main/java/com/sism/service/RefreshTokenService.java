package com.sism.service;

import com.sism.entity.SysUser;
import com.sism.entity.RefreshToken;
import com.sism.exception.UnauthorizedException;
import com.sism.repository.RefreshTokenRepository;
import com.sism.repository.UserRepository;
import com.sism.util.JwtUtil;
import com.sism.vo.UserVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh Token 服务
 * 实现 Token 的生成、验证、刷新和撤销逻辑
 * 
 * 安全特性:
 * - 使用 SecureRandom 生成随机 Token
 * - 存储 Token 的 SHA-256 哈希值
 * - 支持 Token 轮换（刷新时撤销旧 Token，生成新 Token）
 * - 定期清理过期 Token
 * - 优雅降级：当数据库表不存在时，自动禁用 refresh token 功能
 * 
 * Requirements: 1.2.2, 1.2.4, 1.2.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    /**
     * Refresh Token 功能是否可用
     * 当数据库表不存在时自动设为 false
     */
    private volatile boolean refreshTokenEnabled = true;

    /**
     * Refresh Token 有效期（毫秒）
     * 默认 7 天
     */
    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration;

    /**
     * Access Token 有效期（毫秒）
     * 默认 15 分钟
     */
    @Value("${jwt.expiration:900000}")
    private Long accessTokenExpiration;

    /**
     * 每个用户最大会话数
     * 超过此数量时，最旧的会话将被撤销
     */
    @Value("${jwt.max-sessions-per-user:5}")
    private int maxSessionsPerUser;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32; // 256 bits

    /**
     * 启动时检查 refresh_tokens 表是否存在
     */
    @PostConstruct
    public void checkTableExists() {
        try {
            // 尝试执行一个简单查询来检查表是否存在
            refreshTokenRepository.count();
            refreshTokenEnabled = true;
            log.info("Refresh token feature is ENABLED");
        } catch (Exception e) {
            refreshTokenEnabled = false;
            log.warn("Refresh token feature is DISABLED - table 'refresh_tokens' does not exist. " +
                    "Login will work without refresh token support. " +
                    "To enable, create the table using the SQL in database/V1.1__add_refresh_tokens.sql");
        }
    }

    /**
     * 检查 refresh token 功能是否可用
     * 
     * @return true 如果功能可用
     */
    public boolean isEnabled() {
        return refreshTokenEnabled;
    }

    /**
     * 为用户生成新的 Refresh Token
     * 
     * @param user 用户实体
     * @param deviceInfo 设备信息（User-Agent）
     * @param ipAddress 客户端 IP 地址
     * @return 原始 Refresh Token（仅返回一次，不存储），如果功能禁用则返回 null
     */
    @Transactional
    public String generateRefreshToken(SysUser user, String deviceInfo, String ipAddress) {
        if (!refreshTokenEnabled) {
            log.debug("Refresh token generation skipped - feature disabled");
            return null;
        }

        try {
            // 检查并限制用户的会话数
            enforceMaxSessions(user.getId());

            // 生成随机 Token
            String rawToken = generateSecureToken();
            String tokenHash = hashToken(rawToken);

            // 计算过期时间
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(refreshTokenExpiration / 1000);

            // 创建并保存 Refresh Token 实体
            RefreshToken refreshToken = new RefreshToken(
                    user, tokenHash, expiresAt, deviceInfo, ipAddress
            );
            refreshTokenRepository.save(refreshToken);

            log.info("Generated refresh token for user: {}, expires at: {}", 
                    user.getUsername(), expiresAt);

            return rawToken;
        } catch (Exception e) {
            log.error("Failed to generate refresh token, disabling feature: {}", e.getMessage());
            refreshTokenEnabled = false;
            return null;
        }
    }

    /**
     * 验证 Refresh Token 并返回关联的用户
     * 
     * @param rawToken 原始 Refresh Token
     * @return 关联的用户实体
     * @throws UnauthorizedException 如果 Token 无效、过期或已撤销
     */
    @Transactional(readOnly = true)
    public SysUser validateRefreshToken(String rawToken) {
        if (!refreshTokenEnabled) {
            throw new UnauthorizedException("Refresh token feature is not available");
        }

        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = hashToken(rawToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            log.warn("Attempted to use revoked refresh token for user: {}", 
                    refreshToken.getUser().getUsername());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            log.warn("Attempted to use expired refresh token for user: {}", 
                    refreshToken.getUser().getUsername());
            throw new UnauthorizedException("Refresh token has expired");
        }

        return refreshToken.getUser();
    }

    /**
     * 刷新 Token（Token 轮换）
     * 撤销旧的 Refresh Token，生成新的 Access Token 和 Refresh Token
     * 
     * @param rawToken 原始 Refresh Token
     * @param deviceInfo 设备信息
     * @param ipAddress IP 地址
     * @return 新的登录响应（包含新的 Access Token 和 Refresh Token）
     */
    @Transactional
    public RefreshResult refreshTokens(String rawToken, String deviceInfo, String ipAddress) {
        if (!refreshTokenEnabled) {
            throw new UnauthorizedException("Refresh token feature is not available");
        }

        // 验证旧 Token
        SysUser user = validateRefreshToken(rawToken);

        // 撤销旧 Token（Token 轮换）
        String oldTokenHash = hashToken(rawToken);
        refreshTokenRepository.revokeByTokenHash(oldTokenHash, LocalDateTime.now());
        log.debug("Revoked old refresh token for user: {}", user.getUsername());

        // 生成新的 Access Token
        String newAccessToken = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getOrg().getId()
        );

        // 生成新的 Refresh Token
        String newRefreshToken = generateRefreshToken(user, deviceInfo, ipAddress);

        // 构建用户 VO
        UserVO userVO = userService.getUserVOByUsername(user.getUsername());

        log.info("Refreshed tokens for user: {}", user.getUsername());

        return new RefreshResult(newAccessToken, newRefreshToken, userVO);
    }

    /**
     * 撤销指定的 Refresh Token
     * 
     * @param rawToken 原始 Refresh Token
     */
    @Transactional
    public void revokeToken(String rawToken) {
        if (!refreshTokenEnabled || rawToken == null || rawToken.isBlank()) {
            return;
        }

        try {
            String tokenHash = hashToken(rawToken);
            int revoked = refreshTokenRepository.revokeByTokenHash(tokenHash, LocalDateTime.now());
            
            if (revoked > 0) {
                log.info("Revoked refresh token");
            }
        } catch (Exception e) {
            log.warn("Failed to revoke refresh token: {}", e.getMessage());
        }
    }

    /**
     * 撤销用户的所有 Refresh Token
     * 用于登出所有设备或安全事件处理
     * 
     * @param userId 用户 ID
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        if (!refreshTokenEnabled) {
            return;
        }

        try {
            int revoked = refreshTokenRepository.revokeAllById(userId, LocalDateTime.now());
            log.info("Revoked {} refresh tokens for user ID: {}", revoked, userId);
        } catch (Exception e) {
            log.warn("Failed to revoke all user tokens: {}", e.getMessage());
        }
    }

    /**
     * 定期清理过期和已撤销的 Token
     * 每天凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        if (!refreshTokenEnabled) {
            return;
        }

        try {
            LocalDateTime cutoffTime = LocalDateTime.now();
            int deleted = refreshTokenRepository.deleteExpiredOrRevoked(cutoffTime);
            log.info("Cleaned up {} expired/revoked refresh tokens", deleted);
        } catch (Exception e) {
            log.warn("Failed to cleanup expired tokens: {}", e.getMessage());
        }
    }

    /**
     * 强制限制用户的最大会话数
     * 如果超过限制，撤销最旧的会话
     */
    private void enforceMaxSessions(Long userId) {
        long validTokenCount = refreshTokenRepository.countValidTokensById(userId);
        
        if (validTokenCount >= maxSessionsPerUser) {
            // 获取用户的所有有效 Token，按创建时间排序
            var validTokens = refreshTokenRepository.findValidTokensById(userId);
            
            // 撤销最旧的 Token，直到数量符合限制
            int tokensToRevoke = (int) (validTokenCount - maxSessionsPerUser + 1);
            validTokens.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .limit(tokensToRevoke)
                    .forEach(token -> {
                        token.revoke();
                        refreshTokenRepository.save(token);
                        log.debug("Revoked old session for user ID: {} due to max sessions limit", userId);
                    });
        }
    }

    /**
     * 生成安全的随机 Token
     * 
     * @return Base64 编码的随机 Token
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * 计算 Token 的 SHA-256 哈希值
     * 
     * @param token 原始 Token
     * @return 十六进制格式的哈希值
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Token 刷新结果
     */
    public record RefreshResult(
            String accessToken,
            String refreshToken,
            UserVO user
    ) {}
}
