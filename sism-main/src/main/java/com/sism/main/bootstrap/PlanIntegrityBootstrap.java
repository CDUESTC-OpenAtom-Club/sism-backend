package com.sism.main.bootstrap;

import com.sism.strategy.application.PlanIntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanIntegrityBootstrap implements ApplicationRunner {

    private final PlanIntegrityService planIntegrityService;
    @Value("${app.plan-integrity.fail-fast-on-startup:false}")
    private boolean failFastOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[PlanIntegrityBootstrap] Ensuring baseline plan matrix");
        try {
            planIntegrityService.ensurePlanMatrix();
        } catch (Exception e) {
            log.error("[PlanIntegrityBootstrap] Failed to ensure plan matrix", e);
            if (failFastOnStartup) {
                throw new IllegalStateException("Failed to ensure baseline plan matrix", e);
            }
            log.warn("[PlanIntegrityBootstrap] Continuing startup because fail-fast is disabled");
        }
    }
}
