package com.sism.repository;

import com.sism.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Refresh Token 数据访问层
 * 提供 Refresh Token 的 CRUD 操作和查询方法
 * 
 * Requirements: 1.2.2 实现 Refresh Token 机制
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 根据 Token 哈希值查找 Refresh Token
     * 
     * @param tokenHash Token 的 SHA-256 哈希值
     * @return 匹配的 Refresh Token
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 查找用户的所有有效 Refresh Token
     * 有效条件: 未过期且未被撤销
     * 
     * @param userId 用户 ID
     * @return 有效的 Refresh Token 列表
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revokedAt IS NULL AND rt.expiresAt > CURRENT_TIMESTAMP")
    List<RefreshToken> findValidTokensById(@Param("userId") Long userId);

    /**
     * 查找用户的所有 Refresh Token（包括已撤销和过期的）
     * 
     * @param userId 用户 ID
     * @return Refresh Token 列表
     */
    List<RefreshToken> findByUser_Id(Long userId);

    /**
     * 撤销用户的所有 Refresh Token
     * 用于登出所有设备或安全事件处理
     * 
     * @param userId 用户 ID
     * @param revokedAt 撤销时间
     * @return 受影响的行数
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt " +
           "WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    int revokeAllById(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * 撤销指定的 Refresh Token
     * 
     * @param tokenHash Token 的 SHA-256 哈希值
     * @param revokedAt 撤销时间
     * @return 受影响的行数
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt " +
           "WHERE rt.tokenHash = :tokenHash AND rt.revokedAt IS NULL")
    int revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * 删除过期或已撤销的 Refresh Token
     * 用于定期清理
     * 
     * @param cutoffTime 截止时间
     * @return 删除的行数
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoffTime OR rt.revokedAt IS NOT NULL")
    int deleteExpiredOrRevoked(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 统计用户的有效 Token 数量
     * 用于限制每个用户的最大会话数
     * 
     * @param userId 用户 ID
     * @return 有效 Token 数量
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revokedAt IS NULL AND rt.expiresAt > CURRENT_TIMESTAMP")
    long countValidTokensById(@Param("userId") Long userId);

    /**
     * 检查 Token 哈希是否存在
     * 
     * @param tokenHash Token 的 SHA-256 哈希值
     * @return true 如果存在
     */
    boolean existsByTokenHash(String tokenHash);

    /**
     * 查找并锁定 Token（用于并发安全的 Token 轮换）
     * 
     * @param tokenHash Token 的 SHA-256 哈希值
     * @return 匹配的 Refresh Token
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
