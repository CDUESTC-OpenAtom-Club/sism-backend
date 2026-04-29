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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanIntegrityService {

    private static final long ENSURE_INTERVAL_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final Object STRAT_TO_FUNC_LOCK = new Object();
    private static final Object FUNC_TO_COLLEGE_LOCK = new Object();

    private final PlanRepository planRepository;
    private final CycleRepository cycleRepository;
    private final OrganizationRepository organizationRepository;
    private final StrategyOrgProperties strategyOrgProperties;
    private final AtomicBoolean ensureInProgress = new AtomicBoolean(false);
    private volatile long lastEnsuredAtMillis = -1L;

    @Transactional
    public void ensurePlanMatrix() {
        long now = System.currentTimeMillis();
        if (isRecentlyEnsured(now)) {
            return;
        }

        if (!ensureInProgress.compareAndSet(false, true)) {
            log.debug("[PlanIntegrityService] Skip ensurePlanMatrix because another execution is in progress");
            return;
        }

        List<Cycle> cycles = cycleRepository.findAll();
        try {
            now = System.currentTimeMillis();
            if (isRecentlyEnsured(now)) {
                return;
            }

            List<SysOrg> organizations = organizationRepository.findAll().stream()
                    .filter(org -> Boolean.TRUE.equals(org.getIsActive()))
                    .filter(org -> !Boolean.TRUE.equals(org.getIsDeleted()))
                    .toList();

            List<SysOrg> functionalOrgs = organizations.stream()
                    .filter(org -> OrgType.functional.equals(org.getType()))
                    .toList();
            List<SysOrg> academicOrgs = organizations.stream()
                    .filter(org -> OrgType.academic.equals(org.getType()))
                    .toList();

            for (Cycle cycle : cycles) {
                ensureStrategyToFunctionalPlans(cycle, functionalOrgs);
                ensureFunctionalToCollegePlans(cycle, functionalOrgs, academicOrgs);
            }

            markEnsureCompletedAfterCommit();
            log.info(
                    "[PlanIntegrityService] Ensured plan matrix for cycles={}, functionalOrgs={}, academicOrgs={}",
                    cycles.size(),
                    functionalOrgs.size(),
                    academicOrgs.size()
            );
        } finally {
            ensureInProgress.set(false);
        }
    }

    private boolean isRecentlyEnsured(long nowMillis) {
        return lastEnsuredAtMillis > 0
                && nowMillis - lastEnsuredAtMillis < ENSURE_INTERVAL_MILLIS;
    }

    private void markEnsureCompletedAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            lastEnsuredAtMillis = System.currentTimeMillis();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                lastEnsuredAtMillis = System.currentTimeMillis();
            }
        });
    }

    private void ensureStrategyToFunctionalPlans(Cycle cycle, List<SysOrg> functionalOrgs) {
        synchronized (STRAT_TO_FUNC_LOCK) {
            for (SysOrg functionalOrg : functionalOrgs) {
                ensurePlanExists(
                        cycle.getId(),
                        PlanLevel.STRAT_TO_FUNC,
                        strategyOrgProperties.getSystemAdminOrgId(),
                        functionalOrg.getId()
                );
            }
        }
    }

    private void ensureFunctionalToCollegePlans(Cycle cycle, List<SysOrg> functionalOrgs, List<SysOrg> academicOrgs) {
        synchronized (FUNC_TO_COLLEGE_LOCK) {
            for (SysOrg functionalOrg : functionalOrgs) {
                for (SysOrg academicOrg : academicOrgs) {
                    ensurePlanExists(
                            cycle.getId(),
                            PlanLevel.FUNC_TO_COLLEGE,
                            functionalOrg.getId(),
                            academicOrg.getId()
                    );
                }
            }
        }
    }

    private void ensurePlanExists(Long cycleId, PlanLevel planLevel, Long createdByOrgId, Long targetOrgId) {
        List<Plan> activePlans = planRepository.findActiveByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                cycleId,
                planLevel,
                createdByOrgId,
                targetOrgId
        );

        if (activePlans.size() > 1) {
            log.error(
                    "[PlanIntegrityService] Duplicate active plans detected: planIds={}, cycleId={}, planLevel={}, createdByOrgId={}, targetOrgId={}",
                    activePlans.stream().map(Plan::getId).toList(),
                    cycleId,
                    planLevel,
                    createdByOrgId,
                    targetOrgId
            );
        }

        if (!activePlans.isEmpty()) {
            return;
        }

        Plan createdPlan = Plan.create(cycleId, targetOrgId, createdByOrgId, planLevel);
        try {
            planRepository.saveAndFlush(createdPlan);
            log.info(
                    "[PlanIntegrityService] Auto-created missing plan: cycleId={}, planLevel={}, createdByOrgId={}, targetOrgId={}",
                    cycleId,
                    planLevel,
                    createdByOrgId,
                    targetOrgId
            );
        } catch (DataIntegrityViolationException ex) {
            List<Plan> concurrentPlans = planRepository.findActiveByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                    cycleId,
                    planLevel,
                    createdByOrgId,
                    targetOrgId
            );
            if (!concurrentPlans.isEmpty()) {
                log.info(
                        "[PlanIntegrityService] Detected concurrent plan creation, reusing existing active plan: planId={}, cycleId={}, planLevel={}, createdByOrgId={}, targetOrgId={}",
                        concurrentPlans.get(0).getId(),
                        cycleId,
                        planLevel,
                        createdByOrgId,
                        targetOrgId
                );
                return;
            }
            throw ex;
        }
    }
}
