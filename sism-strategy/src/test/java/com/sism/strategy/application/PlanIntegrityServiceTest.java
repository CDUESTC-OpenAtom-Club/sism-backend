package com.sism.strategy.application;

import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.OrganizationRepository;
import com.sism.strategy.domain.cycle.Cycle;
import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.plan.PlanLevel;
import com.sism.strategy.domain.repository.CycleRepository;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.strategy.infrastructure.StrategyOrgProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Plan Integrity Service Tests")
class PlanIntegrityServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private CycleRepository cycleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Test
    @DisplayName("Should skip repeated full matrix ensure within throttle window")
    void shouldSkipRepeatedEnsureWithinThrottleWindow() {
        PlanIntegrityService service = new PlanIntegrityService(
                planRepository,
                cycleRepository,
                organizationRepository,
                new StrategyOrgProperties()
        );

        Cycle cycle = new Cycle();
        cycle.setId(90L);

        SysOrg functionalOrg = new SysOrg();
        functionalOrg.setId(36L);
        functionalOrg.setType(OrgType.functional);
        functionalOrg.setIsActive(true);
        functionalOrg.setIsDeleted(false);

        SysOrg academicOrg = new SysOrg();
        academicOrg.setId(55L);
        academicOrg.setType(OrgType.academic);
        academicOrg.setIsActive(true);
        academicOrg.setIsDeleted(false);

        when(cycleRepository.findAll()).thenReturn(List.of(cycle));
        when(organizationRepository.findAll()).thenReturn(List.of(functionalOrg, academicOrg));
        when(planRepository.findActiveByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(any(), any(), any(), any()))
                .thenReturn(List.of(Plan.create(90L, 36L, 35L, PlanLevel.STRAT_TO_FUNC)));

        service.ensurePlanMatrix();
        service.ensurePlanMatrix();

        verify(cycleRepository, times(1)).findAll();
        verify(organizationRepository, times(1)).findAll();
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should tolerate concurrent duplicate creation when unique constraint rejects insert")
    void shouldReuseExistingPlanWhenConcurrentInsertWins() {
        PlanIntegrityService service = new PlanIntegrityService(
                planRepository,
                cycleRepository,
                organizationRepository,
                new StrategyOrgProperties()
        );

        Cycle cycle = new Cycle();
        cycle.setId(90L);

        SysOrg functionalOrg = new SysOrg();
        functionalOrg.setId(36L);
        functionalOrg.setType(OrgType.functional);
        functionalOrg.setIsActive(true);
        functionalOrg.setIsDeleted(false);

        when(cycleRepository.findAll()).thenReturn(List.of(cycle));
        when(organizationRepository.findAll()).thenReturn(List.of(functionalOrg));
        when(planRepository.findActiveByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                90L, PlanLevel.STRAT_TO_FUNC, 35L, 36L))
                .thenReturn(List.of())
                .thenReturn(List.of(existingPlan(90L, 36L, 35L, PlanLevel.STRAT_TO_FUNC, 501L)));
        when(planRepository.saveAndFlush(any(Plan.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        service.ensurePlanMatrix();

        verify(planRepository, times(1)).saveAndFlush(any(Plan.class));
    }

    @Test
    @DisplayName("Should update throttle timestamp only after commit when transaction synchronization is active")
    void shouldUpdateThrottleTimestampAfterCommit() {
        PlanIntegrityService service = new PlanIntegrityService(
                planRepository,
                cycleRepository,
                organizationRepository,
                new StrategyOrgProperties()
        );

        Cycle cycle = new Cycle();
        cycle.setId(90L);

        SysOrg functionalOrg = new SysOrg();
        functionalOrg.setId(36L);
        functionalOrg.setType(OrgType.functional);
        functionalOrg.setIsActive(true);
        functionalOrg.setIsDeleted(false);

        when(cycleRepository.findAll()).thenReturn(List.of(cycle));
        when(organizationRepository.findAll()).thenReturn(List.of(functionalOrg));
        when(planRepository.findActiveByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                90L, PlanLevel.STRAT_TO_FUNC, 35L, 36L))
                .thenReturn(List.of(Plan.create(90L, 36L, 35L, PlanLevel.STRAT_TO_FUNC)));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.ensurePlanMatrix();
            service.ensurePlanMatrix();
            verify(cycleRepository, times(2)).findAll();

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        service.ensurePlanMatrix();
        verify(cycleRepository, times(2)).findAll();
    }

    private static Plan existingPlan(Long cycleId, Long targetOrgId, Long createdByOrgId, PlanLevel planLevel, Long id) {
        Plan plan = Plan.create(cycleId, targetOrgId, createdByOrgId, planLevel);
        plan.setId(id);
        return plan;
    }
}
