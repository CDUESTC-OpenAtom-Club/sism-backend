package com.sism.strategy.application;

import com.sism.execution.domain.model.milestone.Milestone;
import com.sism.execution.domain.repository.MilestoneRepository;
import com.sism.organization.domain.SysOrg;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyApplicationService {

    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;
    private final IndicatorRepository indicatorRepository;
    private final MilestoneRepository milestoneRepository;

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

    public Page<Indicator> searchIndicators(String keyword, Pageable pageable) {
        return indicatorRepository.findByKeyword(keyword, pageable);
    }

    public List<Indicator> getIndicatorsByTaskId(Long taskId) {
        return indicatorRepository.findByTaskId(taskId);
    }

    @Transactional
    public List<Milestone> createMilestones(Long indicatorId, List<com.sism.strategy.interfaces.rest.IndicatorController.MilestoneRequest> requests) {
        Indicator indicator = indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + indicatorId));
        List<Milestone> milestones = new ArrayList<>();
        for (com.sism.strategy.interfaces.rest.IndicatorController.MilestoneRequest request : requests) {
            Milestone milestone = new Milestone();
            milestone.setIndicator(indicator);
            milestone.setMonth(request.getMonth());
            milestone.setTargetProgress(request.getTargetProgress());
            if (request.getDeadline() != null) {
                milestone.setDeadline(LocalDate.parse(request.getDeadline()));
            }
            milestone = milestoneRepository.save(milestone);
            milestones.add(milestone);
        }
        return milestones;
    }

    public List<Milestone> getMilestonesByIndicatorId(Long indicatorId) {
        return milestoneRepository.findByIndicatorId(indicatorId);
    }

    public boolean isMilestonePaired(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId).orElse(null);
        return milestone != null && milestone.getReport() != null;
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
