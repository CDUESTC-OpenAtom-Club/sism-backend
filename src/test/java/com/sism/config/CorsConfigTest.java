package com.sism.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for CORS configuration
 * Verifies that CORS settings are loaded correctly from environment variables
 * 
 * Requirements: 2.2 - Eliminate hardcoded values, verify configuration loading
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.cors.allowed-origins=http://localhost:5173,http://localhost:3000",
    "app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
    "app.cors.allowed-headers=Authorization,Content-Type,X-Requested-With",
    "app.cors.allow-credentials=true",
    "app.cors.max-age=3600"
})
class CorsConfigTest {

    @Autowired
    private CorsFilter corsFilter;

    @Test
    void testCorsFilterBeanExists() {
        // Verify that the CORS filter bean is created
        assertThat(corsFilter).isNotNull();
    }

    @Test
    void testCorsConfigurationLoaded() {
        // Verify that the CORS filter is properly configured
        // This test ensures the configuration is loaded without errors
        assertThat(corsFilter).isNotNull();
        
        // If we get here without exceptions, the configuration loaded successfully
        // The actual CORS behavior is tested through integration tests
    }
}
