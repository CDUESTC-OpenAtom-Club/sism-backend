package com.sism.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SismMainApplicationTest {

    @Test
    void springBootApplicationShouldScanRequiredPackagesAndKeepJpaScanningCentralized() {
        SpringBootApplication springBootApplication = SismMainApplication.class.getAnnotation(SpringBootApplication.class);
        EnableScheduling enableScheduling = SismMainApplication.class.getAnnotation(EnableScheduling.class);

        assertTrue(Arrays.asList(springBootApplication.scanBasePackages()).contains("com.sism.alert"));
        assertTrue(Arrays.asList(springBootApplication.scanBasePackages()).contains("com.sism.main"));
        assertTrue(Arrays.asList(springBootApplication.scanBasePackages()).contains("com.sism.config"));
        assertNotNull(enableScheduling);
    }

    @Test
    @SuppressWarnings("unchecked")
    void applicationYmlShouldNotEnableBeanDefinitionOverriding() throws IOException {
        Resource resource = new ClassPathResource("application.yml");
        Map<String, Object> properties = (Map<String, Object>) new YamlPropertySourceLoader()
                .load("application", resource)
                .stream()
                .findFirst()
                .orElseThrow()
                .getSource();

        assertNull(properties.get("spring.main.allow-bean-definition-overriding"));
    }
}
