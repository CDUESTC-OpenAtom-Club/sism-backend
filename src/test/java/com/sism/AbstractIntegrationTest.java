package com.sism;

import com.sism.config.TestContainersConfiguration;
import com.sism.config.TestSecurityConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests using TestContainers.
 * All integration tests should extend this class to automatically
 * use the PostgreSQL container for testing.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
public abstract class AbstractIntegrationTest {
    // Base class for integration tests
    // TestContainers will automatically start a PostgreSQL container
    // and configure Spring Boot to use it
}
