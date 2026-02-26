package com.sism.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 幂等性记录实体类
 * 用于存储请求的幂等性 Key 和对应的响应结果
 * 
 * 功能:
 * - 防止重复请求产生重复数据
 * - 在 TTL 时间内返回缓存的响应
 * - 支持自动清理过期记录
 * 
 * **Validates: Requirements 2.2.2, 2.2.4**
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key"),
    @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
})
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 幂等性 Key (SHA-256 哈希值)
     * 由前端根据请求内容生成
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    /**
     * 缓存的响应体 (JSON 格式)
     */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    /**
     * HTTP 状态码
     */
    @Column(name = "status_code")
    private Integer statusCode;

    /**
     * 请求的 HTTP 方法
     */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /**
     * 请求的 URL 路径
     */
    @Column(name = "request_path", length = 255)
    private String requestPath;

    /**
     * 记录创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 记录过期时间
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 请求处理状态
     * PENDING: 正在处理中
     * COMPLETED: 处理完成
     * FAILED: 处理失败
     */
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;

    /**
     * 创建新的幂等性记录
     * 
     * @param idempotencyKey 幂等性 Key
     * @param httpMethod HTTP 方法
     * @param requestPath 请求路径
     * @param ttlSeconds 过期时间（秒）
     */
    public IdempotencyRecord(String idempotencyKey, String httpMethod, 
                             String requestPath, int ttlSeconds) {
        this.idempotencyKey = idempotencyKey;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusSeconds(ttlSeconds);
        this.status = IdempotencyStatus.PENDING;
    }

    /**
     * 检查记录是否已过期
     * 
     * @return true 如果记录已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查记录是否有效（未过期且已完成）
     * 
     * @return true 如果记录有效
     */
    public boolean isValid() {
        return !isExpired() && status == IdempotencyStatus.COMPLETED;
    }

    /**
     * 检查请求是否正在处理中
     * 
     * @return true 如果请求正在处理中
     */
    public boolean isPending() {
        return status == IdempotencyStatus.PENDING && !isExpired();
    }

    /**
     * 标记请求处理完成并保存响应
     * 
     * @param responseBody 响应体
     * @param statusCode HTTP 状态码
     */
    public void complete(String responseBody, int statusCode) {
        this.responseBody = responseBody;
        this.statusCode = statusCode;
        this.status = IdempotencyStatus.COMPLETED;
    }

    /**
     * 标记请求处理失败
     * 
     * @param errorResponse 错误响应
     * @param statusCode HTTP 状态码
     */
    public void fail(String errorResponse, int statusCode) {
        this.responseBody = errorResponse;
        this.statusCode = statusCode;
        this.status = IdempotencyStatus.FAILED;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * 幂等性记录状态枚举
     */
    public enum IdempotencyStatus {
        /** 请求正在处理中 */
        PENDING,
        /** 请求处理完成 */
        COMPLETED,
        /** 请求处理失败 */
        FAILED
    }
}
