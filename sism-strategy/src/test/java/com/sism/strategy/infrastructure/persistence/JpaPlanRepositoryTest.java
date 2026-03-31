package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.plan.PlanLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Jpa Plan Repository Tests")
class JpaPlanRepositoryTest {

    @Mock
    private JpaPlanRepositoryInternal jpaRepository;

    @Test
    @DisplayName("Should use generic page query when status is blank")
    void shouldUseGenericPageQueryWhenStatusIsBlank() {
        JpaPlanRepository repository = new JpaPlanRepository(jpaRepository);
        PageRequest pageable = PageRequest.of(0, 20);
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);

        when(jpaRepository.findPage(eq(List.of(2026L)), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(plan), pageable, 1));

        var result = repository.findPage(List.of(2026L), List.of(), pageable);

        assertEquals(1, result.getTotalElements());
        verify(jpaRepository).findPage(List.of(2026L), false, pageable);
        verify(jpaRepository, never()).findPageByStatus(any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("Should use status-specific page query when status is provided")
    void shouldUseStatusSpecificPageQueryWhenStatusIsProvided() {
        JpaPlanRepository repository = new JpaPlanRepository(jpaRepository);
        PageRequest pageable = PageRequest.of(0, 20);
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);

        when(jpaRepository.findPageByStatus(eq(List.of(2026L)), eq(false), eq(List.of("DISTRIBUTED", "APPROVED")), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(plan), pageable, 1));

        var result = repository.findPage(List.of(2026L), List.of("DISTRIBUTED", "APPROVED"), pageable);

        assertEquals(1, result.getTotalElements());
        verify(jpaRepository).findPageByStatus(List.of(2026L), false, List.of("DISTRIBUTED", "APPROVED"), pageable);
        verify(jpaRepository, never()).findPage(any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("Should skip cycle filter when cycle ids are empty")
    void shouldSkipCycleFilterWhenCycleIdsAreEmpty() {
        JpaPlanRepository repository = new JpaPlanRepository(jpaRepository);
        PageRequest pageable = PageRequest.of(0, 20);

        when(jpaRepository.findPage(eq(List.of()), eq(true), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        var result = repository.findPage(List.of(), List.of(), pageable);

        assertEquals(0, result.getTotalElements());
        verify(jpaRepository).findPage(List.of(), true, pageable);
    }

    @Test
    @DisplayName("Should prefer active plan list when duplicate rows exist for same business key")
    void shouldPreferActivePlanListWhenDuplicateRowsExistForSameBusinessKey() {
        JpaPlanRepository repository = new JpaPlanRepository(jpaRepository);
        Plan canonicalPlan = Plan.create(4L, 36L, 35L, PlanLevel.STRAT_TO_FUNC);
        Plan duplicatePlan = Plan.create(4L, 36L, 35L, PlanLevel.STRAT_TO_FUNC);

        when(jpaRepository.findActiveByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                4L, PlanLevel.STRAT_TO_FUNC, 35L, 36L))
                .thenReturn(List.of(canonicalPlan, duplicatePlan));

        Optional<Plan> result = repository.findByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                4L, PlanLevel.STRAT_TO_FUNC, 35L, 36L);

        assertTrue(result.isPresent());
        assertSame(canonicalPlan, result.orElseThrow());
        verify(jpaRepository).findActiveByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                4L, PlanLevel.STRAT_TO_FUNC, 35L, 36L);
        verify(jpaRepository, never()).findByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                4L, PlanLevel.STRAT_TO_FUNC, 35L, 36L);
    }

    @Test
    @DisplayName("Should fall back to legacy single lookup when no active plan exists")
    void shouldFallBackToLegacySingleLookupWhenNoActivePlanExists() {
        JpaPlanRepository repository = new JpaPlanRepository(jpaRepository);
        Plan legacyPlan = Plan.create(4L, 36L, 35L, PlanLevel.STRAT_TO_FUNC);

        when(jpaRepository.findActiveByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                4L, PlanLevel.STRAT_TO_FUNC, 35L, 36L))
                .thenReturn(List.of());
        when(jpaRepository.findByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                4L, PlanLevel.STRAT_TO_FUNC, 35L, 36L))
                .thenReturn(Optional.of(legacyPlan));

        Optional<Plan> result = repository.findByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                4L, PlanLevel.STRAT_TO_FUNC, 35L, 36L);

        assertSame(legacyPlan, result.orElseThrow());
        verify(jpaRepository).findByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                4L, PlanLevel.STRAT_TO_FUNC, 35L, 36L);
    }
}
