package com.sism.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing JWT token blacklist.
 * Supports Redis storage when available and falls back to in-memory TTL-based storage.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private static final String REDIS_KEY_PREFIX = "token:blacklist:";
    private static final int REDIS_SCAN_COUNT = 1000;
    private static final int REDIS_DELETE_BATCH_SIZE = 500;
    private static final long LOCAL_PURGE_INTERVAL_MILLIS = Duration.ofMinutes(5).toMillis();

    private final ConcurrentMap<String, Instant> localBlacklist = new ConcurrentHashMap<>();
    private final AtomicLong lastLocalPurgeAt = new AtomicLong(0L);
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration defaultTtl;
    private final boolean redisEnabled;

    public TokenBlacklistService(
            @Value("${token.blacklist.ttl-seconds:3600}") long ttlSeconds,
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate
    ) {
        this.defaultTtl = Duration.ofSeconds(Math.max(1, ttlSeconds));
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisTemplate != null;
    }

    public void blacklist(String token) {
        blacklist(token, defaultTtl);
    }

    public void blacklist(String token, Duration ttl) {
        if (token == null || token.isBlank()) {
            return;
        }

        Duration effectiveTtl = ttl == null || ttl.isNegative() ? defaultTtl : ttl;

        if (redisEnabled) {
            String key = redisKey(token);
            redisTemplate.opsForValue().set(key, "1", effectiveTtl);
            log.debug("Token blacklisted in Redis (key={})", key);
            return;
        }

        Instant expiry = Instant.now().plus(effectiveTtl);
        localBlacklist.put(token, expiry);
        log.debug("Token blacklisted in-memory until {}", expiry);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        if (redisEnabled) {
            Boolean exists = redisTemplate.hasKey(redisKey(token));
            return Boolean.TRUE.equals(exists);
        }

        purgeExpiredIfNeeded();
        Instant expiresAt = localBlacklist.get(token);
        return expiresAt != null && Instant.now().isBefore(expiresAt);
    }

    public void remove(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        if (redisEnabled) {
            redisTemplate.delete(redisKey(token));
        } else {
            localBlacklist.remove(token);
        }
    }

    public void clear() {
        try {
            if (redisEnabled) {
                clearRedisBlacklist();
            }
        } finally {
            localBlacklist.clear();
            lastLocalPurgeAt.set(0L);
        }
    }

    private void clearRedisBlacklist() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(REDIS_KEY_PREFIX + "*")
                .count(REDIS_SCAN_COUNT)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            if (cursor == null) {
                return;
            }

            List<String> batch = new ArrayList<>(REDIS_DELETE_BATCH_SIZE);
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= REDIS_DELETE_BATCH_SIZE) {
                    redisTemplate.delete(List.copyOf(batch));
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                redisTemplate.delete(List.copyOf(batch));
            }
        }
    }

    private void purgeExpiredIfNeeded() {
        long now = System.currentTimeMillis();
        long lastPurge = lastLocalPurgeAt.get();
        if (now - lastPurge < LOCAL_PURGE_INTERVAL_MILLIS) {
            return;
        }

        if (!lastLocalPurgeAt.compareAndSet(lastPurge, now)) {
            return;
        }

        purgeExpired(Instant.ofEpochMilli(now));
    }

    private void purgeExpired(Instant now) {
        localBlacklist.entrySet().removeIf(entry -> {
            Instant expiresAt = entry.getValue();
            return expiresAt == null || !expiresAt.isAfter(now);
        });
    }

    private String redisKey(String token) {
        return REDIS_KEY_PREFIX + token;
    }
}
