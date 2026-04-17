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
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id));
        return cycle;
    }

    public List<Cycle> getCyclesByStatus(String status) {
        if (status == null || status.isBlank()) {
            return cycleRepository.findAll();
        }
        String expectedStatus = status.trim().toUpperCase();
        return cycleRepository.findAll().stream()
                .filter(cycle -> expectedStatus.equals(cycle.deriveStatus()))
                .toList();
    }

    public List<Cycle> getCyclesByYear(Integer year) {
        return cycleRepository.findByYear(year);
    }

    public List<Cycle> getCyclesByStatusAndYear(String status, Integer year) {
        if (status == null || status.isBlank()) {
            return cycleRepository.findByYear(year);
        }
        String expectedStatus = status.trim().toUpperCase();
        return cycleRepository.findByYear(year).stream()
                .filter(cycle -> expectedStatus.equals(cycle.deriveStatus()))
                .toList();
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
        Cycle savedCycle = cycleRepository.save(cycle);
        return savedCycle;
    }

    @Transactional
    public Cycle deactivateCycle(Long id) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id));
        cycle.deactivate();
        Cycle savedCycle = cycleRepository.save(cycle);
        return savedCycle;
    }

    @Transactional
    public void deleteCycle(Long id) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id));
        cycleRepository.delete(cycle);
    }

    /**
     * 获取所有可用的年份（用于年份选择器）
     * 返回从数据库中实际存在的年份
     */
    public List<Integer> getAvailableYears() {
        return cycleRepository.findDistinctYears();
    }
}
