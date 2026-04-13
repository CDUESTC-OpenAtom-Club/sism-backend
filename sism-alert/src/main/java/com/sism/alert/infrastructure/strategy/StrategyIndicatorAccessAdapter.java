package com.sism.alert.infrastructure.strategy;

import com.sism.alert.application.IndicatorAccessPort;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class StrategyIndicatorAccessAdapter implements IndicatorAccessPort {

    private final IndicatorRepository indicatorRepository;

    @Override
    public Set<Long> findAccessibleIndicatorIds(Long orgId) {
        Set<Long> indicatorIds = new LinkedHashSet<>();
        indicatorRepository.findByOwnerOrgId(orgId).stream()
                .map(Indicator::getId)
                .filter(Objects::nonNull)
                .forEach(indicatorIds::add);
        indicatorRepository.findByTargetOrgId(orgId).stream()
                .map(Indicator::getId)
                .filter(Objects::nonNull)
                .forEach(indicatorIds::add);
        return indicatorIds;
    }

    @Override
    public Set<Long> findAllIndicatorIds() {
        Set<Long> indicatorIds = new LinkedHashSet<>();
        indicatorRepository.findAll().stream()
                .map(Indicator::getId)
                .filter(Objects::nonNull)
                .forEach(indicatorIds::add);
        return indicatorIds;
    }
}
