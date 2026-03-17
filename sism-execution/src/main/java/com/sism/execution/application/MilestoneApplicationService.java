package com.sism.execution.application;

import com.sism.execution.domain.model.milestone.Milestone;
import com.sism.execution.domain.repository.ExecutionMilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MilestoneApplicationService - 里程碑应用服务
 * 处理里程碑的业务逻辑
 */
@Service("executionMilestoneApplicationService")
@RequiredArgsConstructor
public class MilestoneApplicationService {

    private final ExecutionMilestoneRepository milestoneRepository;

    /**
     * 创建里程碑
     */
    @Transactional
    public Milestone createMilestone(Long indicatorId, String milestoneName,
                                     String description, LocalDateTime dueDate,
                                     Integer targetProgress, String status,
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
                                     Integer targetProgress, String status,
                                     Integer sortOrder, Boolean isPaired,
                                     Long inheritedFrom) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

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
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

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
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return convertListToPage(milestoneRepository.findAll(), pageable);
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
    public List<Milestone> findMilestonesByStatus(String status) {
        return milestoneRepository.findByStatus(status);
    }

    /**
     * 根据状态分页查询里程碑
     */
    public Page<Milestone> findMilestonesByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return convertListToPage(milestoneRepository.findByStatus(status), pageable);
    }

    /**
     * 检查里程碑是否存在
     */
    public boolean existsById(Long id) {
        return milestoneRepository.existsById(id);
    }

    /**
     * 将List转换为Page（用于支持分页查询的Repository接口）
     */
    private Page<Milestone> convertListToPage(List<Milestone> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());

        if (start >= list.size()) {
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, list.size());
        }

        return new org.springframework.data.domain.PageImpl<>(
                list.subList(start, end),
                pageable,
                list.size()
        );
    }
}
