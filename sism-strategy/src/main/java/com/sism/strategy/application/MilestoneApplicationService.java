package com.sism.strategy.application;

import com.sism.execution.domain.model.milestone.Milestone;
import com.sism.execution.domain.repository.MilestoneRepository;
import com.sism.strategy.interfaces.dto.CreateMilestoneRequest;
import com.sism.strategy.interfaces.dto.MilestoneResponse;
import com.sism.strategy.interfaces.dto.UpdateMilestoneRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MilestoneApplicationService - 里程碑应用服务
 * 处理里程碑的业务逻辑，包括里程碑的创建、更新、查询等操作
 */
@Service("strategyMilestoneApplicationService")
@RequiredArgsConstructor
public class MilestoneApplicationService {

    private final MilestoneRepository milestoneRepository;

    /**
     * 创建里程碑
     */
    @Transactional
    public MilestoneResponse createMilestone(CreateMilestoneRequest request) {
        // 创建基础里程碑数据（使用Milestone实体）
        // 注意：Milestone实体使用indicatorId，但Controller的CreateMilestoneRequest使用planId
        // 这里需要决定是使用indicatorId还是需要扩展Milestone实体支持planId

        Milestone milestone = new Milestone();
        milestone.setMilestoneName(request.getMilestoneName());
        milestone.setTargetDate(request.getTargetDate());
        milestone.setStatus(request.getStatus() != null ? request.getStatus() : "PLANNED");
        milestone.setProgress(request.getPriority() != null && request.getPriority() > 0
                ? 0
                : 0);
        milestone.setCreatedAt(LocalDateTime.now());
        milestone.setUpdatedAt(LocalDateTime.now());

        // 如果提供了indicatorId，设置它
        if (request.getIndicatorId() != null) {
            milestone.setIndicatorId(request.getIndicatorId());
        }

        Milestone saved = milestoneRepository.save(milestone);
        return convertToResponse(saved, request.getPlanId());
    }

    /**
     * 更新里程碑
     */
    @Transactional
    public MilestoneResponse updateMilestone(Long id, UpdateMilestoneRequest request) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + id));

        if (request.getMilestoneName() != null) {
            milestone.setMilestoneName(request.getMilestoneName());
        }

        if (request.getTargetDate() != null) {
            milestone.setTargetDate(request.getTargetDate());
        }

        if (request.getStatus() != null) {
            milestone.setStatus(request.getStatus());
        }

        if (request.getPriority() != null) {
            milestone.setProgress(request.getPriority());
        }

        if (request.getCompletionPercentage() != null) {
            milestone.setProgress(request.getCompletionPercentage());
        }

        milestone.setUpdatedAt(LocalDateTime.now());

        Milestone updated = milestoneRepository.save(milestone);
        return convertToResponse(updated, null);
    }

    /**
     * 删除里程碑
     */
    @Transactional
    public void deleteMilestone(Long id) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + id));

        milestoneRepository.delete(milestone);
    }

    /**
     * 根据ID查询里程碑
     */
    public Optional<MilestoneResponse> getMilestoneById(Long id) {
        return milestoneRepository.findById(id)
                .map(milestone -> convertToResponse(milestone, null));
    }

    /**
     * 查询所有里程碑
     */
    public List<MilestoneResponse> getAllMilestones() {
        return milestoneRepository.findAll().stream()
                .map(milestone -> convertToResponse(milestone, null))
                .collect(Collectors.toList());
    }

    /**
     * 分页查询里程碑
     */
    public Page<MilestoneResponse> getMilestones(int page, int size, Long planId, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Milestone> allMilestones = milestoneRepository.findAll();

        // 应用过滤
        List<Milestone> filteredMilestones = allMilestones.stream()
                .filter(milestone -> {
                    boolean matchPlan = planId == null; // 如果需要planId过滤，需要扩展Milestone实体支持planId
                    boolean matchStatus = status == null || status.equals(milestone.getStatus());
                    return matchPlan && matchStatus;
                })
                .collect(Collectors.toList());

        // 分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredMilestones.size());

        if (start >= filteredMilestones.size()) {
            return new PageImpl<>(List.of(), pageable, filteredMilestones.size());
        }

        List<MilestoneResponse> pageContent = filteredMilestones.subList(start, end).stream()
                .map(milestone -> convertToResponse(milestone, null))
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, filteredMilestones.size());
    }

    /**
     * 根据指标ID查询里程碑
     */
    public List<MilestoneResponse> getMilestonesByIndicatorId(Long indicatorId) {
        return milestoneRepository.findByIndicatorId(indicatorId).stream()
                .map(milestone -> convertToResponse(milestone, null))
                .collect(Collectors.toList());
    }

    /**
     * 检查里程碑是否存在
     */
    public boolean existsById(Long id) {
        return milestoneRepository.existsById(id);
    }

    /**
     * 将Milestone实体转换为响应DTO
     */
    private MilestoneResponse convertToResponse(Milestone milestone, Long planId) {
        return MilestoneResponse.builder()
                .id(milestone.getId())
                .milestoneName(milestone.getMilestoneName())
                .description(null) // Milestone实体当前没有description字段
                .targetDate(milestone.getTargetDate())
                .actualDate(null) // Milestone实体当前没有actualDate字段
                .status(milestone.getStatus())
                .priority(milestone.getProgress()) // 使用progress字段作为优先级（临时方案）
                .completionPercentage(milestone.getProgress())
                .planId(planId)
                .indicatorId(milestone.getIndicatorId())
                .createTime(milestone.getCreatedAt())
                .updateTime(milestone.getUpdatedAt())
                .progress(milestone.getProgress())
                .build();
    }
}
