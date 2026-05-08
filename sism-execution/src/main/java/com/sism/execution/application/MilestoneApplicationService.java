package com.sism.execution.application;

import com.sism.exception.ResourceNotFoundException;
import com.sism.execution.domain.milestone.ExecutionMilestoneRepository;
import com.sism.execution.domain.milestone.Milestone;
import com.sism.execution.domain.milestone.MilestoneStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MilestoneApplicationService - 里程碑应用服务
 * 处理里程碑的业务逻辑
 */
@Service("executionMilestoneApplicationService")
@RequiredArgsConstructor
public class MilestoneApplicationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ExecutionMilestoneRepository milestoneRepository;

    /**
     * 创建里程碑
     */
    @Transactional
    public Milestone createMilestone(Long indicatorId, String milestoneName,
                                     String description, LocalDateTime dueDate,
                                     Integer targetProgress, MilestoneStatus status,
                                     Integer sortOrder, Boolean isPaired,
                                     Long inheritedFrom) {
        Milestone milestone = new Milestone();
        milestone.setIndicatorId(indicatorId);
        milestone.setMilestoneName(milestoneName);
        milestone.setDescription(description);
        milestone.setTargetDate(dueDate);
        milestone.setProgress(targetProgress);
        milestone.setStatus(status);
        milestone.setSortOrder(sortOrder);
        milestone.setIsPaired(isPaired != null ? isPaired : false);
        milestone.setInheritedFrom(inheritedFrom);
        milestone.setCreatedAt(LocalDateTime.now());
        milestone.setUpdatedAt(LocalDateTime.now());

        return milestoneRepository.save(milestone);
    }

    /**
     * 更新里程碑
     */
    @Transactional
    public Milestone updateMilestone(Long milestoneId, Long indicatorId, String milestoneName,
                                     String description, LocalDateTime dueDate,
                                     Integer targetProgress, MilestoneStatus status,
                                     Integer sortOrder, Boolean isPaired,
                                     Long inheritedFrom) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found with id: " + milestoneId));

        if (indicatorId != null) {
            milestone.setIndicatorId(indicatorId);
        }
        if (milestoneName != null) {
            milestone.setMilestoneName(milestoneName);
        }
        milestone.setDescription(description);
        milestone.setTargetDate(dueDate);
        milestone.setProgress(targetProgress);
        if (status != null) {
            milestone.setStatus(status);
        }
        milestone.setSortOrder(sortOrder);
        milestone.setIsPaired(isPaired);
        milestone.setInheritedFrom(inheritedFrom);
        milestone.setUpdatedAt(LocalDateTime.now());

        return milestoneRepository.save(milestone);
    }

    /**
     * 删除里程碑
     */
    @Transactional
    public void deleteMilestone(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));

        milestoneRepository.delete(milestone);
    }

    /**
     * 根据ID查询里程碑
     */
    public Optional<Milestone> findMilestoneById(Long milestoneId) {
        return milestoneRepository.findById(milestoneId);
    }

    /**
     * 查询所有里程碑
     */
    public List<Milestone> findAllMilestones() {
        return milestoneRepository.findAll();
    }

    /**
     * 分页查询所有里程碑
     */
    public Page<Milestone> findAllMilestones(int page, int size) {
        Pageable pageable = createPageable(page, size);
        return milestoneRepository.findAll(pageable);
    }

    /**
     * 根据指标ID查询里程碑
     */
    public List<Milestone> findMilestonesByIndicatorId(Long indicatorId) {
        return milestoneRepository.findByIndicatorId(indicatorId);
    }

    /**
     * 根据状态查询里程碑
     */
    public List<Milestone> findMilestonesByStatus(MilestoneStatus status) {
        return milestoneRepository.findByStatus(status);
    }

    /**
     * 根据状态分页查询里程碑
     */
    public Page<Milestone> findMilestonesByStatus(MilestoneStatus status, int page, int size) {
        Pageable pageable = createPageable(page, size);
        return milestoneRepository.findByStatus(status, pageable);
    }

    /**
     * 检查里程碑是否存在
     */
    public boolean existsById(Long id) {
        return milestoneRepository.existsById(id);
    }

    /**
     * 查询里程碑配对状态
     */
    public Optional<Map<String, Object>> getMilestonePairingStatus(Long milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .map(milestone -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("milestoneId", milestone.getId());
                    status.put("isPaired", milestone.getIsPaired() != null ? milestone.getIsPaired() : false);
                    status.put("pairedIndicatorId", milestone.getIndicatorId());
                    status.put("pairedAt", milestone.getCreatedAt());
                    return status;
                });
    }

    /**
     * 查询指标里程碑配对状态
     */
    public Map<String, Object> getIndicatorMilestonePairingStatus(Long indicatorId) {
        List<Milestone> milestones = milestoneRepository.findByIndicatorId(indicatorId);
        Map<String, Object> status = new HashMap<>();
        status.put("indicatorId", indicatorId);
        status.put("pairedMilestoneCount", milestones.stream().filter(m -> m.getIsPaired() != null && m.getIsPaired()).count());

        List<Map<String, Object>> milestoneList = milestones.stream()
                .map(milestone -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", milestone.getId());
                    m.put("name", milestone.getMilestoneName());
                    m.put("isPaired", milestone.getIsPaired() != null ? milestone.getIsPaired() : false);
                    return m;
                })
                .collect(Collectors.toList());

        status.put("milestones", milestoneList);
        return status;
    }

    /**
     * 检查指标里程碑是否可填报
     */
    public Map<String, Object> checkIndicatorMilestoneCanReport(Long indicatorId, Long milestoneId) {
        Map<String, Object> result = new HashMap<>();
        result.put("indicatorId", indicatorId);
        result.put("milestoneId", milestoneId);

        Optional<Milestone> milestoneOptional = milestoneRepository.findById(milestoneId);
        if (milestoneOptional.isPresent()) {
            Milestone milestone = milestoneOptional.get();
            boolean canReport = milestone.getIsPaired() != null && milestone.getIsPaired()
                    && milestone.getIndicatorId() != null && milestone.getIndicatorId().equals(indicatorId);
            result.put("canReport", canReport);
            result.put("reason", canReport ? "可以填报" : "里程碑未配对或指标不匹配");
        } else {
            result.put("canReport", false);
            result.put("reason", "里程碑不存在");
        }

        return result;
    }

    private Pageable createPageable(int page, int size) {
        return PageRequest.of(normalizePageNumber(page), normalizePageSize(size), DEFAULT_SORT);
    }

    private int normalizePageNumber(int page) {
        return Math.max(page, 1) - 1;
    }

    private int normalizePageSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

}
