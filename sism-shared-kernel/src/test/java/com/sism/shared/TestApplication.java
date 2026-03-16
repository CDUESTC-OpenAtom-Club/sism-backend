package com.sism.shared;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * TestApplication - 测试专用的Spring Boot应用配置
 *
 * 仅用于启动测试上下文，扫描shared-kernel模块的实体和Repository
 * 不包含任何业务逻辑，仅为JPA集成测试提供支持
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.sism.shared.infrastructure.event")
@EntityScan(basePackages = "com.sism.shared.infrastructure.event")
public class TestApplication {
    // 测试用启动类，无需实现任何方法
}
