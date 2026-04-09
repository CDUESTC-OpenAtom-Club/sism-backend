package com.sism.analytics;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {
        "com.sism.analytics.domain",
        "com.sism.strategy.domain",
        "com.sism.organization.domain",
        "com.sism.alert.domain"
})
public class TestApplication {
}
