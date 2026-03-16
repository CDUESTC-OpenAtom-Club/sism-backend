package com.sism.strategy.application;

import com.sism.organization.domain.SysOrg;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyApplicationService {

    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;
    private final IndicatorRepository indicatorRepository;

    @Transactional
    public Indicator createIndicator(String indicatorDesc, SysOrg ownerOrg, SysOrg targetOrg) {
        Indicator indicator = Indicator.create(indicatorDesc, ownerOrg, targetOrg);
        indicator.validate();
        indicator = indicatorRepository.save(indicator);
        publishAndSaveEvents(indicator);
        return indicator;
    }

    @Transactional
    public Indicator submitIndicatorForReview(Indicator indicator) {
        indicator.submitForReview();
        indicator = indicatorRepository.save(indicator);
        publishAndSaveEvents(indicator);
        return indicator;
    }

    @Transactional
    public Indicator approveIndicator(Indicator indicator) {
        indicator.approve();
        indicator = indicatorRepository.save(indicator);
        publishAndSaveEvents(indicator);
        return indicator;
    }

    @Transactional
    public Indicator rejectIndicator(Indicator indicator) {
        indicator.reject();
        indicator = indicatorRepository.save(indicator);
        publishAndSaveEvents(indicator);
        return indicator;
    }

    @Transactional
    public Indicator distributeIndicator(Long id) {
        Indicator indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + id));
        indicator.distribute();
        indicator = indicatorRepository.save(indicator);
        publishAndSaveEvents(indicator);
        return indicator;
    }

    @Transactional
    public Indicator withdrawIndicator(Long id, String reason) {
        Indicator indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + id));
        indicator.withdraw();
        indicator = indicatorRepository.save(indicator);
        publishAndSaveEvents(indicator);
        return indicator;
    }

    public Indicator getIndicatorById(Long id) {
        return indicatorRepository.findById(id).orElse(null);
    }

    public List<Indicator> getAllIndicators() {
        return indicatorRepository.findAll();
    }

    public List<Indicator> searchIndicators(String keyword) {
        return indicatorRepository.findByKeyword(keyword);
    }

    public List<Indicator> getIndicatorsByTaskId(Long taskId) {
        return indicatorRepository.findByTaskId(taskId);
    }

    @Transactional
    public Indicator breakdownIndicator(Long id) {
        Indicator indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + id));

        if (!indicator.canBreakdown()) {
            throw new IllegalStateException("Indicator cannot be broken down");
        }

        indicator.markAsBrokenDown();
        indicator = indicatorRepository.save(indicator);
        publishAndSaveEvents(indicator);
        return indicator;
    }

    @Transactional
    public Indicator activateIndicator(Long id) {
        Indicator indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + id));

        indicator.activate();
        indicator = indicatorRepository.save(indicator);
        publishAndSaveEvents(indicator);
        return indicator;
    }

    @Transactional
    public Indicator terminateIndicator(Long id, String reason) {
        Indicator indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + id));

        indicator.terminate(reason);
        indicator = indicatorRepository.save(indicator);
        publishAndSaveEvents(indicator);
        return indicator;
    }

    private void publishAndSaveEvents(com.sism.shared.domain.model.base.AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();

        for (DomainEvent event : events) {
            eventStore.save(event);
        }

        eventPublisher.publishAll(events);

        aggregate.clearEvents();
    }
}
