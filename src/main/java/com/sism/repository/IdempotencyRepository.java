package com.sism.repository;

import com.sism.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 幂等性记录数据访问层
 * 
 * **Validates: Requirements 2.2.2, 2.2.4**
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    /**
     * 根据幂等性 Key 查找记录
     * 
     * @param idempotencyKey 幂等性 Key
     * @return 幂等性记录（如果存在）
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * 根据幂等性 Key 查找未过期的记录
     * 
     * @param idempotencyKey 幂等性 Key
     * @param now 当前时间
     * @return 未过期的幂等性记录（如果存在）
     */
    @Query("SELECT r FROM IdempotencyRecord r WHERE r.idempotencyKey = :key AND r.expiresAt > :now")
    Optional<IdempotencyRecord> findValidByIdempotencyKey(
            @Param("key") String idempotencyKey,
            @Param("now") LocalDateTime now);

    /**
     * 检查幂等性 Key 是否存在且未过期
     * 
     * @param idempotencyKey 幂等性 Key
     * @param now 当前时间
     * @return true 如果存在未过期的记录
     */
    @Query("SELECT COUNT(r) > 0 FROM IdempotencyRecord r WHERE r.idempotencyKey = :key AND r.expiresAt > :now")
    boolean existsValidByIdempotencyKey(
            @Param("key") String idempotencyKey,
            @Param("now") LocalDateTime now);

    /**
     * 删除过期的记录
     * 
     * @param cutoffTime 截止时间
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :cutoffTime")
    int deleteExpiredRecords(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 统计未过期的记录数
     * 
     * @param now 当前时间
     * @return 未过期的记录数
     */
    @Query("SELECT COUNT(r) FROM IdempotencyRecord r WHERE r.expiresAt > :now")
    long countValidRecords(@Param("now") LocalDateTime now);
}
