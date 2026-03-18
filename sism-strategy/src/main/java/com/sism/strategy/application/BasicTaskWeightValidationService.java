package com.sism.strategy.application;

import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
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

    private final TaskRepository taskRepository;
    private final IndicatorRepository indicatorRepository;

    public void validatePlanBasicWeight(Long planId, Long targetOrgId) {
        if (planId == null || targetOrgId == null) {
            return;
        }

        List<StrategicTask> basicTasks = taskRepository.findByPlanId(planId).stream()
                .filter(task -> !Boolean.TRUE.equals(task.getIsDeleted()))
                .filter(task -> task.getTaskType() == TaskType.BASIC)
                .toList();

        if (basicTasks.isEmpty()) {
            throw new IllegalStateException("当前计划不存在基础性任务，不能下发");
        }

        Set<Long> basicTaskIds = basicTasks.stream()
                .map(StrategicTask::getId)
                .collect(Collectors.toSet());

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
}
