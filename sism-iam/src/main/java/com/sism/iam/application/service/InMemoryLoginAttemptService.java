package com.sism.iam.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryLoginAttemptService implements LoginAttemptService {

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Value("${auth.login.max-failures:5}")
    private int maxFailures;

    @Value("${auth.login.lock-duration-seconds:900}")
    private long lockDurationSeconds;

    @Override
    public void assertNotBlocked(String username, String clientKey) {
        AttemptState state = attempts.get(buildKey(username, clientKey));
        if (state == null) {
            return;
        }

        Instant now = Instant.now();
        if (state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
            long remainingSeconds = Duration.between(now, state.lockedUntil()).toSeconds();
            throw new IllegalStateException("登录失败次数过多，请在 " + Math.max(remainingSeconds, 1) + " 秒后重试");
        }

        if (state.lockedUntil() != null && !state.lockedUntil().isAfter(now)) {
            attempts.remove(buildKey(username, clientKey));
        }
    }

    @Override
    public void recordFailure(String username, String clientKey) {
        String key = buildKey(username, clientKey);
        Instant now = Instant.now();
        attempts.compute(key, (ignored, current) -> {
            AttemptState state = current;
            if (state == null || (state.lockedUntil() != null && !state.lockedUntil().isAfter(now))) {
                state = new AttemptState(0, null);
            }

            int nextFailures = state.failures() + 1;
            Instant lockedUntil = nextFailures >= Math.max(maxFailures, 1)
                    ? now.plusSeconds(Math.max(lockDurationSeconds, 1))
                    : null;
            return new AttemptState(nextFailures, lockedUntil);
        });
    }

    @Override
    public void recordSuccess(String username, String clientKey) {
        attempts.remove(buildKey(username, clientKey));
    }

    private String buildKey(String username, String clientKey) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        String normalizedClientKey = clientKey == null ? "unknown" : clientKey.trim().toLowerCase();
        return normalizedUsername + "@" + normalizedClientKey;
    }

    private record AttemptState(int failures, Instant lockedUntil) {
    }
}
