package com.sism.execution.domain.service;

import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import com.sism.execution.domain.repository.PlanRepository;
import com.sism.execution.domain.event.PlanCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PlanDomainService - 计划领域服务
 * 处理计划激活、状态流转等复杂业务逻辑
 */
@Service
@RequiredArgsConstructor
public class PlanDomainService {

    private final PlanRepository planRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建计划
     */
    @Transactional
    public Plan createPlan(Long cycleId, Long targetOrgId, Long createdByOrgId, PlanLevel planLevel) {
        Plan plan = Plan.create(cycleId, targetOrgId, createdByOrgId, planLevel);
        Plan saved = planRepository.save(plan);
        eventPublisher.publishEvent(new PlanCreatedEvent(saved.getId(), cycleId, targetOrgId));
        return saved;
    }

    /**
     * 激活计划
     */
    @Transactional
    public Plan activate(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        plan.activate();
        return planRepository.save(plan);
    }

    /**
     * 关闭计划
     */
    @Transactional
    public Plan close(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        plan.close();
        return planRepository.save(plan);
    }

    /**
     * 查询目标组织的所有计划
     */
    public List<Plan> findByTargetOrg(Long orgId) {
        return planRepository.findByTargetOrgId(orgId);
    }

    /**
     * 查询某周期的所有计划
     */
    public List<Plan> findByCycle(Long cycleId) {
        return planRepository.findByCycleId(cycleId);
    }

    /**
     * 查询某层级的所有计划
     */
    public List<Plan> findByLevel(PlanLevel level) {
        return planRepository.findByPlanLevel(level);
    }

    /**
     * 查询某状态的所有计划
     */
    public List<Plan> findByStatus(String status) {
        return planRepository.findByStatus(status);
    }
}
