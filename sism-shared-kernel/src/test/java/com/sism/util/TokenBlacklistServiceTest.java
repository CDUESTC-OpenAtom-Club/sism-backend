package com.sism.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private static final Pattern HASH_KEY_PATTERN = Pattern.compile("token:blacklist:[0-9a-f]{64}");

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cursor<String> cursor;

    @Test
    void clearShouldScanAndDeleteOnlyBlacklistKeysWhenRedisIsEnabled() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn("token:blacklist:one", "token:blacklist:two");

        TokenBlacklistService service = new TokenBlacklistService(3600, redisTemplate);
        service.clear();

        ArgumentCaptor<ScanOptions> optionsCaptor = ArgumentCaptor.forClass(ScanOptions.class);
        verify(redisTemplate).scan(optionsCaptor.capture());
        ScanOptions options = optionsCaptor.getValue();
        assertEquals("token:blacklist:*", options.getPattern());
        assertEquals(1000L, options.getCount());
        verify(redisTemplate).delete(List.of("token:blacklist:one", "token:blacklist:two"));
        verify(redisTemplate, never()).keys(anyString());
        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    void shouldUseHashedRedisKeyAndFallbackToMemoryWhenRedisFails() {
        @SuppressWarnings("unchecked")
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOperations =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(valueOperations).set(anyString(), any(), any(Duration.class));

        TokenBlacklistService service = new TokenBlacklistService(3600, redisTemplate);
        service.blacklist("token-1", Duration.ofSeconds(10));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), any(), any(Duration.class));
        assertTrue(HASH_KEY_PATTERN.matcher(keyCaptor.getValue()).matches());
        assertTrue(service.isBlacklisted("token-1"));
    }

    @Test
    void clearShouldRemoveInMemoryTokensWhenRedisIsDisabled() {
        TokenBlacklistService service = new TokenBlacklistService(3600, null);
        service.blacklist("token-1", Duration.ofSeconds(10));

        assertTrue(service.isBlacklisted("token-1"));

        service.clear();

        assertFalse(service.isBlacklisted("token-1"));
    }
}
