package com.sism.strategy.application;

import com.sism.strategy.domain.model.milestone.Milestone;
import com.sism.strategy.domain.repository.MilestoneRepository;
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
        Milestone milestone = new Milestone();
        milestone.setMilestoneName(request.getMilestoneName());
        milestone.setDescription(request.getDescription());
        milestone.setTargetDate(request.getDueDate());
        milestone.setStatus(request.getStatus() != null ? request.getStatus() : "PLANNED");
        milestone.setProgress(request.getTargetProgress() != null ? request.getTargetProgress() : 0);
        milestone.setSortOrder(request.getSortOrder());
        milestone.setIsPaired(request.getIsPaired());
        milestone.setInheritedFrom(request.getInheritedFrom());
        milestone.setCreatedAt(LocalDateTime.now());
        milestone.setUpdatedAt(LocalDateTime.now());

        if (request.getIndicatorId() != null) {
            milestone.setIndicatorId(request.getIndicatorId());
        }

        Milestone saved = milestoneRepository.save(milestone);
        return convertToResponse(saved);
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

        if (request.getDescription() != null) {
            milestone.setDescription(request.getDescription());
        }

        if (request.getDueDate() != null) {
            milestone.setTargetDate(request.getDueDate());
        }

        if (request.getStatus() != null) {
            milestone.setStatus(request.getStatus());
        }

        if (request.getTargetProgress() != null) {
            milestone.setProgress(request.getTargetProgress());
        }

        if (request.getSortOrder() != null) {
            milestone.setSortOrder(request.getSortOrder());
        }

        if (request.getIsPaired() != null) {
            milestone.setIsPaired(request.getIsPaired());
        }

        if (request.getInheritedFrom() != null) {
            milestone.setInheritedFrom(request.getInheritedFrom());
        }

        if (request.getIndicatorId() != null) {
            milestone.setIndicatorId(request.getIndicatorId());
        }

        milestone.setUpdatedAt(LocalDateTime.now());

        Milestone updated = milestoneRepository.save(milestone);
        return convertToResponse(updated);
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
                .map(this::convertToResponse);
    }

    /**
     * 查询所有里程碑
     */
    public List<MilestoneResponse> getAllMilestones() {
        return milestoneRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询里程碑
     */
    public Page<MilestoneResponse> getMilestones(int page, int size, Long indicatorId, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Milestone> allMilestones = milestoneRepository.findAll();

        // 应用过滤
        List<Milestone> filteredMilestones = allMilestones.stream()
                .filter(milestone -> {
                    boolean matchIndicator = indicatorId == null ||
                            (milestone.getIndicatorId() != null && milestone.getIndicatorId().equals(indicatorId));
                    boolean matchStatus = status == null || status.equals(milestone.getStatus());
                    return matchIndicator && matchStatus;
                })
                .collect(Collectors.toList());

        // 分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredMilestones.size());

        if (start >= filteredMilestones.size()) {
            return new PageImpl<>(List.of(), pageable, filteredMilestones.size());
        }

        List<MilestoneResponse> pageContent = filteredMilestones.subList(start, end).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, filteredMilestones.size());
    }

    /**
     * 根据指标ID查询里程碑
     */
    public List<MilestoneResponse> getMilestonesByIndicatorId(Long indicatorId) {
        return milestoneRepository.findByIndicatorId(indicatorId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据多个指标ID批量查询里程碑，按指标ID分组返回
     */
    public java.util.Map<Long, List<MilestoneResponse>> getMilestonesByIndicatorIds(List<Long> indicatorIds) {
        if (indicatorIds == null || indicatorIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<Milestone> milestones = milestoneRepository.findByIndicatorIdIn(indicatorIds);
        return milestones.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getIndicatorId() != null ? m.getIndicatorId() : -1L,
                        Collectors.mapping(this::convertToResponse, Collectors.toList())
                ));
    }

    /**
     * 检查里程碑是否存在
     */
    public boolean existsById(Long id) {
        return milestoneRepository.existsById(id);
    }

    /**
     * 将Milestone实体转换为响应DTO
     * 使用 MilestoneResponse.fromEntity() 静态方法保持一致性
     */
    private MilestoneResponse convertToResponse(Milestone milestone) {
        return MilestoneResponse.fromEntity(milestone);
    }
}
