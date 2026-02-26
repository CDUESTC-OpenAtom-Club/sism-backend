package com.sism.annotation;

import com.sism.config.TestSecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Meta-annotation for integration tests
 * Combines common test annotations and imports test security configuration
 * 
 * Usage:
 * @IntegrationTest
 * class MyControllerIntegrationTest {
 *     // test methods
 * }
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
public @interface IntegrationTest {
}
