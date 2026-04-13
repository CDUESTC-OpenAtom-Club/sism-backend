package com.sism.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorsConfigTest {

    @Test
    void shouldRejectWildcardOriginWhenCredentialsEnabled() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "*");
        ReflectionTestUtils.setField(config, "allowedMethods", "GET,POST");
        ReflectionTestUtils.setField(config, "allowedHeaders", "Authorization,Content-Type");
        ReflectionTestUtils.setField(config, "allowCredentials", true);
        ReflectionTestUtils.setField(config, "maxAge", 3600L);

        assertThrows(IllegalStateException.class, config::corsFilter);
    }

    @Test
    void shouldAllowWildcardPatternsWhenCredentialsEnabled() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://*.example.com");
        ReflectionTestUtils.setField(config, "allowedMethods", "GET,POST");
        ReflectionTestUtils.setField(config, "allowedHeaders", "Authorization,Content-Type");
        ReflectionTestUtils.setField(config, "allowCredentials", true);
        ReflectionTestUtils.setField(config, "maxAge", 3600L);

        assertDoesNotThrow(config::corsFilter);
    }
}
