package com.sism.main.bootstrap;

import com.sism.strategy.application.PlanIntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanIntegrityBootstrap implements ApplicationRunner {

    private final PlanIntegrityService planIntegrityService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[PlanIntegrityBootstrap] Ensuring baseline plan matrix");
        planIntegrityService.ensurePlanMatrix();
    }
}
