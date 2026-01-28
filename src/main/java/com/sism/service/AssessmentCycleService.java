package com.sism.service;

import com.sism.entity.AssessmentCycle;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.AssessmentCycleRepository;
import com.sism.vo.AssessmentCycleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Assessment Cycle management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentCycleService {

    private final AssessmentCycleRepository assessmentCycleRepository;

    /**
     * Get all assessment cycles ordered by year descending
     */
    @Transactional(readOnly = true)
    public List<AssessmentCycleVO> getAllCycles() {
        log.debug("Getting all assessment cycles");
        List<AssessmentCycle> cycles = assessmentCycleRepository.findAllByOrderByYearDesc();
        return cycles.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * Get assessment cycle by ID
     */
    @Transactional(readOnly = true)
    public AssessmentCycleVO getCycleById(Long cycleId) {
        log.debug("Getting assessment cycle by ID: {}", cycleId);
        AssessmentCycle cycle = assessmentCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("AssessmentCycle", cycleId));
        return toVO(cycle);
    }

    /**
     * Get assessment cycle by year
     */
    @Transactional(readOnly = true)
    public AssessmentCycleVO getCycleByYear(Integer year) {
        log.debug("Getting assessment cycle by year: {}", year);
        AssessmentCycle cycle = assessmentCycleRepository.findByYear(year)
                .orElseThrow(() -> new ResourceNotFoundException("AssessmentCycle for year " + year));
        return toVO(cycle);
    }

    /**
     * Get active or future assessment cycles
     */
    @Transactional(readOnly = true)
    public List<AssessmentCycleVO> getActiveOrFutureCycles() {
        log.debug("Getting active or future assessment cycles");
        LocalDate currentDate = LocalDate.now();
        List<AssessmentCycle> cycles = assessmentCycleRepository.findActiveOrFutureCycles(currentDate);
        return cycles.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * Get assessment cycle that contains a specific date
     */
    @Transactional(readOnly = true)
    public AssessmentCycleVO getCycleByDate(LocalDate date) {
        log.debug("Getting assessment cycle by date: {}", date);
        AssessmentCycle cycle = assessmentCycleRepository.findByDateInRange(date)
                .orElseThrow(() -> new ResourceNotFoundException("AssessmentCycle for date " + date));
        return toVO(cycle);
    }

    /**
     * Convert entity to VO
     */
    private AssessmentCycleVO toVO(AssessmentCycle cycle) {
        return AssessmentCycleVO.builder()
                .cycleId(cycle.getCycleId())
                .cycleName(cycle.getCycleName())
                .year(cycle.getYear())
                .startDate(cycle.getStartDate())
                .endDate(cycle.getEndDate())
                .description(cycle.getDescription())
                .createdAt(cycle.getCreatedAt())
                .updatedAt(cycle.getUpdatedAt())
                .build();
    }
}
