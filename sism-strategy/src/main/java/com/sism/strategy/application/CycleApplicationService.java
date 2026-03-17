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
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CycleApplicationService {

    private final CycleRepository cycleRepository;

    public Page<Cycle> getAllCycles(Pageable pageable) {
        Page<Cycle> page = cycleRepository.findAll(pageable);
        page.forEach(this::normalizeCycle);
        return page;
    }

    public List<Cycle> getAllCyclesList() {
        return cycleRepository.findAll().stream()
                .peek(this::normalizeCycle)
                .toList();
    }

    public Cycle getCycleById(Long id) {
        Cycle cycle = cycleRepository.findById(id).orElse(null);
        if (cycle != null) {
            normalizeCycle(cycle);
        }
        return cycle;
    }

    public List<Cycle> getCyclesByStatus(String status) {
        return cycleRepository.findAll().stream()
                .peek(this::normalizeCycle)
                .filter(cycle -> matchesStatus(cycle, status))
                .toList();
    }

    public List<Cycle> getCyclesByYear(Integer year) {
        return cycleRepository.findByYear(year).stream()
                .peek(this::normalizeCycle)
                .toList();
    }

    public List<Cycle> getCyclesByStatusAndYear(String status, Integer year) {
        return cycleRepository.findByYear(year).stream()
                .peek(this::normalizeCycle)
                .filter(cycle -> matchesStatus(cycle, status))
                .toList();
    }

    @Transactional
    public Cycle createCycle(String name, Integer year, LocalDate startDate, LocalDate endDate) {
        Cycle cycle = Cycle.create(name, year, startDate, endDate);
        Cycle savedCycle = cycleRepository.save(cycle);
        normalizeCycle(savedCycle);
        return savedCycle;
    }

    @Transactional
    public Cycle activateCycle(Long id) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id));
        cycle.activate();
        Cycle savedCycle = cycleRepository.save(cycle);
        normalizeCycle(savedCycle);
        return savedCycle;
    }

    @Transactional
    public Cycle deactivateCycle(Long id) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id));
        cycle.deactivate();
        Cycle savedCycle = cycleRepository.save(cycle);
        normalizeCycle(savedCycle);
        return savedCycle;
    }

    @Transactional
    public void deleteCycle(Long id) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id));
        cycleRepository.delete(cycle);
    }

    private void normalizeCycle(Cycle cycle) {
        cycle.setStatus(cycle.deriveStatus());
        if (cycle.getIsDeleted() == null) {
            cycle.setIsDeleted(false);
        }
    }

    private boolean matchesStatus(Cycle cycle, String expectedStatus) {
        if (expectedStatus == null || expectedStatus.isBlank()) {
            return true;
        }
        return cycle.getStatus() != null
                && cycle.getStatus().equalsIgnoreCase(expectedStatus.trim().toUpperCase(Locale.ROOT));
    }
}
