package com.sism.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

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
        EntityScan entityScan = SismMainApplication.class.getAnnotation(EntityScan.class);
        ComponentScan componentScan = SismMainApplication.class.getAnnotation(ComponentScan.class);
        EnableJpaRepositories enableJpaRepositories = SismMainApplication.class.getAnnotation(EnableJpaRepositories.class);

        assertTrue(Arrays.asList(springBootApplication.scanBasePackages()).contains("com.sism.alert"));
        assertTrue(Arrays.asList(springBootApplication.scanBasePackages()).contains("com.sism.main"));
        assertTrue(Arrays.asList(springBootApplication.scanBasePackages()).contains("com.sism.config"));
        assertNotNull(entityScan);
        assertNotNull(componentScan);
        assertTrue(Arrays.asList(componentScan.basePackages()).contains("com.sism.alert"));
        assertTrue(Arrays.asList(componentScan.basePackages()).contains("com.sism.main"));
        assertNull(enableJpaRepositories);
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
