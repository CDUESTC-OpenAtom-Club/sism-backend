package com.sism.strategy.domain.service;

import com.sism.strategy.domain.indicator.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * IndicatorDomainService - 指标领域服务
 * 处理指标下发、分解、状态流转等复杂业务逻辑
 */
@Service
@RequiredArgsConstructor
public class IndicatorDomainService {

    private final IndicatorRepository indicatorRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * 下发指标到目标组织
     */
    @Transactional
    public Indicator distribute(Long sourceIndicatorId, Long targetOrgId, Double targetValue) {
        Indicator sourceIndicator = indicatorRepository.findById(sourceIndicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Source indicator not found"));

        SysOrg targetOrg = organizationRepository.findById(targetOrgId)
                .orElseThrow(() -> new IllegalArgumentException("Target organization not found"));

        // 创建新指标并从源指标复制属性
        Indicator newIndicator = Indicator.create(
                sourceIndicator.getDescription(),
                sourceIndicator.getOwnerOrg(),
                targetOrg,
                sourceIndicator.getType()
        );
        newIndicator.setParent(sourceIndicator);
        if (targetValue != null) {
            newIndicator.setWeight(java.math.BigDecimal.valueOf(targetValue));
        }

        // 直接调用实例方法
        newIndicator.distributeFrom(sourceIndicator, targetOrg, targetValue);

        return indicatorRepository.save(newIndicator);
    }

    /**
     * 分解指标（战略 -> 职能 -> 学院）
     */
    @Transactional
    public List<Indicator> breakdown(Long parentIndicatorId, List<BreakdownItem> breakdownItems) {
        Indicator parent = indicatorRepository.findById(parentIndicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Parent indicator not found"));

        if (!parent.canBreakdown()) {
            throw new IllegalStateException("Indicator cannot be broken down in current status");
        }

        List<Indicator> children = breakdownItems.stream()
                .map(item -> {
                    // 创建新指标并从父指标复制属性
                    Indicator child = Indicator.create(
                            parent.getDescription(),
                            parent.getTargetOrg(),
                            item.targetOrg(),
                            parent.getType()
                    );
                    child.setParent(parent);
                    if (item.targetValue() != null) {
                        child.setWeight(java.math.BigDecimal.valueOf(item.targetValue()));
                    }
                    return indicatorRepository.save(child);
                })
                .toList();

        parent.markAsBrokenDown();
        indicatorRepository.save(parent);

        return children;
    }

    /**
     * 激活指标
     */
    @Transactional
    public Indicator activate(Long indicatorId) {
        Indicator indicator = indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found"));

        indicator.activate();
        return indicatorRepository.save(indicator);
    }

    /**
     * 终止指标
     */
    @Transactional
    public Indicator terminate(Long indicatorId, String reason) {
        Indicator indicator = indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found"));

        indicator.terminate(reason);
        return indicatorRepository.save(indicator);
    }

    /**
     * 查询某组织的一级指标
     */
    public List<Indicator> findFirstLevelIndicators(Long orgId) {
        return indicatorRepository.findByOwnerOrgId(orgId).stream()
                .filter(i -> i.getParentIndicatorId() == null)
                .toList();
    }

    /**
     * 查询某组织的二级指标
     */
    public List<Indicator> findSecondLevelIndicators(Long parentIndicatorId) {
        return indicatorRepository.findByParentIndicatorId(parentIndicatorId);
    }

    /**
     * 分解明细项
     */
    public record BreakdownItem(SysOrg targetOrg, Double targetValue) {}
}
