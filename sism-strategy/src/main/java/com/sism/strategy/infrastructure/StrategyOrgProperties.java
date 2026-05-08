package com.sism.strategy.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sism.strategy.org")
public class StrategyOrgProperties {

    /**
     * Strategy organization ID used for strategy-to-functional plan creation.
     */
    private Long strategyOrgId = 35L;

    /**
     * System admin organization ID used by integrity checks.
     */
    private Long systemAdminOrgId = 35L;
}
