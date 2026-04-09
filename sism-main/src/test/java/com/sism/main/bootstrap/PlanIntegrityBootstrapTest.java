package com.sism.main.bootstrap;

import com.sism.strategy.application.PlanIntegrityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("Plan Integrity Bootstrap Tests")
class PlanIntegrityBootstrapTest {

    @Mock
    private PlanIntegrityService planIntegrityService;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    @DisplayName("Should fail fast when baseline plan matrix initialization fails")
    void shouldFailFastWhenInitializationFails() {
        PlanIntegrityBootstrap bootstrap = new PlanIntegrityBootstrap(planIntegrityService);
        doThrow(new RuntimeException("boom")).when(planIntegrityService).ensurePlanMatrix();

        assertThrows(IllegalStateException.class, () -> bootstrap.run(applicationArguments));
    }
}
