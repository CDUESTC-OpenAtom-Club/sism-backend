package com.sism.strategy.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Plan Aggregate Root Tests")
class PlanTest {

    @Test
    @DisplayName("Should create Plan with valid parameters")
    void shouldCreatePlanWithValidParameters() {
        Plan plan = Plan.create(1L, 1L, 1L, PlanLevel.COMPREHENSIVE);

        assertNotNull(plan);
        assertEquals(1L, plan.getCycleId());
        assertEquals(1L, plan.getTargetOrgId());
        assertEquals(1L, plan.getCreatedByOrgId());
        assertEquals(PlanLevel.COMPREHENSIVE, plan.getPlanLevel());
        assertEquals("DRAFT", plan.getStatus());
        assertFalse(plan.getIsDeleted());
        assertNotNull(plan.getCreatedAt());
        assertNotNull(plan.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw exception when creating Plan with null cycle id")
    void shouldThrowExceptionWhenCreatingPlanWithNullCycleId() {
        assertThrows(IllegalArgumentException.class, () ->
            Plan.create(null, 1L, 1L, PlanLevel.COMPREHENSIVE)
        );
    }

    @Test
    @DisplayName("Should activate Plan successfully")
    void shouldActivatePlanSuccessfully() {
        Plan plan = Plan.create(1L, 1L, 1L, PlanLevel.COMPREHENSIVE);

        plan.activate();

        assertEquals("ACTIVE", plan.getStatus());
        assertNotNull(plan.getUpdatedAt());
    }

    @Test
    @DisplayName("Should complete Plan successfully")
    void shouldCompletePlanSuccessfully() {
        Plan plan = Plan.create(1L, 1L, 1L, PlanLevel.COMPREHENSIVE);
        plan.activate();

        plan.complete();

        assertEquals("COMPLETED", plan.getStatus());
        assertNotNull(plan.getUpdatedAt());
    }

    @Test
    @DisplayName("Should cancel Plan successfully")
    void shouldCancelPlanSuccessfully() {
        Plan plan = Plan.create(1L, 1L, 1L, PlanLevel.COMPREHENSIVE);
        plan.activate();

        plan.cancel();

        assertEquals("CANCELLED", plan.getStatus());
        assertNotNull(plan.getUpdatedAt());
    }

    @Test
    @DisplayName("Should validate Plan with valid parameters")
    void shouldValidatePlanWithValidParameters() {
        Plan plan = Plan.create(1L, 1L, 1L, PlanLevel.COMPREHENSIVE);

        assertDoesNotThrow(plan::validate);
    }

    @Test
    @DisplayName("Should validate Plan with invalid parameters")
    void shouldValidatePlanWithInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            Plan.create(null, null, null, null);
        });
    }
}
