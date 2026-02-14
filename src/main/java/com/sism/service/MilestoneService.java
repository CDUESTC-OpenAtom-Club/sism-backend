package com.sism.service;

import com.sism.dto.MilestoneCreateRequest;
import com.sism.dto.MilestoneUpdateRequest;
import com.sism.entity.Indicator;
import com.sism.entity.Milestone;
import com.sism.enums.MilestoneStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.MilestoneRepository;
import com.sism.vo.MilestoneVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for milestone management
 * Provides CRUD operations with weight validation
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final IndicatorService indicatorService;
    private final IndicatorRepository indicatorRepository;

    private static final BigDecimal WEIGHT_SUM_TARGET = new BigDecimal("100.00");

    /**
     * Get all milestones (optimized with batch loading)
     * 
     * @return list of all milestones
     */
    public List<MilestoneVO> getAllMilestones() {
        List<Milestone> milestones = milestoneRepository.findAll();
        log.info("Found {} total milestones", milestones.size());
        return toMilestoneVOsBatch(milestones);
    }
    
    /**
     * Batch convert Milestone entities to VOs with optimized queries
     */
    private List<MilestoneVO> toMilestoneVOsBatch(List<Milestone> milestones) {
        if (milestones.isEmpty()) {
            return List.of();
        }
        
        // 收集所有indicatorId
        Set<Long> indicatorIds = milestones.stream()
                .map(m -> m.getIndicator().getIndicatorId())
                .collect(Collectors.toSet());
        
        // 批量查询所有indicator
        List<Indicator> indicators = indicatorRepository.findAllById(indicatorIds);
        Map<Long, Indicator> indicatorMap = indicators.stream()
                .collect(Collectors.toMap(Indicator::getIndicatorId, i -> i));
        
        // 收集所有inheritedFromId
        Set<Long> inheritedFromIds = milestones.stream()
                .filter(m -> m.getInheritedFrom() != null)
                .map(m -> m.getInheritedFrom().getMilestoneId())
                .collect(Collectors.toSet());
        
        // 批量查询所有inheritedFrom milestones
        List<Milestone> inheritedMilestones = inheritedFromIds.isEmpty() 
            ? List.of() 
            : milestoneRepository.findAllById(inheritedFromIds);
        Map<Long, Milestone> inheritedMap = inheritedMilestones.stream()
                .collect(Collectors.toMap(Milestone::getMilestoneId, m -> m));
        
        // 组装VO
        return milestones.stream()
                .map(milestone -> {
                    MilestoneVO vo = new MilestoneVO();
                    vo.setMilestoneId(milestone.getMilestoneId());
                    
                    // 从缓存Map中获取indicator
                    Indicator indicator = indicatorMap.get(milestone.getIndicator().getIndicatorId());
                    if (indicator != null) {
                        vo.setIndicatorId(indicator.getIndicatorId());
                        vo.setIndicatorDesc(indicator.getIndicatorDesc());
                    }
                    
                    vo.setMilestoneName(milestone.getMilestoneName());
                    vo.setMilestoneDesc(milestone.getMilestoneDesc());
                    vo.setDueDate(milestone.getDueDate());
                    vo.setStatus(milestone.getStatus());
                    vo.setSortOrder(milestone.getSortOrder());
                    
                    // 从缓存Map中获取inheritedFrom
                    if (milestone.getInheritedFrom() != null) {
                        Milestone inherited = inheritedMap.get(milestone.getInheritedFrom().getMilestoneId());
                        if (inherited != null) {
                            vo.setInheritedFromId(inherited.getMilestoneId());
                        }
                    }
                    
                    vo.setCreatedAt(milestone.getCreatedAt());
                    vo.setUpdatedAt(milestone.getUpdatedAt());
                    vo.setTargetProgress(milestone.getTargetProgress());
                    vo.setIsPaired(milestone.getIsPaired());
                    
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get milestone by ID
     * 
     * @param milestoneId milestone ID
     * @return milestone VO
     * @throws ResourceNotFoundException if milestone not found
     */
    public MilestoneVO getMilestoneById(Long milestoneId) {
        Milestone milestone = findMilestoneById(milestoneId);
        return toMilestoneVO(milestone);
    }

    /**
     * Get milestones by indicator ID
     * Requirements: 5.3 - Query milestones by indicator
     * 
     * @param indicatorId indicator ID
     * @return list of milestones for the indicator
     */
    public List<MilestoneVO> getMilestonesByIndicatorId(Long indicatorId) {
        return milestoneRepository.findByIndicator_IndicatorIdOrderBySortOrderAsc(indicatorId).stream()
                .map(this::toMilestoneVO)
                .collect(Collectors.toList());
    }

    /**
     * Get milestones by indicator ID ordered by due date
     * 
     * @param indicatorId indicator ID
     * @return list of milestones ordered by due date
     */
    public List<MilestoneVO> getMilestonesByIndicatorIdOrderByDueDate(Long indicatorId) {
        return milestoneRepository.findByIndicator_IndicatorIdOrderByDueDateAsc(indicatorId).stream()
                .map(this::toMilestoneVO)
                .collect(Collectors.toList());
    }

    /**
     * Get milestones by status
     * 
     * @param status milestone status
     * @return list of milestones with the status
     */
    public List<MilestoneVO> getMilestonesByStatus(MilestoneStatus status) {
        return milestoneRepository.findByStatus(status).stream()
                .map(this::toMilestoneVO)
                .collect(Collectors.toList());
    }

    /**
     * Get overdue milestones
     * 
     * @return list of overdue milestones
     */
    public List<MilestoneVO> getOverdueMilestones() {
        return milestoneRepository.findOverdueMilestones(LocalDate.now()).stream()
                .map(this::toMilestoneVO)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming milestones within specified days
     * 
     * @param days number of days to look ahead
     * @return list of upcoming milestones
     */
    public List<MilestoneVO> getUpcomingMilestones(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        return milestoneRepository.findUpcomingMilestones(startDate, endDate).stream()
                .map(this::toMilestoneVO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new milestone
     * Requirements: 5.1 - Add milestone to indicator
     * Requirements: 5.2 - Calculate total weight and display warning if not 100%
     * 
     * @param request milestone creation request
     * @return created milestone VO with weight validation result
     */
    @Transactional
    public MilestoneVO createMilestone(MilestoneCreateRequest request) {
        // Validate indicator exists
        Indicator indicator = indicatorService.findIndicatorById(request.getIndicatorId());

        // Check for duplicate milestone name
        if (milestoneRepository.existsByIndicator_IndicatorIdAndMilestoneName(
                request.getIndicatorId(), request.getMilestoneName())) {
            throw new BusinessException("Milestone with this name already exists for the indicator");
        }

        // Validate inherited from milestone if provided
        Milestone inheritedFrom = null;
        if (request.getInheritedFromId() != null) {
            inheritedFrom = findMilestoneById(request.getInheritedFromId());
        }

        Milestone milestone = new Milestone();
        milestone.setIndicator(indicator);
        milestone.setMilestoneName(request.getMilestoneName());
        milestone.setMilestoneDesc(request.getMilestoneDesc());
        milestone.setDueDate(request.getDueDate());
        milestone.setStatus(MilestoneStatus.NOT_STARTED);
        milestone.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        milestone.setInheritedFrom(inheritedFrom);

        Milestone savedMilestone = milestoneRepository.save(milestone);
        
        return toMilestoneVO(savedMilestone);
    }

    /**
     * Update an existing milestone
     * Requirements: 5.2 - Update weight and recalculate total
     * 
     * @param milestoneId milestone ID
     * @param request milestone update request
     * @return updated milestone VO
     */
    @Transactional
    public MilestoneVO updateMilestone(Long milestoneId, MilestoneUpdateRequest request) {
        Milestone milestone = findMilestoneById(milestoneId);

        if (request.getMilestoneName() != null) {
            // Check for duplicate name if changing
            if (!milestone.getMilestoneName().equals(request.getMilestoneName()) &&
                    milestoneRepository.existsByIndicator_IndicatorIdAndMilestoneName(
                            milestone.getIndicator().getIndicatorId(), request.getMilestoneName())) {
                throw new BusinessException("Milestone with this name already exists for the indicator");
            }
            milestone.setMilestoneName(request.getMilestoneName());
        }
        if (request.getMilestoneDesc() != null) {
            milestone.setMilestoneDesc(request.getMilestoneDesc());
        }
        if (request.getDueDate() != null) {
            milestone.setDueDate(request.getDueDate());
        }
        if (request.getStatus() != null) {
            milestone.setStatus(request.getStatus());
        }
        if (request.getSortOrder() != null) {
            milestone.setSortOrder(request.getSortOrder());
        }

        Milestone updatedMilestone = milestoneRepository.save(milestone);
        
        // Check weight sum and log warning if not 100%
        validateWeightSum(milestone.getIndicator().getIndicatorId());

        return toMilestoneVO(updatedMilestone);
    }

    /**
     * Delete a milestone
     * 
     * @param milestoneId milestone ID
     */
    @Transactional
    public void deleteMilestone(Long milestoneId) {
        Milestone milestone = findMilestoneById(milestoneId);
        Long indicatorId = milestone.getIndicator().getIndicatorId();
        
        milestoneRepository.delete(milestone);
        
        // Check weight sum after deletion
        validateWeightSum(indicatorId);
    }

    /**
     * Update milestone status
     * 
     * @param milestoneId milestone ID
     * @param status new status
     * @return updated milestone VO
     */
    @Transactional
    public MilestoneVO updateMilestoneStatus(Long milestoneId, MilestoneStatus status) {
        Milestone milestone = findMilestoneById(milestoneId);
        milestone.setStatus(status);
        Milestone updatedMilestone = milestoneRepository.save(milestone);
        return toMilestoneVO(updatedMilestone);
    }

    /**
     * Calculate total weight percentage for an indicator
     * Requirements: 5.1 - Calculate total weight of all milestones
     * 
     * @param indicatorId indicator ID
     * @return total weight percentage
     */
    public BigDecimal calculateTotalWeight(Long indicatorId) {
        // TODO: weight_percent字段已从数据库移除，暂时返回0
        return BigDecimal.ZERO;
    }

    /**
     * Validate weight sum and return validation result
     * Requirements: 5.2 - Display warning if weight sum is not 100%
     * Requirements: 5.4 - Block submission if weights incomplete (warning only, not blocking save)
     * 
     * @param indicatorId indicator ID
     * @return weight validation result
     */
    public WeightValidationResult validateWeightSum(Long indicatorId) {
        // TODO: weight_percent字段已从数据库移除，暂时跳过验证
        return new WeightValidationResult(true, BigDecimal.ZERO, WEIGHT_SUM_TARGET);
    }

    /**
     * Check if indicator has complete milestone weights
     * Requirements: 5.4 - Check if weights are complete for submission
     * 
     * @param indicatorId indicator ID
     * @return true if weights sum to 100%
     */
    public boolean hasCompleteWeights(Long indicatorId) {
        // TODO: weight_percent字段已从数据库移除，暂时返回true
        return true;
    }

    /**
     * Find milestone entity by ID
     * 
     * @param milestoneId milestone ID
     * @return milestone entity
     * @throws ResourceNotFoundException if milestone not found
     */
    public Milestone findMilestoneById(Long milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));
    }

    /**
     * Convert Milestone entity to MilestoneVO
     */
    private MilestoneVO toMilestoneVO(Milestone milestone) {
        MilestoneVO vo = new MilestoneVO();
        vo.setMilestoneId(milestone.getMilestoneId());
        vo.setIndicatorId(milestone.getIndicator().getIndicatorId());
        vo.setIndicatorDesc(milestone.getIndicator().getIndicatorDesc());
        vo.setMilestoneName(milestone.getMilestoneName());
        vo.setMilestoneDesc(milestone.getMilestoneDesc());
        vo.setDueDate(milestone.getDueDate());
        vo.setStatus(milestone.getStatus());
        vo.setSortOrder(milestone.getSortOrder());
        if (milestone.getInheritedFrom() != null) {
            vo.setInheritedFromId(milestone.getInheritedFrom().getMilestoneId());
        }
        vo.setCreatedAt(milestone.getCreatedAt());
        vo.setUpdatedAt(milestone.getUpdatedAt());
        
        // 新增字段映射 (前端数据对齐)
        vo.setTargetProgress(milestone.getTargetProgress());
        vo.setIsPaired(milestone.getIsPaired());
        
        return vo;
    }

    /**
     * Weight validation result
     */
    public record WeightValidationResult(
            boolean isValid,
            BigDecimal actualSum,
            BigDecimal expectedSum
    ) {
        public String getMessage() {
            if (isValid) {
                return "Milestone weights are complete (100%)";
            }
            return String.format("Milestone weight sum is %.2f%% (expected %.2f%%)", 
                    actualSum, expectedSum);
        }
    }

    // ==================== Pairing Mechanism (配对机制) ====================

    /**
     * Get the next milestone that needs to be reported for an indicator
     * This implements the "catch-up" rule: returns the earliest unpaired milestone
     * 
     * @param indicatorId indicator ID
     * @return the next milestone to report, or null if all milestones are paired
     */
    public MilestoneVO getNextMilestoneToReport(Long indicatorId) {
        return milestoneRepository.findFirstUnpairedMilestone(indicatorId)
                .map(this::toMilestoneVO)
                .orElse(null);
    }

    /**
     * Get all unpaired milestones for an indicator, ordered by due date
     * 
     * @param indicatorId indicator ID
     * @return list of unpaired milestones
     */
    public List<MilestoneVO> getUnpairedMilestones(Long indicatorId) {
        return milestoneRepository.findUnpairedMilestonesByIndicator(indicatorId).stream()
                .map(this::toMilestoneVO)
                .collect(Collectors.toList());
    }

    /**
     * Check if a specific milestone is paired (has an approved report)
     * 
     * @param milestoneId milestone ID
     * @return true if the milestone has an approved report
     */
    public boolean isMilestonePaired(Long milestoneId) {
        return milestoneRepository.isMilestonePaired(milestoneId);
    }

    /**
     * Check if a milestone can be reported on (must be the earliest unpaired milestone)
     * This enforces the catch-up rule
     * 
     * @param indicatorId indicator ID
     * @param milestoneId milestone ID to check
     * @return true if this milestone is the next one to report
     */
    public boolean canReportOnMilestone(Long indicatorId, Long milestoneId) {
        return milestoneRepository.findFirstUnpairedMilestone(indicatorId)
                .map(m -> m.getMilestoneId().equals(milestoneId))
                .orElse(false);
    }

    /**
     * Get pairing status summary for an indicator
     * 
     * @param indicatorId indicator ID
     * @return pairing status summary
     */
    public PairingStatusSummary getPairingStatus(Long indicatorId) {
        List<Milestone> allMilestones = milestoneRepository.findByIndicator_IndicatorIdOrderByDueDateAsc(indicatorId);
        List<Milestone> unpairedMilestones = milestoneRepository.findUnpairedMilestonesByIndicator(indicatorId);
        
        int total = allMilestones.size();
        int paired = total - unpairedMilestones.size();
        
        MilestoneVO nextToReport = unpairedMilestones.isEmpty() ? null : toMilestoneVO(unpairedMilestones.get(0));
        
        return new PairingStatusSummary(total, paired, unpairedMilestones.size(), nextToReport);
    }

    /**
     * Pairing status summary record
     */
    public record PairingStatusSummary(
            int totalMilestones,
            int pairedCount,
            int unpairedCount,
            MilestoneVO nextMilestoneToReport
    ) {
        public boolean isAllPaired() {
            return unpairedCount == 0;
        }

        public double getPairingProgress() {
            if (totalMilestones == 0) return 100.0;
            return (pairedCount * 100.0) / totalMilestones;
        }
    }
}
