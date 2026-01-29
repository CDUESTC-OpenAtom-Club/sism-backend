package com.sism.service;

import com.sism.dto.IndicatorAuditData;
import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.entity.AppUser;
import com.sism.entity.Indicator;
import com.sism.entity.Milestone;
import com.sism.entity.Org;
import com.sism.entity.StrategicTask;
import com.sism.enums.AuditEntityType;
import com.sism.enums.IndicatorStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.OrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.repository.UserRepository;
import com.sism.vo.IndicatorVO;
import com.sism.vo.MilestoneVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for indicator management
 * Provides CRUD operations with soft deletion, organization filtering, and audit logging
 * 
 * Requirements: 2.2, 2.3, 2.4, 2.5, 7.1, 7.2, 7.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndicatorService {

    private final IndicatorRepository indicatorRepository;
    private final TaskRepository taskRepository;
    private final OrgRepository orgRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * Get indicator by ID
     * 
     * @param indicatorId indicator ID
     * @return indicator VO
     * @throws ResourceNotFoundException if indicator not found
     */
    public IndicatorVO getIndicatorById(Long indicatorId) {
        Indicator indicator = findIndicatorById(indicatorId);
        return toIndicatorVO(indicator, true);
    }

    /**
     * Get all active indicators
     * 
     * @return list of active indicators
     */
    public List<IndicatorVO> getAllActiveIndicators() {
        return indicatorRepository.findByStatus(IndicatorStatus.ACTIVE).stream()
                .map(i -> toIndicatorVO(i, false))
                .collect(Collectors.toList());
    }

    /**
     * Get indicators by task ID
     * Requirements: 2.2 - Load all indicators under a task
     * 
     * @param taskId task ID
     * @return list of indicators for the task
     */
    public List<IndicatorVO> getIndicatorsByTaskId(Long taskId) {
        return indicatorRepository.findByTask_TaskIdAndStatus(taskId, IndicatorStatus.ACTIVE).stream()
                .map(i -> toIndicatorVO(i, true))
                .collect(Collectors.toList());
    }

    /**
     * Get root indicators by task ID (no parent)
     * 
     * @param taskId task ID
     * @return list of root indicators
     */
    public List<IndicatorVO> getRootIndicatorsByTaskId(Long taskId) {
        return indicatorRepository.findByTask_TaskIdAndParentIndicatorIsNull(taskId).stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .map(i -> toIndicatorVO(i, true))
                .collect(Collectors.toList());
    }

    /**
     * Get indicators by owner organization ID
     * 
     * @param ownerOrgId owner organization ID
     * @return list of indicators owned by the organization
     */
    public List<IndicatorVO> getIndicatorsByOwnerOrgId(Long ownerOrgId) {
        return indicatorRepository.findByOwnerOrgAndStatus(ownerOrgId, IndicatorStatus.ACTIVE).stream()
                .map(i -> toIndicatorVO(i, false))
                .collect(Collectors.toList());
    }

    /**
     * Get indicators by target organization ID
     * Requirements: 2.2 - Filter indicators by organization
     * 
     * @param targetOrgId target organization ID
     * @return list of indicators targeting the organization
     */
    public List<IndicatorVO> getIndicatorsByTargetOrgId(Long targetOrgId) {
        return indicatorRepository.findByTargetOrg_OrgId(targetOrgId).stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .map(i -> toIndicatorVO(i, false))
                .collect(Collectors.toList());
    }

    /**
     * Get indicators by target organization hierarchy
     * Returns indicators where target org matches or is a descendant
     * 
     * @param orgId organization ID
     * @return list of indicators in the hierarchy
     */
    public List<IndicatorVO> getIndicatorsByTargetOrgHierarchy(Long orgId) {
        return indicatorRepository.findByTargetOrgHierarchy(orgId).stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .map(i -> toIndicatorVO(i, false))
                .collect(Collectors.toList());
    }

    /**
     * Create a new indicator
     * Requirements: 2.3 - Create new indicator
     * Requirements: 7.1 - Record CREATE audit log
     * 
     * @param request indicator creation request
     * @return created indicator VO
     */
    @Transactional
    public IndicatorVO createIndicator(IndicatorCreateRequest request) {
        return createIndicator(request, null, null);
    }

    /**
     * Create a new indicator with audit logging
     * Requirements: 2.3 - Create new indicator
     * Requirements: 7.1 - Record CREATE audit log
     * 
     * @param request indicator creation request
     * @param actorUserId user performing the action (optional)
     * @param reason reason for the action (optional)
     * @return created indicator VO
     */
    @Transactional
    public IndicatorVO createIndicator(IndicatorCreateRequest request, Long actorUserId, String reason) {
        log.info("Creating indicator: {} for task: {}", request.getIndicatorDesc(), request.getTaskId());
        
        // Validate task exists
        StrategicTask task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Strategic Task", request.getTaskId()));

        // Validate owner organization exists
        Org ownerOrg = orgRepository.findById(request.getOwnerOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner Organization", request.getOwnerOrgId()));

        // Validate target organization exists
        Org targetOrg = orgRepository.findById(request.getTargetOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Target Organization", request.getTargetOrgId()));

        // Validate parent indicator if provided
        Indicator parentIndicator = null;
        if (request.getParentIndicatorId() != null) {
            parentIndicator = findIndicatorById(request.getParentIndicatorId());
            validateParentIndicatorLevel(parentIndicator, request.getLevel());
        }

        Indicator indicator = new Indicator();
        indicator.setTask(task);
        indicator.setParentIndicator(parentIndicator);
        indicator.setLevel(request.getLevel());
        indicator.setOwnerOrg(ownerOrg);
        indicator.setTargetOrg(targetOrg);
        indicator.setIndicatorDesc(request.getIndicatorDesc());
        indicator.setWeightPercent(request.getWeightPercent());
        indicator.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        indicator.setYear(request.getYear());
        indicator.setStatus(IndicatorStatus.ACTIVE);
        indicator.setRemark(request.getRemark());
        
        // Set canWithdraw if provided, otherwise use default (false)
        if (request.getCanWithdraw() != null) {
            indicator.setCanWithdraw(request.getCanWithdraw());
        }

        Indicator savedIndicator = indicatorRepository.save(indicator);
        indicatorRepository.flush(); // Force immediate database write
        
        log.info("Successfully created indicator with ID: {}", savedIndicator.getIndicatorId());

        // Record audit log for CREATE operation
        AppUser actorUser = actorUserId != null ? 
                userRepository.findById(actorUserId).orElse(null) : null;
        Org actorOrg = actorUser != null ? actorUser.getOrg() : ownerOrg;
        
        try {
            auditLogService.logCreate(
                    AuditEntityType.INDICATOR,
                    savedIndicator.getIndicatorId(),
                    IndicatorAuditData.fromEntity(savedIndicator),
                    actorUser,
                    actorOrg,
                    reason
            );
        } catch (Exception e) {
            log.warn("Failed to create audit log for indicator creation: {}", e.getMessage());
        }

        return toIndicatorVO(savedIndicator, false);
    }

    /**
     * Update an existing indicator
     * Requirements: 2.4 - Modify indicator information
     * 
     * @param indicatorId indicator ID
     * @param request indicator update request
     * @return updated indicator VO
     */
    @Transactional
    public IndicatorVO updateIndicator(Long indicatorId, IndicatorUpdateRequest request) {
        return updateIndicator(indicatorId, request, null, null);
    }

    /**
     * Update an existing indicator with audit logging
     * Requirements: 2.4 - Modify indicator information
     * Requirements: 7.2 - Record UPDATE audit log with data differences
     * 
     * @param indicatorId indicator ID
     * @param request indicator update request
     * @param actorUserId user performing the action (optional)
     * @param reason reason for the action (optional)
     * @return updated indicator VO
     */
    @Transactional
    public IndicatorVO updateIndicator(Long indicatorId, IndicatorUpdateRequest request, 
                                        Long actorUserId, String reason) {
        Indicator indicator = findIndicatorById(indicatorId);

        // Check if indicator is active
        if (indicator.getStatus() == IndicatorStatus.ARCHIVED) {
            throw new BusinessException("Cannot update archived indicator");
        }

        // Capture before state for audit log
        IndicatorAuditData beforeData = IndicatorAuditData.fromEntity(indicator);

        if (request.getParentIndicatorId() != null) {
            if (request.getParentIndicatorId().equals(indicatorId)) {
                throw new BusinessException("Indicator cannot be its own parent");
            }
            Indicator parentIndicator = findIndicatorById(request.getParentIndicatorId());
            validateParentIndicatorLevel(parentIndicator, 
                    request.getLevel() != null ? request.getLevel() : indicator.getLevel());
            indicator.setParentIndicator(parentIndicator);
        }

        if (request.getLevel() != null) {
            indicator.setLevel(request.getLevel());
        }
        if (request.getOwnerOrgId() != null) {
            Org ownerOrg = orgRepository.findById(request.getOwnerOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner Organization", request.getOwnerOrgId()));
            indicator.setOwnerOrg(ownerOrg);
        }
        if (request.getTargetOrgId() != null) {
            Org targetOrg = orgRepository.findById(request.getTargetOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target Organization", request.getTargetOrgId()));
            indicator.setTargetOrg(targetOrg);
        }
        if (request.getIndicatorDesc() != null) {
            indicator.setIndicatorDesc(request.getIndicatorDesc());
        }
        if (request.getWeightPercent() != null) {
            indicator.setWeightPercent(request.getWeightPercent());
        }
        if (request.getSortOrder() != null) {
            indicator.setSortOrder(request.getSortOrder());
        }
        if (request.getYear() != null) {
            indicator.setYear(request.getYear());
        }
        if (request.getRemark() != null) {
            indicator.setRemark(request.getRemark());
        }
        if (request.getCanWithdraw() != null) {
            indicator.setCanWithdraw(request.getCanWithdraw());
        }
        if (request.getStatus() != null) {
            indicator.setStatus(request.getStatus());
        }
        if (request.getProgress() != null) {
            indicator.setProgress(request.getProgress());
        }
        if (request.getProgressApprovalStatus() != null) {
            indicator.setProgressApprovalStatus(request.getProgressApprovalStatus());
        }
        if (request.getPendingProgress() != null) {
            indicator.setPendingProgress(request.getPendingProgress());
        }
        if (request.getPendingRemark() != null) {
            indicator.setPendingRemark(request.getPendingRemark());
        }
        if (request.getPendingAttachments() != null) {
            indicator.setPendingAttachments(request.getPendingAttachments());
        }
        if (request.getTargetValue() != null) {
            indicator.setTargetValue(request.getTargetValue());
        }
        if (request.getActualValue() != null) {
            indicator.setActualValue(request.getActualValue());
        }
        if (request.getUnit() != null) {
            indicator.setUnit(request.getUnit());
        }
        if (request.getResponsiblePerson() != null) {
            indicator.setResponsiblePerson(request.getResponsiblePerson());
        }

        Indicator updatedIndicator = indicatorRepository.save(indicator);

        // Record audit log for UPDATE operation
        AppUser actorUser = actorUserId != null ? 
                userRepository.findById(actorUserId).orElse(null) : null;
        Org actorOrg = actorUser != null ? actorUser.getOrg() : indicator.getOwnerOrg();
        IndicatorAuditData afterData = IndicatorAuditData.fromEntity(updatedIndicator);
        
        try {
            auditLogService.logUpdate(
                    AuditEntityType.INDICATOR,
                    indicatorId,
                    beforeData,
                    afterData,
                    actorUser,
                    actorOrg,
                    reason
            );
        } catch (Exception e) {
            log.warn("Failed to create audit log for indicator update: {}", e.getMessage());
        }

        return toIndicatorVO(updatedIndicator, false);
    }

    /**
     * Soft delete an indicator (archive)
     * Requirements: 2.5 - Soft deletion by updating status to ARCHIVED
     * 
     * @param indicatorId indicator ID
     */
    @Transactional
    public void deleteIndicator(Long indicatorId) {
        deleteIndicator(indicatorId, null, null);
    }

    /**
     * Soft delete an indicator (archive) with audit logging
     * Requirements: 2.5 - Soft deletion by updating status to ARCHIVED
     * Requirements: 7.3 - Record DELETE audit log with complete snapshot
     * 
     * @param indicatorId indicator ID
     * @param actorUserId user performing the action (optional)
     * @param reason reason for the action (optional)
     */
    @Transactional
    public void deleteIndicator(Long indicatorId, Long actorUserId, String reason) {
        Indicator indicator = findIndicatorById(indicatorId);
        
        // Check if already archived
        if (indicator.getStatus() == IndicatorStatus.ARCHIVED) {
            throw new BusinessException("Indicator is already archived");
        }

        // Capture before state for audit log
        IndicatorAuditData beforeData = IndicatorAuditData.fromEntity(indicator);

        // Soft delete by setting status to ARCHIVED
        indicator.setStatus(IndicatorStatus.ARCHIVED);
        indicatorRepository.save(indicator);

        // Record audit log for DELETE (ARCHIVE) operation
        AppUser actorUser = actorUserId != null ? 
                userRepository.findById(actorUserId).orElse(null) : null;
        Org actorOrg = actorUser != null ? actorUser.getOrg() : indicator.getOwnerOrg();
        
        try {
            auditLogService.logArchive(
                    AuditEntityType.INDICATOR,
                    indicatorId,
                    beforeData,
                    actorUser,
                    actorOrg,
                    reason != null ? reason : "Soft delete indicator"
            );
        } catch (Exception e) {
            log.warn("Failed to create audit log for indicator deletion: {}", e.getMessage());
        }
    }

    /**
     * Search indicators by description keyword
     * 
     * @param keyword search keyword
     * @return list of matching indicators
     */
    public List<IndicatorVO> searchIndicators(String keyword) {
        return indicatorRepository.searchByDescriptionKeyword(keyword).stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .map(i -> toIndicatorVO(i, false))
                .collect(Collectors.toList());
    }

    /**
     * Find indicator entity by ID
     * 
     * @param indicatorId indicator ID
     * @return indicator entity
     * @throws ResourceNotFoundException if indicator not found
     */
    public Indicator findIndicatorById(Long indicatorId) {
        return indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Indicator", indicatorId));
    }

    /**
     * Validate parent indicator level hierarchy
     * Parent must have a higher level than child
     */
    private void validateParentIndicatorLevel(Indicator parent, com.sism.enums.IndicatorLevel childLevel) {
        // STRAT_TO_FUNC should be parent of FUNC_TO_COLLEGE
        if (parent.getLevel() == childLevel) {
            throw new BusinessException("Parent indicator must have a different level than child");
        }
    }

    /**
     * Convert Indicator entity to IndicatorVO
     * 
     * Updated: 2026-01-19 - Added mapping for new fields (frontend data alignment)
     * Requirements: data-alignment-sop 5.3
     * 
     * @param indicator indicator entity
     * @param includeChildren whether to include child indicators and milestones
     * @return indicator VO
     */
    private IndicatorVO toIndicatorVO(Indicator indicator, boolean includeChildren) {
        IndicatorVO vo = new IndicatorVO();
        vo.setIndicatorId(indicator.getIndicatorId());
        vo.setTaskId(indicator.getTask().getTaskId());
        vo.setTaskName(indicator.getTask().getTaskName());
        
        if (indicator.getParentIndicator() != null) {
            vo.setParentIndicatorId(indicator.getParentIndicator().getIndicatorId());
            vo.setParentIndicatorDesc(indicator.getParentIndicator().getIndicatorDesc());
        }
        
        vo.setLevel(indicator.getLevel());
        vo.setOwnerOrgId(indicator.getOwnerOrg().getOrgId());
        vo.setOwnerOrgName(indicator.getOwnerOrg().getOrgName());
        vo.setTargetOrgId(indicator.getTargetOrg().getOrgId());
        vo.setTargetOrgName(indicator.getTargetOrg().getOrgName());
        vo.setIndicatorDesc(indicator.getIndicatorDesc());
        vo.setWeightPercent(indicator.getWeightPercent());
        vo.setSortOrder(indicator.getSortOrder());
        vo.setYear(indicator.getYear());
        vo.setStatus(indicator.getStatus());
        vo.setRemark(indicator.getRemark());
        vo.setCreatedAt(indicator.getCreatedAt());
        vo.setUpdatedAt(indicator.getUpdatedAt());

        // ==================== 新增字段映射 (前端数据对齐) ====================
        
        // 指标类型字段
        vo.setIsQualitative(indicator.getIsQualitative());
        vo.setType1(indicator.getType1());
        vo.setType2(indicator.getType2());
        
        // 目标值和单位
        vo.setTargetValue(indicator.getTargetValue());
        vo.setActualValue(indicator.getActualValue());
        vo.setUnit(indicator.getUnit());
        
        // 责任人
        vo.setResponsiblePerson(indicator.getResponsiblePerson());
        
        // 进度
        vo.setProgress(indicator.getProgress());
        
        // 撤回控制
        vo.setCanWithdraw(indicator.getCanWithdraw());
        
        // 审计日志 (JSON)
        vo.setStatusAudit(indicator.getStatusAudit());
        
        // 进度审批相关
        vo.setProgressApprovalStatus(indicator.getProgressApprovalStatus());
        vo.setPendingProgress(indicator.getPendingProgress());
        vo.setPendingRemark(indicator.getPendingRemark());
        vo.setPendingAttachments(indicator.getPendingAttachments());
        
        // 派生字段
        // isStrategic: STRAT_TO_FUNC -> true, FUNC_TO_COLLEGE -> false
        vo.setIsStrategic(indicator.getLevel() == com.sism.enums.IndicatorLevel.STRAT_TO_FUNC);
        
        // responsibleDept: 等同于 targetOrgName
        vo.setResponsibleDept(indicator.getTargetOrg().getOrgName());
        
        // ownerDept: 等同于 ownerOrgName
        vo.setOwnerDept(indicator.getOwnerOrg().getOrgName());

        if (includeChildren) {
            // Include child indicators
            List<IndicatorVO> children = indicator.getChildIndicators().stream()
                    .filter(c -> c.getStatus() == IndicatorStatus.ACTIVE)
                    .map(c -> toIndicatorVO(c, true))
                    .collect(Collectors.toList());
            vo.setChildIndicators(children);

            // Include milestones
            List<MilestoneVO> milestones = indicator.getMilestones().stream()
                    .map(this::toMilestoneVO)
                    .collect(Collectors.toList());
            vo.setMilestones(milestones);
        }

        return vo;
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
        vo.setWeightPercent(milestone.getWeightPercent());
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

    // ==================== Indicator Distribution (指标下发) ====================

    /**
     * Distribute an indicator to a target organization
     * Creates a child indicator linked to the parent, with milestone inheritance for quantitative indicators
     * 
     * @param parentIndicatorId parent indicator ID
     * @param targetOrgId target organization ID
     * @param customDesc optional custom description (for qualitative indicators)
     * @param actorUserId user performing the action
     * @return created child indicator VO
     */
    @Transactional
    public IndicatorVO distributeIndicator(Long parentIndicatorId, Long targetOrgId, 
                                            String customDesc, Long actorUserId) {
        Indicator parentIndicator = findIndicatorById(parentIndicatorId);
        
        // Validate parent indicator is active
        if (parentIndicator.getStatus() != IndicatorStatus.ACTIVE) {
            throw new BusinessException("只能下发状态为 ACTIVE 的指标");
        }

        // Validate target organization exists
        Org targetOrg = orgRepository.findById(targetOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Target Organization", targetOrgId));

        // Determine child level based on parent level
        com.sism.enums.IndicatorLevel childLevel = determineChildLevel(parentIndicator.getLevel());

        // Create child indicator
        Indicator childIndicator = new Indicator();
        childIndicator.setTask(parentIndicator.getTask());
        childIndicator.setParentIndicator(parentIndicator);
        childIndicator.setLevel(childLevel);
        childIndicator.setOwnerOrg(parentIndicator.getTargetOrg()); // Owner is the parent's target
        childIndicator.setTargetOrg(targetOrg);
        
        // Use custom description if provided, otherwise inherit from parent
        childIndicator.setIndicatorDesc(customDesc != null && !customDesc.trim().isEmpty() 
                ? customDesc : parentIndicator.getIndicatorDesc());
        
        childIndicator.setWeightPercent(parentIndicator.getWeightPercent());
        childIndicator.setSortOrder(parentIndicator.getSortOrder());
        childIndicator.setYear(parentIndicator.getYear());
        childIndicator.setStatus(IndicatorStatus.ACTIVE);
        childIndicator.setRemark("下发自指标 #" + parentIndicatorId);

        Indicator savedChild = indicatorRepository.save(childIndicator);

        // Inherit milestones from parent (for quantitative indicators)
        inheritMilestones(parentIndicator, savedChild);

        // Record audit log
        AppUser actorUser = actorUserId != null ? 
                userRepository.findById(actorUserId).orElse(null) : null;
        try {
            auditLogService.logCreate(
                    AuditEntityType.INDICATOR,
                    savedChild.getIndicatorId(),
                    IndicatorAuditData.fromEntity(savedChild),
                    actorUser,
                    parentIndicator.getTargetOrg(),
                    "指标下发：从指标 #" + parentIndicatorId + " 下发到 " + targetOrg.getOrgName()
            );
        } catch (Exception e) {
            log.warn("Failed to create audit log for indicator distribution: {}", e.getMessage());
        }

        log.info("Distributed indicator {} to org {} as new indicator {}", 
                parentIndicatorId, targetOrgId, savedChild.getIndicatorId());

        return toIndicatorVO(savedChild, true);
    }

    /**
     * Batch distribute an indicator to multiple target organizations
     * 
     * @param parentIndicatorId parent indicator ID
     * @param targetOrgIds list of target organization IDs
     * @param actorUserId user performing the action
     * @return list of created child indicator VOs
     */
    @Transactional
    public List<IndicatorVO> batchDistributeIndicator(Long parentIndicatorId, 
                                                       List<Long> targetOrgIds, 
                                                       Long actorUserId) {
        return targetOrgIds.stream()
                .map(targetOrgId -> distributeIndicator(parentIndicatorId, targetOrgId, null, actorUserId))
                .collect(Collectors.toList());
    }

    /**
     * Determine child indicator level based on parent level
     */
    private com.sism.enums.IndicatorLevel determineChildLevel(com.sism.enums.IndicatorLevel parentLevel) {
        return switch (parentLevel) {
            case STRAT_TO_FUNC -> com.sism.enums.IndicatorLevel.FUNC_TO_COLLEGE;
            case FUNC_TO_COLLEGE -> throw new BusinessException("FUNC_TO_COLLEGE 级别的指标不能再下发");
        };
    }

    /**
     * Inherit milestones from parent indicator to child indicator
     * Creates copies of all milestones with inherited_from reference
     * 
     * @param parentIndicator parent indicator
     * @param childIndicator child indicator
     */
    private void inheritMilestones(Indicator parentIndicator, Indicator childIndicator) {
        List<Milestone> parentMilestones = parentIndicator.getMilestones();
        
        if (parentMilestones == null || parentMilestones.isEmpty()) {
            log.info("No milestones to inherit from indicator {}", parentIndicator.getIndicatorId());
            return;
        }

        for (Milestone parentMilestone : parentMilestones) {
            Milestone childMilestone = new Milestone();
            childMilestone.setIndicator(childIndicator);
            childMilestone.setMilestoneName(parentMilestone.getMilestoneName());
            childMilestone.setMilestoneDesc(parentMilestone.getMilestoneDesc());
            childMilestone.setDueDate(parentMilestone.getDueDate());
            childMilestone.setWeightPercent(parentMilestone.getWeightPercent());
            childMilestone.setStatus(com.sism.enums.MilestoneStatus.NOT_STARTED);
            childMilestone.setSortOrder(parentMilestone.getSortOrder());
            childMilestone.setInheritedFrom(parentMilestone);
            
            childIndicator.getMilestones().add(childMilestone);
        }

        log.info("Inherited {} milestones from indicator {} to indicator {}", 
                parentMilestones.size(), parentIndicator.getIndicatorId(), childIndicator.getIndicatorId());
    }

    /**
     * Get all child indicators distributed from a parent indicator
     * 
     * @param parentIndicatorId parent indicator ID
     * @return list of child indicators
     */
    public List<IndicatorVO> getDistributedIndicators(Long parentIndicatorId) {
        return indicatorRepository.findByParentIndicator_IndicatorId(parentIndicatorId).stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .map(i -> toIndicatorVO(i, true))
                .collect(Collectors.toList());
    }

    /**
     * Check if an indicator can be distributed (has valid level and is active)
     * 
     * @param indicatorId indicator ID
     * @return distribution eligibility info
     */
    public DistributionEligibility checkDistributionEligibility(Long indicatorId) {
        Indicator indicator = findIndicatorById(indicatorId);
        
        boolean canDistribute = indicator.getStatus() == IndicatorStatus.ACTIVE 
                && indicator.getLevel() == com.sism.enums.IndicatorLevel.STRAT_TO_FUNC;
        
        String reason = "";
        if (indicator.getStatus() != IndicatorStatus.ACTIVE) {
            reason = "指标状态不是 ACTIVE";
        } else if (indicator.getLevel() != com.sism.enums.IndicatorLevel.STRAT_TO_FUNC) {
            reason = "只有 STRAT_TO_FUNC 级别的指标可以下发";
        }

        int existingDistributions = indicatorRepository.findByParentIndicator_IndicatorId(indicatorId).size();

        return new DistributionEligibility(canDistribute, reason, existingDistributions);
    }

    /**
     * Distribution eligibility result
     */
    public record DistributionEligibility(
            boolean canDistribute,
            String reason,
            int existingDistributionCount
    ) {}

    // ==================== Indicator Filtering (指标过滤) ====================

    /**
     * Get indicators filtered by type1 (定性/定量)
     * Requirements: 7.3, 7.5 - Filter by indicator type
     * 
     * @param type1 indicator type1 value ("定性" or "定量")
     * @return list of matching indicators
     */
    public List<IndicatorVO> getIndicatorsByType1(String type1) {
        return indicatorRepository.findByType1AndStatus(type1, IndicatorStatus.ACTIVE).stream()
                .map(i -> toIndicatorVO(i, false))
                .collect(Collectors.toList());
    }

    /**
     * Get indicators filtered by type2 (发展性/基础性)
     * Requirements: 7.3, 7.5 - Filter by indicator type
     * 
     * @param type2 indicator type2 value ("发展性" or "基础性")
     * @return list of matching indicators
     */
    public List<IndicatorVO> getIndicatorsByType2(String type2) {
        return indicatorRepository.findByType2AndStatus(type2, IndicatorStatus.ACTIVE).stream()
                .map(i -> toIndicatorVO(i, false))
                .collect(Collectors.toList());
    }

    /**
     * Get indicators filtered by isQualitative flag
     * Requirements: 7.3, 7.5 - Filter by qualitative/quantitative
     * 
     * @param isQualitative true for qualitative, false for quantitative
     * @return list of matching indicators
     */
    public List<IndicatorVO> getIndicatorsByQualitative(Boolean isQualitative) {
        return indicatorRepository.findByIsQualitativeAndStatus(isQualitative, IndicatorStatus.ACTIVE).stream()
                .map(i -> toIndicatorVO(i, false))
                .collect(Collectors.toList());
    }

    /**
     * Get indicators filtered by status
     * Requirements: 7.3, 7.5 - Filter by status
     * 
     * @param status indicator status
     * @return list of matching indicators
     */
    public List<IndicatorVO> getIndicatorsByStatus(IndicatorStatus status) {
        return indicatorRepository.findByStatus(status).stream()
                .map(i -> toIndicatorVO(i, false))
                .collect(Collectors.toList());
    }

    /**
     * Get indicators with combined filters
     * Requirements: 7.3, 7.5 - Combined filtering
     * 
     * @param type1 indicator type1 (optional)
     * @param type2 indicator type2 (optional)
     * @param status indicator status (optional, defaults to ACTIVE)
     * @return list of matching indicators
     */
    public List<IndicatorVO> getIndicatorsWithFilters(String type1, String type2, IndicatorStatus status) {
        IndicatorStatus effectiveStatus = status != null ? status : IndicatorStatus.ACTIVE;
        
        if (type1 != null && type2 != null) {
            return indicatorRepository.findByType1AndType2AndStatus(type1, type2, effectiveStatus).stream()
                    .map(i -> toIndicatorVO(i, false))
                    .collect(Collectors.toList());
        } else if (type1 != null) {
            return indicatorRepository.findByType1AndStatus(type1, effectiveStatus).stream()
                    .map(i -> toIndicatorVO(i, false))
                    .collect(Collectors.toList());
        } else if (type2 != null) {
            return indicatorRepository.findByType2AndStatus(type2, effectiveStatus).stream()
                    .map(i -> toIndicatorVO(i, false))
                    .collect(Collectors.toList());
        } else {
            return indicatorRepository.findByStatus(effectiveStatus).stream()
                    .map(i -> toIndicatorVO(i, false))
                    .collect(Collectors.toList());
        }
    }
}
