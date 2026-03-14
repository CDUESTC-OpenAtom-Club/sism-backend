package com.sism.strategy.application;

import com.sism.strategy.domain.Cycle;
import com.sism.strategy.domain.repository.CycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CycleApplicationService {

    private final CycleRepository cycleRepository;

    public Page<Cycle> getAllCycles(Pageable pageable) {
        return cycleRepository.findAll(pageable);
    }

    public List<Cycle> getAllCyclesList() {
        return cycleRepository.findAll();
    }

    public Cycle getCycleById(Long id) {
        return cycleRepository.findById(id).orElse(null);
    }

    public List<Cycle> getCyclesByStatus(String status) {
        return cycleRepository.findByStatus(status);
    }

    public List<Cycle> getCyclesByYear(Integer year) {
        return cycleRepository.findByYear(year);
    }

    public List<Cycle> getCyclesByStatusAndYear(String status, Integer year) {
        return cycleRepository.findByStatusAndYear(status, year);
    }

    @Transactional
    public Cycle createCycle(String name, Integer year, LocalDate startDate, LocalDate endDate) {
        Cycle cycle = Cycle.create(name, year, startDate, endDate);
        return cycleRepository.save(cycle);
    }

    @Transactional
    public Cycle activateCycle(Long id) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id));
        cycle.activate();
        return cycleRepository.save(cycle);
    }

    @Transactional
    public Cycle deactivateCycle(Long id) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id));
        cycle.deactivate();
        return cycleRepository.save(cycle);
    }

    @Transactional
    public void deleteCycle(Long id) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id));
        cycle.delete();
        cycleRepository.save(cycle);
    }
}
