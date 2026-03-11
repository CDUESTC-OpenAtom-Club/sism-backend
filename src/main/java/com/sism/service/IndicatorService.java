package com.sism.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.dto.MilestoneUpdateRequest;
import com.sism.entity.SysUser;
import com.sism.entity.Indicator;
import com.sism.entity.Milestone;
import com.sism.entity.SysOrg;
import com.sism.entity.StrategicTask;
import com.sism.entity.AuditInstance;
import com.sism.enums.AuditEntityType;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.MilestoneStatus;
import com.sism.enums.ProgressApprovalStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.MilestoneRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.repository.UserRepository;
import com.sism.repository.AuditInstanceRepository;
import com.sism.vo.IndicatorVO;
import com.sism.vo.MilestoneVO;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for indicator management - Simplified to match actual database
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndicatorService {

    private final IndicatorRepository indicatorRepository;
    private final TaskRepository taskRepository;
    private final SysOrgRepository orgRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AuditInstanceRepository auditInstanceRepository;
    private final com.sism.repository.MilestoneRepository milestoneRepository;
    private final AuditInstanceService auditInstanceService;
    private final ObjectMapper objectMapper;
    
    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    /**
     * Get indicator by ID
     */
    public IndicatorVO getIndicatorById(Long indicatorId) {
        Indicator indicator = findIndicatorById(indicatorId);
        return toIndicatorVO(indicator);
    }

    /**
     * Get all indicators (optimized with batch loading)
     */
    public List<IndicatorVO> getAllIndicators() {
        // 使用 JOIN FETCH 查询,避免 N+1 问题
        List<Indicator> indicators = indicatorRepository.findAllWithOrganizations();
        log.info("Found {} total indicators in database (with organizations loaded)", indicators.size());
        
        List<Indicator> activeIndicators = indicators.stream()
                .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                .collect(Collectors.toList());
        log.info("Filtered to {} active indicators (is_deleted = false or NULL)", activeIndicators.size());
        
        // 批量加载关联数据
        return toIndicatorVOsBatch(activeIndicators);
    }

    /**
     * Get all active indicators (alias for getAllIndicators)
     */
    public List<IndicatorVO> getAllActiveIndicators() {
        return getAllIndicators();
    }

    /**
     * Get indicators by year (including all statuses except deleted)
     * Requirements: Year filtering for indicator list
     *
     * Changed from filtering by ACTIVE status to filtering all non-deleted indicators
     * to support showing DRAFT, DISTRIBUTED, and other status indicators to users.
     *
     * @param year target year
     * @return list of indicators for the specified year
     */
    public List<IndicatorVO> getIndicatorsByYear(Integer year) {
        // 使用 findByYear 而不是 findByYearAndStatus，返回所有非删除状态的指标
        List<Indicator> indicators = indicatorRepository.findByYear(year);

        // 过滤掉已删除的指标
        List<Indicator> nonDeletedIndicators = indicators.stream()
                .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                .collect(Collectors.toList());

        log.info("Found {} total indicators for year {}, filtered to {} non-deleted indicators",
                indicators.size(), year, nonDeletedIndicators.size());
        return toIndicatorVOsBatch(nonDeletedIndicators);
    }
    
    /**
     * Get indicators by year with status filtering (for role-based access control)
     * 
     * @param year target year
     * @param filterByStatus whether to filter by DISTRIBUTED status (for non-strategic departments)
     * @return list of indicators for the specified year
     */
    public List<IndicatorVO> getIndicatorsByYearWithFilter(Integer year, boolean filterByStatus) {
        List<Indicator> indicators = indicatorRepository.findByYear(year);

        // 过滤掉已删除的指标
        List<Indicator> nonDeletedIndicators = indicators.stream()
                .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                .collect(Collectors.toList());

        // 如果需要状态过滤（职能部门和学院只能看到已下发的指标）
        if (filterByStatus) {
            nonDeletedIndicators = nonDeletedIndicators.stream()
                    .filter(i -> i.getStatus() == IndicatorStatus.DISTRIBUTED || 
                                 i.getStatus() == IndicatorStatus.ACTIVE)  // ACTIVE 是遗留状态，等同于 DISTRIBUTED
                    .collect(Collectors.toList());
            log.info("Filtered to {} DISTRIBUTED indicators for year {}", nonDeletedIndicators.size(), year);
        }

        return toIndicatorVOsBatch(nonDeletedIndicators);
    }

    /**
     * Get indicators by task ID
     */
    public List<IndicatorVO> getIndicatorsByTaskId(Long taskId) {
        return indicatorRepository.findByTaskId(taskId).stream()
                .map(this::toIndicatorVO)
                .collect(Collectors.toList());
    }

    /**
     * Get root indicators by task ID (no parent)
     */
    public List<IndicatorVO> getRootIndicatorsByTaskId(Long taskId) {
        return indicatorRepository.findByTaskIdAndParentIndicatorIdIsNull(taskId).stream()
                .map(this::toIndicatorVO)
                .collect(Collectors.toList());
    }

    /**
     * Get child indicators by parent ID (including all statuses except deleted)
     * Changed from filtering by ACTIVE status to filtering all non-deleted indicators
     */
    public List<IndicatorVO> getIndicatorsByOwnerOrgId(Long ownerOrgId) {
        return indicatorRepository.findByOwnerOrg_Id(ownerOrgId).stream()
                .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                .map(this::toIndicatorVO)
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
        return indicatorRepository.findByTargetOrg_Id(targetOrgId).stream()
                .filter(this::isDistributed)
                .map(this::toIndicatorVO)
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
                .filter(this::isDistributed)
                .map(this::toIndicatorVO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new indicator
     */
    @Transactional
    public IndicatorVO createIndicator(IndicatorCreateRequest request) {
        log.info("Creating indicator: {} for task: {}", request.getIndicatorDesc(), request.getTaskId());
        
        // Validate task exists
        taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Strategic Task", request.getTaskId()));

        // Validate owner organization exists
        SysOrg ownerOrg = orgRepository.findById(request.getOwnerOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner Organization", request.getOwnerOrgId()));

        // Validate target organization exists
        SysOrg targetOrg = orgRepository.findById(request.getTargetOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Target Organization", request.getTargetOrgId()));

        // Parse level from string to enum
        IndicatorLevel level = IndicatorLevel.PRIMARY; // default
        if (request.getLevel() != null) {
            try {
                level = IndicatorLevel.valueOf(request.getLevel());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid level value: {}, using default PRIMARY", request.getLevel());
            }
        }

        // Build indicator using builder pattern
        Indicator indicator = Indicator.builder()
                .taskId(request.getTaskId())
                .parentIndicatorId(request.getParentIndicatorId())
                .level(level)
                .ownerOrg(ownerOrg)
                .targetOrg(targetOrg)
                .indicatorDesc(request.getIndicatorDesc())
                .weightPercent(request.getWeightPercent())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .remark(request.getRemark())
                .type(request.getType() != null ? request.getType() : "基础性")
                .progress(0)
                .ownerDept(ownerOrg.getName())
                .responsibleDept(targetOrg.getName())
                .year(request.getYear())
                .canWithdraw(request.getCanWithdraw() != null ? request.getCanWithdraw() : true)
                .status(IndicatorStatus.DRAFT)  // Always start with DRAFT lifecycle status
                .distributionStatus("NOT_DISTRIBUTED")  // 默认未下发状态
                .progressApprovalStatus(ProgressApprovalStatus.NONE)  // Always start with NONE approval status
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        // Validate the initial status combination (should always be valid for new indicators)
        validateStatusCombination(indicator.getStatus(), indicator.getProgressApprovalStatus());

        Indicator savedIndicator = indicatorRepository.save(indicator);
        log.info("Successfully created indicator with ID: {}", savedIndicator.getIndicatorId());

        return toIndicatorVO(savedIndicator);
    }

    /**
     * Update an existing indicator
     */
    @Transactional
    public IndicatorVO updateIndicator(Long indicatorId, IndicatorUpdateRequest request) {
        Indicator indicator = findIndicatorById(indicatorId);

        // Check if indicator is archived (only restriction)
        if (indicator.getStatus() == IndicatorStatus.ARCHIVED) {
            throw new BusinessException("Cannot update archived indicator");
        }
        
        // Validate status combinations are valid (allow all combinations except archived)
        validateStatusCombination(indicator.getStatus(), 
                                request.getProgressApprovalStatus() != null ? 
                                ProgressApprovalStatus.valueOf(request.getProgressApprovalStatus()) : 
                                indicator.getProgressApprovalStatus());

        if (request.getParentIndicatorId() != null) {
            if (request.getParentIndicatorId().equals(indicatorId)) {
                throw new BusinessException("Indicator cannot be its own parent");
            }
            indicator.setParentIndicatorId(request.getParentIndicatorId());
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
        if (request.getRemark() != null) {
            indicator.setRemark(request.getRemark());
        }
        if (request.getType() != null) {
            indicator.setType(request.getType());
        }
        if (request.getProgress() != null) {
            indicator.setProgress(request.getProgress());
        }
        if (request.getCanWithdraw() != null) {
            indicator.setCanWithdraw(request.getCanWithdraw());
        }
        
        // Update lifecycle status if provided
        if (request.getStatus() != null) {
            try {
                IndicatorStatus newStatus = IndicatorStatus.valueOf(request.getStatus().toUpperCase());
                indicator.setStatus(newStatus);
                log.info("Updated indicator {} status to: {}", indicatorId, newStatus);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value: {}, keeping current status", request.getStatus());
            }
        }

        // Update status audit if provided
        if (request.getStatusAudit() != null) {
            indicator.setStatusAudit(request.getStatusAudit());
        }
        
        // Check if this update contains a distribution action
        boolean shouldTriggerApproval = false;
        if (request.getStatusAudit() != null) {
            try {
                JsonNode auditArray = objectMapper.readTree(request.getStatusAudit());
                if (auditArray.isArray() && auditArray.size() > 0) {
                    // Check the last audit record for "distribute" action
                    JsonNode lastAudit = auditArray.get(auditArray.size() - 1);
                    if (lastAudit.has("action") && "distribute".equals(lastAudit.get("action").asText())) {
                        shouldTriggerApproval = true;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse statusAudit JSON for indicator {}: {}", indicatorId, e.getMessage());
                // Continue without triggering approval if JSON parsing fails
            }
        }
        
        // Update progress approval status if provided
        if (request.getProgressApprovalStatus() != null) {
            String newStatusStr = request.getProgressApprovalStatus();
            ProgressApprovalStatus newStatus = ProgressApprovalStatus.valueOf(newStatusStr);
            ProgressApprovalStatus oldStatus = indicator.getProgressApprovalStatus();
            
            log.info("Updating progressApprovalStatus for indicator {}: {} -> {}", 
                    indicatorId, oldStatus, newStatus);
            
            // 审批通过逻辑：将 pendingProgress 复制到 progress，并清空待审批字段
            if (newStatus == ProgressApprovalStatus.APPROVED && oldStatus != ProgressApprovalStatus.APPROVED) {
                log.info("Approving indicator {}: copying pendingProgress to progress", indicatorId);
                
                // 将待审批进度复制到实际进度
                if (indicator.getPendingProgress() != null) {
                    indicator.setProgress(indicator.getPendingProgress());
                }
                
                // 清空待审批字段（自动处理，前端也会发送 null）
                indicator.setPendingProgress(null);
                indicator.setPendingRemark(null);
                indicator.setPendingAttachments(null);
            }
            
            indicator.setProgressApprovalStatus(newStatus);
            log.info("Successfully set progressApprovalStatus to: {}", newStatus);
        }
        
        // Update pending fields based on approval status transition, not request value
        // Allow pending field updates unless we're transitioning TO approved status
        boolean isTransitioningToApproved = request.getProgressApprovalStatus() != null && 
                                          "APPROVED".equals(request.getProgressApprovalStatus()) &&
                                          indicator.getProgressApprovalStatus() != ProgressApprovalStatus.APPROVED;
        
        if (!isTransitioningToApproved) {
            // Allow updating pending fields when not transitioning to approved
            if (request.getPendingProgress() != null) {
                indicator.setPendingProgress(request.getPendingProgress());
            }
            if (request.getPendingRemark() != null) {
                indicator.setPendingRemark(request.getPendingRemark());
            }
            if (request.getPendingAttachments() != null) {
                indicator.setPendingAttachments(request.getPendingAttachments());
            }
        }

        // Update milestones if provided
        if (request.getMilestones() != null) {
            updateIndicatorMilestones(indicator, request.getMilestones());
        }

        indicator.setUpdatedAt(LocalDateTime.now());
        Indicator updatedIndicator = indicatorRepository.save(indicator);
        
        // TEMPORARILY DISABLED: Automatic approval instance creation
        // audit_instance table structure mismatch prevents creating approval instances
        /*
        // Automatically create approval instance if distribution action detected
        if (shouldTriggerApproval) {
            try {
                String flowCode = determineApprovalFlowCode(updatedIndicator);
                Long submitterId = getCurrentUserId();
                
                if (submitterId == null) {
                    throw new BusinessException("下发失败：无法获取当前用户信息，请联系管理员");
                }
                
                auditInstanceService.createAuditInstance(
                    flowCode,
                    AuditEntityType.INDICATOR,
                    indicatorId,
                    submitterId
                );
                
                log.info("Automatically created approval instance for indicator {} with flow code {}", 
                        indicatorId, flowCode);
            } catch (BusinessException e) {
                // Re-throw business exceptions with custom message
                throw new BusinessException("下发失败：无法创建审批实例，请联系管理员");
            } catch (Exception e) {
                log.error("Failed to create approval instance for indicator {}: {}", indicatorId, e.getMessage(), e);
                throw new BusinessException("下发失败：无法创建审批实例，请联系管理员");
            }
        }
        */
        
        return toIndicatorVO(updatedIndicator);
    }
    /**
     * Validate that lifecycle status and progress approval status combination is valid.
     *
     * This method ensures that the two independent status fields can coexist without conflicts.
     * All combinations are valid except when lifecycle status is ARCHIVED.
     *
     * Valid combinations include:
     * - DRAFT + NONE (newly created indicator)
     * - PENDING_REVIEW + NONE (indicator under review)
     * - DISTRIBUTED + NONE (distributed, awaiting progress)
     * - DISTRIBUTED + DRAFT (distributed, progress draft saved)
     * - DISTRIBUTED + PENDING (distributed, progress awaiting approval) ← This should be allowed
     * - DISTRIBUTED + APPROVED (distributed, progress approved)
     * - DISTRIBUTED + REJECTED (distributed, progress rejected)
     *
     * @param lifecycleStatus the indicator's lifecycle status
     * @param progressApprovalStatus the indicator's progress approval status
     * @throws BusinessException if the combination is invalid
     */
    private void validateStatusCombination(IndicatorStatus lifecycleStatus,
                                         ProgressApprovalStatus progressApprovalStatus) {
        // Only restriction: archived indicators cannot be updated
        if (lifecycleStatus == IndicatorStatus.ARCHIVED) {
            throw new BusinessException("Cannot update archived indicator");
        }

        // All other combinations of lifecycle status and progress approval status are valid
        // The two status fields are independent and orthogonal
        log.debug("Validated status combination: lifecycle={}, progressApproval={}",
                 lifecycleStatus, progressApprovalStatus);
    }
    
    /**
     * Determine the approval flow code based on the indicator's responsible department
     * @param indicator The indicator to determine the flow code for
     * @return "INDICATOR_COLLEGE_APPROVAL" if responsibleDept contains "学院", otherwise "INDICATOR_DEFAULT_APPROVAL"
     */
    private String determineApprovalFlowCode(Indicator indicator) {
        String responsibleDept = indicator.getResponsibleDept();
        if (responsibleDept != null && responsibleDept.contains("学院")) {
            return "INDICATOR_COLLEGE_APPROVAL";
        }
        return "INDICATOR_DEFAULT_APPROVAL";
    }
    
    /**
     * Get the current authenticated user's ID from SecurityContext
     * @return The current user's ID, or null if not authenticated
     */
    private Long getCurrentUserId() {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String username) {
                    return userRepository.findByUsername(username)
                            .map(SysUser::getId)
                            .orElse(null);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get current user ID: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Update milestones for an indicator
     * - Delete milestones not in the request
     * - Update existing milestones
     * - Create new milestones (milestoneId = 0 or null)
     */
    private void updateIndicatorMilestones(Indicator indicator, List<MilestoneUpdateRequest> milestoneRequests) {
        log.info("[updateIndicatorMilestones] 开始更新指标 {} 的里程碑", indicator.getIndicatorId());
        log.info("[updateIndicatorMilestones] 收到 {} 个里程碑更新请求", milestoneRequests.size());
        
        // Get existing milestones
        List<Milestone> existingMilestones = milestoneRepository.findByIndicator_IndicatorId(indicator.getIndicatorId());
        log.info("[updateIndicatorMilestones] 数据库中现有 {} 个里程碑", existingMilestones.size());
        
        // Collect IDs from request
        Set<Long> requestedIds = milestoneRequests.stream()
                .map(MilestoneUpdateRequest::getMilestoneId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        log.info("[updateIndicatorMilestones] 请求中包含的现有里程碑ID: {}", requestedIds);
        
        // Delete milestones not in request
        List<Milestone> toDelete = existingMilestones.stream()
                .filter(m -> !requestedIds.contains(m.getMilestoneId()))
                .collect(Collectors.toList());
        log.info("[updateIndicatorMilestones] 将删除 {} 个里程碑: {}", toDelete.size(), 
                toDelete.stream().map(Milestone::getMilestoneId).collect(Collectors.toList()));
        toDelete.forEach(milestoneRepository::delete);
        
        // Update or create milestones
        int updateCount = 0;
        int createCount = 0;
        for (MilestoneUpdateRequest req : milestoneRequests) {
            log.info("[updateIndicatorMilestones] 处理里程碑: milestoneId={}, name={}", 
                    req.getMilestoneId(), req.getMilestoneName());
            
            if (req.getMilestoneId() != null && req.getMilestoneId() > 0) {
                // Update existing milestone
                Milestone milestone = milestoneRepository.findById(req.getMilestoneId())
                        .orElseThrow(() -> new ResourceNotFoundException("Milestone", req.getMilestoneId()));
                updateMilestoneFromRequest(milestone, req);
                milestoneRepository.save(milestone);
                updateCount++;
                log.info("[updateIndicatorMilestones] 更新了里程碑 {}", req.getMilestoneId());
            } else {
                // Create new milestone
                Milestone newMilestone = new Milestone();
                newMilestone.setIndicator(indicator);  // Set the indicator relationship
                updateMilestoneFromRequest(newMilestone, req);
                Milestone saved = milestoneRepository.save(newMilestone);
                createCount++;
                log.info("[updateIndicatorMilestones] 创建了新里程碑，ID={}, name={}", 
                        saved.getMilestoneId(), saved.getMilestoneName());
            }
        }
        
        log.info("[updateIndicatorMilestones] 完成更新: 更新了 {} 个，创建了 {} 个，删除了 {} 个", 
                updateCount, createCount, toDelete.size());
    }
    
    /**
     * Update milestone fields from request
     */
    private void updateMilestoneFromRequest(Milestone milestone, MilestoneUpdateRequest req) {
        if (req.getMilestoneName() != null) {
            // 清理空白字符并截断过长的名称（数据库限制 200 字符）
            String name = req.getMilestoneName().trim();
            if (name.length() > 200) {
                log.warn("Milestone name too long ({}), truncating to 200 characters: {}", name.length(), name);
                name = name.substring(0, 197) + "...";
            }
            milestone.setMilestoneName(name);
        }
        if (req.getTargetProgress() != null) {
            milestone.setTargetProgress(req.getTargetProgress());
        }
        if (req.getDueDate() != null) {
            milestone.setDueDate(LocalDate.parse(req.getDueDate()));
        }
        if (req.getStatus() != null) {
            milestone.setStatus(MilestoneStatus.valueOf(req.getStatus()));
        }
        // Note: Milestone entity doesn't have weightPercent field, skip it
        if (req.getSortOrder() != null) {
            milestone.setSortOrder(req.getSortOrder());
        }
        milestone.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Delete an indicator (soft delete)
     */
    @Transactional
    public void deleteIndicator(Long indicatorId) {
        // Find indicator without filtering by isDeleted to check if it's already archived
        Indicator indicator = indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Indicator", indicatorId));
        
        // Check if already archived
        if (indicator.getStatus() == IndicatorStatus.ARCHIVED) {
            throw new BusinessException("Indicator is already archived and cannot be deleted again");
        }
        
        // Soft delete: set both isDeleted and status
        indicator.setIsDeleted(true);
        indicator.setStatus(IndicatorStatus.ARCHIVED);
        indicator.setUpdatedAt(LocalDateTime.now());
        indicatorRepository.save(indicator);
    }

    /**
     * Search indicators by keyword
     */
    public List<IndicatorVO> searchIndicators(String keyword) {
        return indicatorRepository.searchByKeyword(keyword).stream()
                .map(this::toIndicatorVO)
                .collect(Collectors.toList());
    }

    /**
     * Find indicator entity by ID
     */
    public Indicator findIndicatorById(Long indicatorId) {
        Indicator indicator = indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Indicator", indicatorId));
        if (indicator.getIsDeleted()) {
            throw new ResourceNotFoundException("Indicator", indicatorId);
        }
        return indicator;
    }

    // ============ Helper Methods ============
    
    /**
     * Batch convert Indicator entities to VOs with optimized queries
     */
    private List<IndicatorVO> toIndicatorVOsBatch(List<Indicator> indicators) {
        if (indicators.isEmpty()) {
            return List.of();
        }
        
        // 批量加载里程碑
        Set<Long> indicatorIds = indicators.stream()
                .map(Indicator::getIndicatorId)
                .collect(Collectors.toSet());
        
        List<Milestone> allMilestones = milestoneRepository.findByIndicator_IndicatorIdIn(new ArrayList<>(indicatorIds));
        Map<Long, List<MilestoneVO>> milestonesByIndicator = allMilestones.stream()
                .collect(Collectors.groupingBy(
                    m -> m.getIndicator().getIndicatorId(),
                    Collectors.mapping(this::toMilestoneVO, Collectors.toList())
                ));
        
        // 批量加载任务名称（避免 N+1 问题）
        Set<Long> taskIds = indicators.stream()
                .map(Indicator::getTaskId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, String> taskNameMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            String sql = "SELECT task_id, task_name FROM sys_task WHERE task_id IN (:taskIds)";
            List<Object[]> results = entityManager.createNativeQuery(sql)
                    .setParameter("taskIds", new ArrayList<>(taskIds))
                    .getResultList();
            for (Object[] row : results) {
                taskNameMap.put(((Number) row[0]).longValue(), row[1].toString());
            }
        }
        
        // 批量加载审批实例（避免 N+1 问题）
        // TEMPORARILY DISABLED: audit_instance table structure mismatch
        Map<Long, AuditInstance> auditInstanceMap = new HashMap<>();
        /*
        try {
            List<AuditInstance> auditInstances = auditInstanceRepository
                    .findActiveInstancesByEntities(AuditEntityType.INDICATOR, new ArrayList<>(indicatorIds));
            auditInstanceMap = auditInstances.stream()
                    .collect(Collectors.toMap(AuditInstance::getEntityId, ai -> ai, (a1, a2) -> a1));
            log.info("[Performance] Batch loaded {} audit instances for {} indicators", 
                    auditInstances.size(), indicatorIds.size());
        } catch (Exception e) {
            log.warn("[Performance] Failed to batch load audit instances: {}", e.getMessage());
        }
        */
        
        // 创建最终的 auditInstanceMap 引用（用于 lambda）
        final Map<Long, AuditInstance> finalAuditInstanceMap = auditInstanceMap;
        
        // 直接组装VO（使用 indicator 表中的字段）
        return indicators.stream()
                .map(indicator -> {
                    List<MilestoneVO> milestones = milestonesByIndicator.getOrDefault(
                        indicator.getIndicatorId(), 
                        List.of()
                    );
                    
                    // 如果 ownerDept 为空,从 ownerOrg 关联对象中获取
                    String ownerDeptName = indicator.getOwnerDept();
                    if (ownerDeptName == null && indicator.getOwnerOrg() != null) {
                        ownerDeptName = indicator.getOwnerOrg().getName();
                    }
                    
                    // 如果 responsibleDept 为空,从 targetOrg 关联对象中获取
                    String responsibleDeptName = indicator.getResponsibleDept();
                    if (responsibleDeptName == null && indicator.getTargetOrg() != null) {
                        responsibleDeptName = indicator.getTargetOrg().getName();
                    }
                    
                    // 从缓存中获取任务名称
                    String taskName = indicator.getTaskId() != null 
                        ? taskNameMap.getOrDefault(indicator.getTaskId(), getTaskNameByTaskId(indicator.getTaskId()))
                        : "未分配任务";
                    
                    IndicatorVO indicatorVO = new IndicatorVO(
                        indicator.getIndicatorId(),
                        indicator.getTaskId(),
                        indicator.getParentIndicatorId(),
                        indicator.getIndicatorDesc(),
                        indicator.getWeightPercent(),
                        indicator.getSortOrder(),
                        indicator.getRemark(),
                        indicator.getType(),
                        indicator.getProgress(),
                        indicator.getCreatedAt(),
                        indicator.getUpdatedAt(),
                        indicator.getYear(),
                        ownerDeptName,
                        responsibleDeptName,
                        indicator.getTargetOrg() != null ? indicator.getTargetOrg().getId() : null, // targetOrgId
                        indicator.getOwnerOrg() != null ? indicator.getOwnerOrg().getId() : null, // ownerOrgId
                        indicator.getWeightPercent(), // weight
                        taskName, // taskName
                        indicator.getCanWithdraw(),
                        indicator.getStatus(),
                        indicator.getDistributionStatus(),
                        indicator.getIsQualitative(),
                        indicator.getType1(),
                        indicator.getType2(),
                        indicator.getLevel() != null ? indicator.getLevel().name() : null, // level
                        indicator.getUnit(), // unit
                        indicator.getActualValue(), // actualValue
                        indicator.getTargetValue(), // targetValue
                        indicator.getResponsiblePerson(), // responsiblePerson
                        // isStrategic - 判断逻辑: owner_dept = '战略发展部' 且 responsible_dept 不包含"学院"
                        "战略发展部".equals(ownerDeptName) && responsibleDeptName != null && !responsibleDeptName.contains("学院"),
                        indicator.getStatusAudit(), // statusAudit - JSON string
                        indicator.getProgressApprovalStatus() != null ? indicator.getProgressApprovalStatus().name() : null, // progressApprovalStatus - convert enum to string
                        indicator.getPendingProgress(), // pendingProgress
                        indicator.getPendingRemark(), // pendingRemark
                        indicator.getPendingAttachments(), // pendingAttachments
                        List.of(), // childIndicators
                        milestones  // milestones - 使用实际数据
                    );
                    
                    // 使用批量加载的审批实例计算显示状态
                    indicatorVO.setDisplayStatus(calculateDisplayStatusFromMap(indicator, finalAuditInstanceMap));
                    
                    return indicatorVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert Indicator entity to IndicatorVO
     */
    private IndicatorVO toIndicatorVO(Indicator indicator) {
        // 加载里程碑
        List<MilestoneVO> milestones = milestoneRepository
                .findByIndicator_IndicatorIdOrderByDueDateAsc(indicator.getIndicatorId())
                .stream()
                .map(this::toMilestoneVO)
                .collect(Collectors.toList());
        
        // 如果 ownerDept 为空,从 ownerOrg 关联对象中获取
        String ownerDeptName = indicator.getOwnerDept();
        if (ownerDeptName == null && indicator.getOwnerOrg() != null) {
            ownerDeptName = indicator.getOwnerOrg().getName();
        }
        
        // 如果 responsibleDept 为空,从 targetOrg 关联对象中获取
        String responsibleDeptName = indicator.getResponsibleDept();
        if (responsibleDeptName == null && indicator.getTargetOrg() != null) {
            responsibleDeptName = indicator.getTargetOrg().getName();
        }
        
        IndicatorVO vo = new IndicatorVO(
            indicator.getIndicatorId(),
            indicator.getTaskId(),
            indicator.getParentIndicatorId(),
            indicator.getIndicatorDesc(),
            indicator.getWeightPercent(),
            indicator.getSortOrder(),
            indicator.getRemark(),
            indicator.getType(),
            indicator.getProgress(),
            indicator.getCreatedAt(),
            indicator.getUpdatedAt(),
            indicator.getYear(),
            ownerDeptName,
            responsibleDeptName,
            indicator.getTargetOrg() != null ? indicator.getTargetOrg().getId() : null, // targetOrgId
            indicator.getOwnerOrg() != null ? indicator.getOwnerOrg().getId() : null, // ownerOrgId
            indicator.getWeightPercent(), // weight
            generateTaskName(indicator), // taskName
            indicator.getCanWithdraw(),
            indicator.getStatus(),
            indicator.getDistributionStatus(),
            indicator.getIsQualitative(),
            indicator.getType1(),
            indicator.getType2(),
            indicator.getLevel() != null ? indicator.getLevel().name() : null, // level
            indicator.getUnit(), // unit
            indicator.getActualValue(), // actualValue
            indicator.getTargetValue(), // targetValue
            indicator.getResponsiblePerson(), // responsiblePerson
            // 判断逻辑: owner_dept = '战略发展部' 且 responsible_dept 不包含"学院"
            "战略发展部".equals(ownerDeptName) && responsibleDeptName != null && !responsibleDeptName.contains("学院"), // isStrategic
            indicator.getStatusAudit(), // statusAudit - JSON string
            indicator.getProgressApprovalStatus() != null ? indicator.getProgressApprovalStatus().name() : null, // progressApprovalStatus - convert enum to string
            indicator.getPendingProgress(), // pendingProgress
            indicator.getPendingRemark(), // pendingRemark
            indicator.getPendingAttachments(), // pendingAttachments
            List.of(), // childIndicators
            milestones  // milestones - 使用实际数据
        );
        
        // 计算并设置显示状态
        vo.setDisplayStatus(calculateDisplayStatus(indicator));
        
        return vo;
    }
    
    /**
     * 计算指标的显示状态
     * 根据审批实例状态动态计算
     * 
     * @param indicator 指标实体
     * @return 显示状态: DRAFT, PENDING_APPROVAL, DISTRIBUTED
     */
    private String calculateDisplayStatus(Indicator indicator) {
        // TEMPORARILY DISABLED: audit_instance table structure mismatch
        // Return DRAFT as default until audit_instance table is fixed
        return "DRAFT";
        
        /*
        try {
            // 1. 查询审批实例
            Optional<AuditInstance> auditInstance = auditInstanceRepository
                    .findActiveInstanceByEntity(AuditEntityType.INDICATOR, indicator.getIndicatorId());
            
            // 2. 无审批实例 = 草稿
            if (!auditInstance.isPresent()) {
                return "DRAFT";
            }
            
            // 3. 根据审批状态映射
            String status = auditInstance.get().getStatus();
            switch (status) {
                case "IN_PROGRESS":
                case "PENDING":
                    return "PENDING_APPROVAL";  // 待审核
                case "APPROVED":
                    return "DISTRIBUTED";       // 已下发
                case "REJECTED":
                    return "DRAFT";             // 草稿
                default:
                    return "DRAFT";
            }
        } catch (Exception e) {
            // Handle test environment where audit_instance table might not exist
            log.debug("Failed to calculate display status for indicator {}: {}", 
                    indicator.getIndicatorId(), e.getMessage());
            return "DRAFT";  // Default to DRAFT in test environment
        }
        */
    }

    /**
     * Calculate display status from pre-loaded audit instance map (batch optimization)
     */
    private String calculateDisplayStatusFromMap(Indicator indicator, Map<Long, AuditInstance> auditInstanceMap) {
        try {
            // 1. 从 map 中获取审批实例
            AuditInstance auditInstance = auditInstanceMap.get(indicator.getIndicatorId());
            
            // 2. 无审批实例 = 草稿
            if (auditInstance == null) {
                return "DRAFT";
            }
            
            // 3. 根据审批状态映射
            String status = auditInstance.getStatus();
            switch (status) {
                case "IN_PROGRESS":
                case "PENDING":
                    return "PENDING_APPROVAL";  // 待审核
                case "APPROVED":
                    return "DISTRIBUTED";       // 已下发
                case "REJECTED":
                    return "DRAFT";             // 草稿
                default:
                    return "DRAFT";
            }
        } catch (Exception e) {
            log.debug("Failed to calculate display status for indicator {}: {}", 
                    indicator.getIndicatorId(), e.getMessage());
            return "DRAFT";
        }
    }
    
    /**
     * 生成任务名称
     * 优先从 sys_task 表查询真实任务名称，如果没有则返回默认值
     */
    private String generateTaskName(Indicator indicator) {
        Long taskId = indicator.getTaskId();
        
        // 如果 task_id 为 NULL，返回默认值
        if (taskId == null) {
            return "未分配任务";
        }
        
        // 尝试从 sys_task 表查询任务名称
        try {
            String sql = "SELECT task_name FROM sys_task WHERE task_id = :taskId";
            Object result = entityManager.createNativeQuery(sql)
                    .setParameter("taskId", taskId)
                    .getSingleResult();
            
            if (result != null) {
                return result.toString();
            }
        } catch (jakarta.persistence.NoResultException e) {
            // 任务不存在，使用备用方案
            log.debug("No task found for task_id {}, using fallback", taskId);
        } catch (Exception e) {
            // 其他查询错误
            log.warn("Failed to query task name for task_id {}: {}", taskId, e.getMessage());
        }
        
        // 备用方案：使用 task_id 映射表生成有意义的任务名称
        return getTaskNameByTaskId(taskId);
    }
    
    /**
     * Convert Milestone entity to MilestoneVO
     */
    private MilestoneVO toMilestoneVO(Milestone milestone) {
        Indicator indicator = milestone.getIndicator();
        return new MilestoneVO(
            milestone.getMilestoneId(),
            indicator != null ? indicator.getIndicatorId() : null,
            indicator != null ? indicator.getIndicatorDesc() : null,
            milestone.getMilestoneName(),
            milestone.getMilestoneDesc(),
            milestone.getDueDate(),
            null, // weightPercent - system deprecated
            milestone.getStatus(),
            milestone.getSortOrder(),
            null, // inheritedFromId - DISABLED: inheritedFrom field removed from database
            milestone.getCreatedAt(),
            milestone.getUpdatedAt(),
            milestone.getTargetProgress(),
            milestone.getIsPaired()
        );
    }
    
    /**
     * 根据 task_id 获取任务名称
     * 基于数据分析结果，为常见的 task_id 提供有意义的名称
     */
    private String getTaskNameByTaskId(Long taskId) {
        // Handle null taskId
        if (taskId == null) {
            return "未分配任务";
        }
        
        // 基于数据分析的任务名称映射
        // 这些名称是根据每个 task_id 下指标的共同特征生成的
        if (taskId >= 11 && taskId <= 20) {
            // 第一批任务（task_id 11-20）：主要是教学、科研、学生工作相关
            String[] names = {
                "教学质量提升任务",      // 11
                "科研创新发展任务",      // 12
                "学科建设任务",          // 13
                "国际交流合作任务",      // 14
                "师资队伍建设任务",      // 15
                "学生工作任务",          // 16
                "教学改革任务",          // 17
                "科研平台建设任务",      // 18
                "人才培养任务",          // 19
                "信息化建设任务"         // 20
            };
            int index = (int)(taskId - 11);
            if (index < names.length) {
                return names[index];
            }
        } else if (taskId >= 21 && taskId <= 30) {
            // 第二批任务（task_id 21-30）
            String[] names = {
                "实践教学任务",          // 21
                "学术交流任务",          // 22
                "质量保障任务",          // 23
                "创新创业任务",          // 24
                "社会服务任务",          // 25
                "文化建设任务",          // 26
                "管理服务任务",          // 27
                "资源保障任务",          // 28
                "安全稳定任务",          // 29
                "综合改革任务"           // 30
            };
            int index = (int)(taskId - 21);
            if (index < names.length) {
                return names[index];
            }
        } else if (taskId >= 31 && taskId <= 42) {
            // 第三批任务（task_id 31-42）
            String[] names = {
                "党建工作任务",          // 31
                "思政教育任务",          // 32
                "招生就业任务",          // 33
                "后勤保障任务",          // 34
                "财务管理任务",          // 35
                "人事管理任务",          // 36
                "制度建设任务",          // 37
                "评估认证任务",          // 38
                "对外合作任务",          // 39
                "校园建设任务",          // 40
                "图书档案任务",          // 41
                "继续教育任务"           // 42
            };
            int index = (int)(taskId - 31);
            if (index < names.length) {
                return names[index];
            }
        } else if (taskId >= 44 && taskId <= 46) {
            // 特殊任务
            return "专项任务 " + taskId;
        } else if (taskId >= 91000 && taskId <= 92999) {
            // 测试任务
            return "测试任务 " + taskId;
        }
        
        // 默认名称
        return "战略任务 " + taskId;
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
        SysOrg targetOrg = orgRepository.findById(targetOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Target Organization", targetOrgId));

        // Create child indicator using builder
        Indicator childIndicator = Indicator.builder()
                .taskId(parentIndicator.getTaskId())
                .parentIndicatorId(parentIndicatorId)
                .indicatorDesc(customDesc != null && !customDesc.trim().isEmpty() 
                        ? customDesc : parentIndicator.getIndicatorDesc())
                .weightPercent(parentIndicator.getWeightPercent())
                .sortOrder(parentIndicator.getSortOrder())
                .type(parentIndicator.getType())
                .progress(0)
                .ownerDept(parentIndicator.getResponsibleDept())
                .responsibleDept(targetOrg.getName())
                .year(parentIndicator.getYear())
                .status(IndicatorStatus.ACTIVE)
                .remark("下发自指标 #" + parentIndicatorId)
                .canWithdraw(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Indicator savedChild = indicatorRepository.save(childIndicator);

        log.info("Distributed indicator {} to org {} as new indicator {}", 
                parentIndicatorId, targetOrgId, savedChild.getIndicatorId());

        return toIndicatorVO(savedChild);
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
     * Get all child indicators distributed from a parent indicator
     * 
     * @param parentIndicatorId parent indicator ID
     * @return list of child indicators
     */
    public List<IndicatorVO> getDistributedIndicators(Long parentIndicatorId) {
        return indicatorRepository.findByParentIndicatorIdDirect(parentIndicatorId).stream()
                .filter(this::isDistributed)
                .map(this::toIndicatorVO)
                .collect(Collectors.toList());
    }

    /**
     * Withdraw a distributed indicator, reverting it back to DRAFT status
     * Only strategic department users can withdraw indicators
     * 
     * @param indicatorId indicator ID to withdraw
     * @return updated indicator VO
     */
    /**
         * Withdraw a distributed indicator, reverting it back to DRAFT status.
         * 
         * <p><strong>LIFECYCLE OPERATION:</strong> This method operates on the indicator's lifecycle status field,
         * transitioning from DISTRIBUTED/ACTIVE → DRAFT. This is completely separate from progress approval
         * withdrawal operations.</p>
         * 
         * <p><strong>Status Field Affected:</strong> {@code status} (lifecycle status)</p>
         * <p><strong>Valid Transitions:</strong></p>
         * <ul>
         *   <li>DISTRIBUTED → DRAFT</li>
         *   <li>ACTIVE → DRAFT (legacy status support)</li>
         * </ul>
         * 
         * <p><strong>NOT AFFECTED:</strong> {@code progressApprovalStatus} field remains unchanged.
         * For progress approval withdrawal (PENDING → DRAFT in progressApprovalStatus), 
         * use {@link #withdrawProgressApproval(Long)} instead.</p>
         * 
         * <p><strong>Authorization:</strong> Only strategic department users can withdraw indicators.</p>
         * 
         * @param indicatorId indicator ID to withdraw from distribution
         * @return updated indicator VO with DRAFT lifecycle status
         * @throws BusinessException if indicator is not in DISTRIBUTED or ACTIVE status
         * @throws ResourceNotFoundException if indicator does not exist
         * 
         * @see #withdrawProgressApproval(Long) for progress approval withdrawal
         */
        @Transactional
        public IndicatorVO withdrawIndicator(Long indicatorId) {
            Indicator indicator = findIndicatorById(indicatorId);

            // Validate indicator is in a distributed state (lifecycle status check only)
            if (indicator.getStatus() != IndicatorStatus.DISTRIBUTED && 
                indicator.getStatus() != IndicatorStatus.ACTIVE) {
                throw new BusinessException("只能撤回状态为已下发的指标");
            }

            // Revert to DRAFT status (lifecycle operation only - does not affect progress approval status)
            indicator.setStatus(IndicatorStatus.DRAFT);
            indicator.setCanWithdraw(true);
            indicator.setUpdatedAt(LocalDateTime.now());

            Indicator savedIndicator = indicatorRepository.save(indicator);

            log.info("Withdrew indicator {} back to DRAFT lifecycle status (progress approval status unchanged)", indicatorId);

            return toIndicatorVO(savedIndicator);
        }
        /**
         * Withdraw a pending progress approval submission, reverting it back to DRAFT approval status.
         *
         * <p><strong>APPROVAL WORKFLOW OPERATION:</strong> This method operates on the indicator's progress
         * approval status field, transitioning from PENDING → DRAFT. This is completely separate from
         * lifecycle status withdrawal operations.</p>
         *
         * <p><strong>Status Field Affected:</strong> {@code progressApprovalStatus} (approval workflow status)</p>
         * <p><strong>Valid Transitions:</strong></p>
         * <ul>
         *   <li>PENDING → DRAFT</li>
         * </ul>
         *
         * <p><strong>NOT AFFECTED:</strong> {@code status} field (lifecycle status) remains unchanged.
         * For lifecycle withdrawal (DISTRIBUTED → DRAFT in status),
         * use {@link #withdrawIndicator(Long)} instead.</p>
         *
         * <p><strong>Authorization:</strong> Department users can withdraw their own pending progress submissions.</p>
         *
         * @param indicatorId indicator ID to withdraw progress approval for
         * @return updated indicator VO with DRAFT progress approval status
         * @throws BusinessException if progress approval status is not PENDING
         * @throws ResourceNotFoundException if indicator does not exist
         *
         * @see #withdrawIndicator(Long) for lifecycle status withdrawal
         */
        @Transactional
        public IndicatorVO withdrawProgressApproval(Long indicatorId) {
            Indicator indicator = findIndicatorById(indicatorId);

            // Validate progress approval is in pending state (approval status check only)
            if (indicator.getProgressApprovalStatus() != ProgressApprovalStatus.PENDING) {
                throw new BusinessException("只能撤回状态为待审批的进度提交");
            }

            // Revert to DRAFT progress approval status (approval workflow operation only - does not affect lifecycle status)
            indicator.setProgressApprovalStatus(ProgressApprovalStatus.DRAFT);
            indicator.setUpdatedAt(LocalDateTime.now());

            Indicator savedIndicator = indicatorRepository.save(indicator);

            log.info("Withdrew progress approval for indicator {} back to DRAFT approval status (lifecycle status unchanged)", indicatorId);

            return toIndicatorVO(savedIndicator);
        }

    /**
     * Check if an indicator can be distributed (has valid level and is active)
     * 
     * @param indicatorId indicator ID
     * @return distribution eligibility info
     */
    public DistributionEligibility checkDistributionEligibility(Long indicatorId) {
        Indicator indicator = findIndicatorById(indicatorId);

        boolean canDistribute = isDistributedStatus(indicator.getStatus())
                && indicator.getLevel() == com.sism.enums.IndicatorLevel.PRIMARY;

        String reason = "";
        if (indicator.getStatus() != IndicatorStatus.ACTIVE) {
            reason = "指标状态不是 ACTIVE";
        } else if (indicator.getLevel() != com.sism.enums.IndicatorLevel.PRIMARY) {
            reason = "只有 PRIMARY 级别的指标可以下发";
        }

        int existingDistributions = indicatorRepository.findByParentIndicatorIdDirect(indicatorId).size();

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
     * Get indicators filtered by type1 (定性/定量) - including all statuses except deleted
     * Requirements: 7.3, 7.5 - Filter by indicator type
     * Changed from filtering by ACTIVE status to filtering all non-deleted indicators
     *
     * @param type1 indicator type1 value ("定性" or "定量")
     * @return list of matching indicators
     */
    public List<IndicatorVO> getIndicatorsByType1(String type1) {
        return indicatorRepository.findByType1(type1).stream()
                .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                .map(this::toIndicatorVO)
                .collect(Collectors.toList());
    }

    /**
     * Get indicators filtered by type2 (发展性/基础性) - including all statuses except deleted
     * Requirements: 7.3, 7.5 - Filter by indicator type
     * Changed from filtering by ACTIVE status to filtering all non-deleted indicators
     *
     * @param type2 indicator type2 value ("发展性" or "基础性")
     * @return list of matching indicators
     */
    public List<IndicatorVO> getIndicatorsByType2(String type2) {
        return indicatorRepository.findByType2(type2).stream()
                .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                .map(this::toIndicatorVO)
                .collect(Collectors.toList());
    }

    /**
     * Get indicators filtered by isQualitative flag - including all statuses except deleted
     * Requirements: 7.3, 7.5 - Filter by qualitative/quantitative
     * Changed from filtering by ACTIVE status to filtering all non-deleted indicators
     *
     * @param isQualitative true for qualitative, false for quantitative
     * @return list of matching indicators
     */
    public List<IndicatorVO> getIndicatorsByQualitative(Boolean isQualitative) {
        return indicatorRepository.findByIsQualitative(isQualitative).stream()
                .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                .map(this::toIndicatorVO)
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
                .map(this::toIndicatorVO)
                .collect(Collectors.toList());
    }

    /**
     * Get indicators with combined filters - including all statuses except deleted
     * Requirements: 7.3, 7.5 - Combined filtering
     * Changed from defaulting to ACTIVE status to including all non-deleted indicators when no status specified
     *
     * @param type1 indicator type1 (optional)
     * @param type2 indicator type2 (optional)
     * @param status indicator status (optional, if null returns all non-deleted indicators)
     * @return list of matching indicators
     */
    public List<IndicatorVO> getIndicatorsWithFilters(String type1, String type2, IndicatorStatus status) {
        // 如果指定了状态，使用状态过滤；否则返回所有非删除状态的指标
        if (type1 != null && type2 != null) {
            if (status != null) {
                return indicatorRepository.findByType1AndType2AndStatus(type1, type2, status).stream()
                        .map(this::toIndicatorVO)
                        .collect(Collectors.toList());
            } else {
                return indicatorRepository.findByType1AndType2(type1, type2).stream()
                        .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                        .map(this::toIndicatorVO)
                        .collect(Collectors.toList());
            }
        } else if (type1 != null) {
            if (status != null) {
                return indicatorRepository.findByType1AndStatus(type1, status).stream()
                        .map(this::toIndicatorVO)
                        .collect(Collectors.toList());
            } else {
                return indicatorRepository.findByType1(type1).stream()
                        .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                        .map(this::toIndicatorVO)
                        .collect(Collectors.toList());
            }
        } else if (type2 != null) {
            if (status != null) {
                return indicatorRepository.findByType2AndStatus(type2, status).stream()
                        .map(this::toIndicatorVO)
                        .collect(Collectors.toList());
            } else {
                return indicatorRepository.findByType2(type2).stream()
                        .filter(i -> i.getIsDeleted() == null || !i.getIsDeleted())
                        .map(this::toIndicatorVO)
                        .collect(Collectors.toList());
            }
        } else {
            if (status != null) {
                return indicatorRepository.findByStatus(status).stream()
                        .map(this::toIndicatorVO)
                        .collect(Collectors.toList());
            } else {
                // Return all non-deleted indicators
                return getAllIndicators();
            }
        }
    }

    // ==================== Indicator Review Workflow (指标审核流程) ====================

    /**
     * Submit indicator for review (DRAFT → PENDING_REVIEW)
     * Requirements: 2.3, 2.5, 2.6, 2.7, 2.8
     *
     * @param indicatorId indicator ID
     * @param userId user performing the action
     * @return updated indicator VO
     */
    @Transactional
    public IndicatorVO submitForReview(Long indicatorId, Long userId) {
        Indicator indicator = findIndicatorById(indicatorId);

        // Validate indicator is in DRAFT state
        if (indicator.getStatus() != IndicatorStatus.DRAFT) {
            throw new BusinessException("只能提交状态为 DRAFT 的指标进行审核");
        }

        // Validate weight sum equals 100% if indicator has children
        List<Indicator> children = indicatorRepository.findByParentIndicatorIdDirect(indicatorId);
        if (!children.isEmpty()) {
            BigDecimal totalWeight = children.stream()
                    .map(Indicator::getWeightPercent)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
                throw new BusinessException("子指标权重总和必须等于 100%，当前为 " + totalWeight + "%");
            }
        }

        // Update status to PENDING_REVIEW
        indicator.setStatus(IndicatorStatus.PENDING_REVIEW);
        indicator.setUpdatedAt(LocalDateTime.now());
        Indicator updatedIndicator = indicatorRepository.save(indicator);

        // Create audit log entry
        SysUser actorUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        auditLogService.logUpdate(
                AuditEntityType.INDICATOR,
                indicatorId,
                Map.of("status", IndicatorStatus.DRAFT),
                Map.of("status", IndicatorStatus.PENDING_REVIEW),
                actorUser,
                actorUser.getOrg(),
                "提交指标进行审核"
        );

        log.info("Indicator {} submitted for review by user {}", indicatorId, userId);
        return toIndicatorVO(updatedIndicator);
    }

    /**
     * Approve indicator review (PENDING_REVIEW → DISTRIBUTED)
     * Requirements: 2.3, 2.5, 2.6, 2.7, 2.8
     *
     * @param indicatorId indicator ID
     * @param userId user performing the action
     * @return updated indicator VO
     */
    @Transactional
    public IndicatorVO approveIndicatorReview(Long indicatorId, Long userId) {
        Indicator indicator = findIndicatorById(indicatorId);

        // Validate indicator is in PENDING_REVIEW state
        if (indicator.getStatus() != IndicatorStatus.PENDING_REVIEW) {
            throw new BusinessException("只能审核通过状态为 PENDING_REVIEW 的指标");
        }

        // Validate user has strategic dept role
        SysUser actorUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (actorUser.getOrg() == null ||
            !"战略发展部".equals(actorUser.getOrg().getName())) {
            throw new BusinessException("只有战略发展部用户可以审核通过指标");
        }

        // Update status to DISTRIBUTED
        indicator.setStatus(IndicatorStatus.DISTRIBUTED);
        indicator.setCanWithdraw(false);
        indicator.setUpdatedAt(LocalDateTime.now());
        Indicator updatedIndicator = indicatorRepository.save(indicator);

        // Create audit log entry
        auditLogService.logUpdate(
                AuditEntityType.INDICATOR,
                indicatorId,
                Map.of("status", IndicatorStatus.PENDING_REVIEW),
                Map.of("status", IndicatorStatus.DISTRIBUTED),
                actorUser,
                actorUser.getOrg(),
                "审核通过指标定义"
        );

        log.info("Indicator {} review approved by user {}", indicatorId, userId);
        return toIndicatorVO(updatedIndicator);
    }

    /**
     * Reject indicator review (PENDING_REVIEW → DRAFT)
     * Requirements: 2.3, 2.5, 2.6, 2.7, 2.8
     *
     * @param indicatorId indicator ID
     * @param reason rejection reason
     * @param userId user performing the action
     * @return updated indicator VO
     */
    @Transactional
    public IndicatorVO rejectIndicatorReview(Long indicatorId, String reason, Long userId) {
        Indicator indicator = findIndicatorById(indicatorId);

        // Validate indicator is in PENDING_REVIEW state
        if (indicator.getStatus() != IndicatorStatus.PENDING_REVIEW) {
            throw new BusinessException("只能驳回状态为 PENDING_REVIEW 的指标");
        }

        // Validate user has strategic dept role
        SysUser actorUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (actorUser.getOrg() == null ||
            !"战略发展部".equals(actorUser.getOrg().getName())) {
            throw new BusinessException("只有战略发展部用户可以驳回指标");
        }

        // Update status to DRAFT
        indicator.setStatus(IndicatorStatus.DRAFT);
        indicator.setUpdatedAt(LocalDateTime.now());
        Indicator updatedIndicator = indicatorRepository.save(indicator);

        // Create audit log entry
        auditLogService.logUpdate(
                AuditEntityType.INDICATOR,
                indicatorId,
                Map.of("status", IndicatorStatus.PENDING_REVIEW),
                Map.of("status", IndicatorStatus.DRAFT),
                actorUser,
                actorUser.getOrg(),
                "驳回指标定义: " + (reason != null ? reason : "无原因")
        );

        log.info("Indicator {} review rejected by user {} with reason: {}", indicatorId, userId, reason);
        return toIndicatorVO(updatedIndicator);
    }

    // ==================== Legacy ACTIVE Status Compatibility ====================

    /**
     * Check if indicator status is equivalent to DISTRIBUTED (including legacy ACTIVE)
     *
     * Phase 1 of ACTIVE status migration: Treat ACTIVE as equivalent to DISTRIBUTED
     * in all business logic while maintaining backward compatibility.
     *
     * @param status The indicator status to check
     * @return true if status is DISTRIBUTED or legacy ACTIVE
     */
    private boolean isDistributedStatus(IndicatorStatus status) {
        return status == IndicatorStatus.DISTRIBUTED || status == IndicatorStatus.ACTIVE;
    }

    /**
     * Check if indicator is in distributed status (including legacy ACTIVE)
     *
     * @param indicator The indicator to check
     * @return true if indicator is distributed or has legacy ACTIVE status
     */
    private boolean isDistributed(Indicator indicator) {
        return isDistributedStatus(indicator.getStatus());
    }
}