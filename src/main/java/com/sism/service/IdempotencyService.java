package com.sism.service;

import com.sism.entity.IdempotencyRecord;
import com.sism.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 幂等性服务
 * 
 * 功能:
 * - 检查请求是否重复
 * - 保存请求结果
 * - 返回缓存的响应
 * - 定期清理过期记录
 * 
 * **Property P8**: 在 TTL 时间内，相同 Key 的请求返回缓存结果；超过 TTL 后，请求正常处理。
 * 
 * **Validates: Requirements 2.2.2, 2.2.4**
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    /**
     * 幂等性记录的默认 TTL（秒）
     * 默认 5 分钟
     */
    @Value("${idempotency.ttl-seconds:300}")
    private int defaultTtlSeconds;

    /**
     * 检查请求是否重复，如果是重复请求则返回缓存的响应
     * 
     * @param idempotencyKey 幂等性 Key
     * @return 如果是重复请求，返回缓存的记录；否则返回空
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> checkDuplicate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        Optional<IdempotencyRecord> existingRecord = idempotencyRepository
                .findValidByIdempotencyKey(idempotencyKey, LocalDateTime.now());

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            
            if (record.isValid()) {
                log.info("Duplicate request detected for key: {}, returning cached response", 
                        idempotencyKey.substring(0, 8) + "...");
                return existingRecord;
            }
            
            if (record.isPending()) {
                log.warn("Request with key {} is still being processed", 
                        idempotencyKey.substring(0, 8) + "...");
                // 返回 pending 状态的记录，让调用方决定如何处理
                return existingRecord;
            }
        }

        return Optional.empty();
    }

    /**
     * 开始处理请求，创建 PENDING 状态的记录
     * 
     * @param idempotencyKey 幂等性 Key
     * @param httpMethod HTTP 方法
     * @param requestPath 请求路径
     * @return 创建的幂等性记录
     */
    @Transactional
    public IdempotencyRecord startProcessing(String idempotencyKey, String httpMethod, 
                                              String requestPath) {
        return startProcessing(idempotencyKey, httpMethod, requestPath, defaultTtlSeconds);
    }

    /**
     * 开始处理请求，创建 PENDING 状态的记录
     * 
     * @param idempotencyKey 幂等性 Key
     * @param httpMethod HTTP 方法
     * @param requestPath 请求路径
     * @param ttlSeconds 过期时间（秒）
     * @return 创建的幂等性记录
     */
    @Transactional
    public IdempotencyRecord startProcessing(String idempotencyKey, String httpMethod, 
                                              String requestPath, int ttlSeconds) {
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpMethod, requestPath, ttlSeconds);
        
        return idempotencyRepository.save(record);
    }

    /**
     * 保存请求成功的结果
     * 
     * @param idempotencyKey 幂等性 Key
     * @param responseBody 响应体
     * @param statusCode HTTP 状态码
     */
    @Transactional
    public void saveSuccess(String idempotencyKey, String responseBody, int statusCode) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(record -> {
                    record.complete(responseBody, statusCode);
                    idempotencyRepository.save(record);
                    log.debug("Saved successful response for key: {}", 
                            idempotencyKey.substring(0, 8) + "...");
                });
    }

    /**
     * 保存请求失败的结果
     * 
     * @param idempotencyKey 幂等性 Key
     * @param errorResponse 错误响应
     * @param statusCode HTTP 状态码
     */
    @Transactional
    public void saveFailure(String idempotencyKey, String errorResponse, int statusCode) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(record -> {
                    record.fail(errorResponse, statusCode);
                    idempotencyRepository.save(record);
                    log.debug("Saved failure response for key: {}", 
                            idempotencyKey.substring(0, 8) + "...");
                });
    }

    /**
     * 删除幂等性记录（用于回滚场景）
     * 
     * @param idempotencyKey 幂等性 Key
     */
    @Transactional
    public void deleteRecord(String idempotencyKey) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(idempotencyRepository::delete);
    }

    /**
     * 检查幂等性 Key 是否存在且有效
     * 
     * @param idempotencyKey 幂等性 Key
     * @return true 如果存在有效的记录
     */
    @Transactional(readOnly = true)
    public boolean exists(String idempotencyKey) {
        return idempotencyRepository.existsValidByIdempotencyKey(
                idempotencyKey, LocalDateTime.now());
    }

    /**
     * 获取默认 TTL（秒）
     * 
     * @return 默认 TTL
     */
    public int getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    /**
     * 定期清理过期的幂等性记录
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupExpiredRecords() {
        LocalDateTime cutoffTime = LocalDateTime.now();
        int deleted = idempotencyRepository.deleteExpiredRecords(cutoffTime);
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency records", deleted);
        }
    }

    /**
     * 获取当前有效记录数（用于监控）
     * 
     * @return 有效记录数
     */
    @Transactional(readOnly = true)
    public long getValidRecordCount() {
        return idempotencyRepository.countValidRecords(LocalDateTime.now());
    }
}
