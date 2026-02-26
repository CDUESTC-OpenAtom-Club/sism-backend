package com.sism.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers configuration for integration tests.
 * Provides a PostgreSQL container that matches the production environment.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    /**
     * Creates a PostgreSQL container for testing.
     * The @ServiceConnection annotation automatically configures Spring Boot
     * to use this container for database connections in tests.
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:12-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true); // Reuse container across test runs for faster execution
    }
}
