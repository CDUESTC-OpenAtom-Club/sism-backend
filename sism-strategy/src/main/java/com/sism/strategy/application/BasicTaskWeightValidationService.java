package com.sism.strategy.application;

import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.plan.PlanLevel;
import com.sism.strategy.domain.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the "basic task indicators must sum to 100" rule for a plan + target organization.
 */
@Service
@RequiredArgsConstructor
public class BasicTaskWeightValidationService {

    private static final BigDecimal REQUIRED_TOTAL_WEIGHT = BigDecimal.valueOf(100);

    private final JdbcTemplate jdbcTemplate;
    private final IndicatorRepository indicatorRepository;
    private final PlanRepository planRepository;

    public void validatePlanBasicWeight(Long planId, Long targetOrgId) {
        if (planId == null || targetOrgId == null) {
            return;
        }

        Set<Long> basicTaskIds = jdbcTemplate.queryForList("""
                        SELECT t.task_id
                        FROM public.sys_task t
                        WHERE t.plan_id = ?
                          AND COALESCE(t.is_deleted, false) = false
                          AND t.task_type = 'BASIC'
                        """,
                Long.class,
                planId
        ).stream().collect(Collectors.toSet());

        if (basicTaskIds.isEmpty()) {
            validateFuncToCollegeIndicators(planId, targetOrgId);
            return;
        }

        BigDecimal totalWeight = indicatorRepository.findAll().stream()
                .filter(indicator -> !Boolean.TRUE.equals(indicator.getIsDeleted()))
                .filter(indicator -> indicator.getParentIndicatorId() == null)
                .filter(indicator -> indicator.getTaskId() != null && basicTaskIds.contains(indicator.getTaskId()))
                .filter(indicator -> indicator.getTargetOrg() != null)
                .filter(indicator -> targetOrgId.equals(indicator.getTargetOrg().getId()))
                .map(Indicator::getWeightPercent)
                .filter(weight -> weight != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(REQUIRED_TOTAL_WEIGHT) != 0) {
            throw new IllegalStateException(
                    "基础性任务指标权重合计必须为100，当前为" + totalWeight.stripTrailingZeros().toPlainString()
            );
        }
    }

    private void validateFuncToCollegeIndicators(Long planId, Long targetOrgId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        if (plan.getPlanLevel() != PlanLevel.FUNC_TO_COLLEGE) {
            throw new IllegalStateException("当前计划不存在基础性任务，不能下发");
        }

        BigDecimal totalWeight = indicatorRepository.findByOwnerOrgIdAndTargetOrgId(
                        plan.getCreatedByOrgId(),
                        targetOrgId
                ).stream()
                .filter(indicator -> !Boolean.TRUE.equals(indicator.getIsDeleted()))
                .filter(indicator -> indicator.getParentIndicatorId() != null)
                .map(Indicator::getWeightPercent)
                .filter(weight -> weight != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("当前计划不存在学院指标，不能下发");
        }

        if (totalWeight.compareTo(REQUIRED_TOTAL_WEIGHT) != 0) {
            throw new IllegalStateException(
                    "学院指标权重合计必须为100，当前为" + totalWeight.stripTrailingZeros().toPlainString()
            );
        }
    }
}
