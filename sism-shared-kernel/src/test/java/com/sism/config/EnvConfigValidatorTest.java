package com.sism.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvConfigValidatorTest {

    @Test
    void shouldResolveJwtSecretFromAppJwtSecretProperty() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.jwt.secret", "test-secret")
                .withProperty("spring.datasource.url", "jdbc:h2:mem:test")
                .withProperty("spring.datasource.username", "sa")
                .withProperty("spring.datasource.password", "password");

        EnvConfigValidator validator = new EnvConfigValidator(environment);

        assertTrue(validator.isVariableConfigured("JWT_SECRET"));
    }

    @Test
    void shouldReportJwtSecretMissingWhenNoMappedPropertyExists() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:test")
                .withProperty("spring.datasource.username", "sa")
                .withProperty("spring.datasource.password", "password");

        EnvConfigValidator validator = new EnvConfigValidator(environment);

        assertFalse(validator.isVariableConfigured("JWT_SECRET"));
    }
}
