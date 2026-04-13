package com.sism.main.bootstrap;

import com.sism.strategy.application.PlanIntegrityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        ReflectionTestUtils.setField(bootstrap, "failFastOnStartup", true);
        doThrow(new RuntimeException("boom")).when(planIntegrityService).ensurePlanMatrix();

        assertThrows(IllegalStateException.class, () -> bootstrap.run(applicationArguments));
    }

    @Test
    @DisplayName("Should continue startup when fail fast is disabled")
    void shouldContinueStartupWhenFailFastIsDisabled() {
        PlanIntegrityBootstrap bootstrap = new PlanIntegrityBootstrap(planIntegrityService);
        ReflectionTestUtils.setField(bootstrap, "failFastOnStartup", false);
        doThrow(new RuntimeException("boom")).when(planIntegrityService).ensurePlanMatrix();

        assertDoesNotThrow(() -> bootstrap.run(applicationArguments));
    }
}
