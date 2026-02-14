package com.sism.service;

import com.sism.dto.AdhocTaskCreateRequest;
import com.sism.dto.AdhocTaskUpdateRequest;
import com.sism.entity.*;
import com.sism.enums.AdhocScopeType;
import com.sism.enums.AdhocTaskStatus;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.*;
import com.sism.vo.AdhocTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for adhoc task management
 * Provides CRUD operations with scope type handling for target organizations and indicators
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdhocTaskService {

    private final AdhocTaskRepository adhocTaskRepository;
    private final AdhocTaskTargetRepository adhocTaskTargetRepository;
    private final AdhocTaskIndicatorMapRepository adhocTaskIndicatorMapRepository;
    private final AssessmentCycleRepository assessmentCycleRepository;
    private final SysOrgRepository orgRepository;
    private final IndicatorRepository indicatorRepository;

    /**
     * Get adhoc task by ID
     * 
     * @param adhocTaskId adhoc task ID
     * @return adhoc task VO
     * @throws ResourceNotFoundException if adhoc task not found
     */
    public AdhocTaskVO getAdhocTaskById(Long adhocTaskId) {
        AdhocTask adhocTask = findAdhocTaskById(adhocTaskId);
        return toAdhocTaskVO(adhocTask, true);
    }


    /**
     * Get all adhoc tasks by cycle ID
     * 
     * @param cycleId assessment cycle ID
     * @return list of adhoc tasks
     */
    public List<AdhocTaskVO> getAdhocTasksByCycleId(Long cycleId) {
        return adhocTaskRepository.findByCycle_CycleId(cycleId).stream()
                .map(task -> toAdhocTaskVO(task, false))
                .collect(Collectors.toList());
    }

    /**
     * Get adhoc tasks by cycle ID with pagination
     * 
     * @param cycleId assessment cycle ID
     * @param pageable pagination parameters
     * @return page of adhoc tasks
     */
    public Page<AdhocTaskVO> getAdhocTasksByCycleId(Long cycleId, Pageable pageable) {
        return adhocTaskRepository.findByCycle_CycleId(cycleId, pageable)
                .map(task -> toAdhocTaskVO(task, false));
    }

    /**
     * Get adhoc tasks by creator organization ID
     * 
     * @param creatorOrgId creator organization ID
     * @return list of adhoc tasks
     */
    public List<AdhocTaskVO> getAdhocTasksByCreatorOrgId(Long creatorOrgId) {
        return adhocTaskRepository.findByCreatorOrg_Id(creatorOrgId).stream()
                .map(task -> toAdhocTaskVO(task, false))
                .collect(Collectors.toList());
    }

    /**
     * Get adhoc tasks by status
     * 
     * @param status adhoc task status
     * @return list of adhoc tasks
     */
    public List<AdhocTaskVO> getAdhocTasksByStatus(AdhocTaskStatus status) {
        return adhocTaskRepository.findByStatus(status).stream()
                .map(task -> toAdhocTaskVO(task, false))
                .collect(Collectors.toList());
    }

    /**
     * Get adhoc tasks by status with pagination
     * 
     * @param status adhoc task status
     * @param pageable pagination parameters
     * @return page of adhoc tasks
     */
    public Page<AdhocTaskVO> getAdhocTasksByStatus(AdhocTaskStatus status, Pageable pageable) {
        return adhocTaskRepository.findByStatus(status, pageable)
                .map(task -> toAdhocTaskVO(task, false));
    }

    /**
     * Search adhoc tasks by keyword
     * 
     * @param keyword search keyword
     * @return list of matching adhoc tasks
     */
    public List<AdhocTaskVO> searchAdhocTasks(String keyword) {
        return adhocTaskRepository.searchByKeyword(keyword).stream()
                .map(task -> toAdhocTaskVO(task, false))
                .collect(Collectors.toList());
    }

    /**
     * Get overdue adhoc tasks
     * 
     * @return list of overdue adhoc tasks
     */
    public List<AdhocTaskVO> getOverdueAdhocTasks() {
        return adhocTaskRepository.findOverdueTasks(java.time.LocalDate.now()).stream()
                .map(task -> toAdhocTaskVO(task, false))
                .collect(Collectors.toList());
    }


    /**
     * Create a new adhoc task
     * Requirements: 10.1 - Save task and determine target organizations based on scope type
     * Requirements: 10.2 - ALL_ORGS: distribute to all designated organizations
     * Requirements: 10.3 - BY_DEPT_ISSUED_INDICATORS: auto-extract secondary indicators
     * Requirements: 10.4 - CUSTOM: allow manual selection
     * 
     * @param request adhoc task creation request
     * @return created adhoc task VO
     */
    @Transactional
    public AdhocTaskVO createAdhocTask(AdhocTaskCreateRequest request) {
        // Validate assessment cycle exists
        AssessmentCycle cycle = assessmentCycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Cycle", request.getCycleId()));

        // Validate creator organization exists
        SysOrg creatorOrg = orgRepository.findById(request.getCreatorOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Creator Organization", request.getCreatorOrgId()));

        // Validate indicator if provided
        Indicator indicator = null;
        if (request.getIndicatorId() != null) {
            indicator = indicatorRepository.findById(request.getIndicatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Indicator", request.getIndicatorId()));
        }

        // Create adhoc task entity
        AdhocTask adhocTask = new AdhocTask();
        adhocTask.setCycle(cycle);
        adhocTask.setCreatorOrg(creatorOrg);
        adhocTask.setScopeType(request.getScopeType());
        adhocTask.setIndicator(indicator);
        adhocTask.setTaskTitle(request.getTaskTitle());
        adhocTask.setTaskDesc(request.getTaskDesc());
        adhocTask.setOpenAt(request.getOpenAt());
        adhocTask.setDueAt(request.getDueAt());
        adhocTask.setIncludeInAlert(request.getIncludeInAlert() != null ? request.getIncludeInAlert() : false);
        adhocTask.setRequireIndicatorReport(request.getRequireIndicatorReport() != null ? request.getRequireIndicatorReport() : false);
        adhocTask.setStatus(AdhocTaskStatus.DRAFT);

        AdhocTask savedTask = adhocTaskRepository.save(adhocTask);

        // Handle scope type specific logic
        handleScopeType(savedTask, request.getScopeType(), request.getTargetOrgIds(), 
                request.getTargetIndicatorIds(), creatorOrg.getId());

        log.info("Created adhoc task: {} with scope type: {}", savedTask.getAdhocTaskId(), request.getScopeType());

        return toAdhocTaskVO(savedTask, true);
    }

    /**
     * Handle scope type specific logic for target organizations and indicators
     * 
     * @param adhocTask the adhoc task
     * @param scopeType scope type
     * @param targetOrgIds target organization IDs (for CUSTOM)
     * @param targetIndicatorIds target indicator IDs (for CUSTOM)
     * @param creatorOrgId creator organization ID
     */
    private void handleScopeType(AdhocTask adhocTask, AdhocScopeType scopeType, 
                                  List<Long> targetOrgIds, List<Long> targetIndicatorIds, Long creatorOrgId) {
        switch (scopeType) {
            case ALL_ORGS:
                // Requirements: 10.2 - Distribute to all designated organizations
                populateAllOrgsTargets(adhocTask);
                break;
            case BY_DEPT_ISSUED_INDICATORS:
                // Requirements: 10.3 - Auto-extract secondary indicators issued by the department
                populateByDeptIssuedIndicators(adhocTask, creatorOrgId);
                break;
            case CUSTOM:
                // Requirements: 10.4 - Allow manual selection of target organizations or indicators
                populateCustomTargets(adhocTask, targetOrgIds, targetIndicatorIds);
                break;
            default:
                log.warn("Unknown scope type: {}", scopeType);
        }
    }


    /**
     * Populate adhoc_task_target table with all active organizations
     * Requirements: 10.2 - ALL_ORGS: distribute to all designated organizations
     * 
     * @param adhocTask the adhoc task
     */
    private void populateAllOrgsTargets(AdhocTask adhocTask) {
        List<SysOrg> allActiveOrgs = orgRepository.findByIsActiveTrue();
        
        for (SysOrg org : allActiveOrgs) {
            AdhocTaskTarget target = new AdhocTaskTarget();
            target.setAdhocTask(adhocTask);
            target.setTargetOrg(org);
            adhocTaskTargetRepository.save(target);
        }
        
        log.debug("Populated {} target organizations for adhoc task {}", 
                allActiveOrgs.size(), adhocTask.getAdhocTaskId());
    }

    /**
     * Populate adhoc_task_indicator_map with indicators issued by the creator department
     * Requirements: 10.3 - BY_DEPT_ISSUED_INDICATORS: auto-extract secondary indicators
     * 
     * @param adhocTask the adhoc task
     * @param creatorOrgId creator organization ID
     */
    private void populateByDeptIssuedIndicators(AdhocTask adhocTask, Long creatorOrgId) {
        // Find all active indicators where owner_org_id matches creator_org_id
        // TODO: 临时注释 - 需要重新实现
        // Focus on secondary level indicators (FUNC_TO_COLLEGE)
        // List<Indicator> issuedIndicators = indicatorRepository
        //         .findByOwnerOrgAndStatus(creatorOrgId, IndicatorStatus.ACTIVE);
        // 
        // // Filter to secondary level indicators if needed
        // List<Indicator> secondaryIndicators = issuedIndicators.stream()
        //         .filter(ind -> ind.getLevel() == IndicatorLevel.FUNC_TO_COLLEGE)
        //         .collect(Collectors.toList());
        // 
        // // If no secondary indicators, use all issued indicators
        List<Indicator> secondaryIndicators = List.of();
        // List<Indicator> indicatorsToMap = secondaryIndicators.isEmpty() ? issuedIndicators : secondaryIndicators;
        List<Indicator> indicatorsToMap = secondaryIndicators;
        
        for (Indicator indicator : indicatorsToMap) {
            AdhocTaskIndicatorMap mapping = new AdhocTaskIndicatorMap();
            mapping.setAdhocTask(adhocTask);
            mapping.setIndicator(indicator);
            adhocTaskIndicatorMapRepository.save(mapping);
        }
        
        log.debug("Populated {} indicator mappings for adhoc task {} (BY_DEPT_ISSUED_INDICATORS)", 
                indicatorsToMap.size(), adhocTask.getAdhocTaskId());
    }

    /**
     * Populate custom targets based on user selection
     * Requirements: 10.4 - CUSTOM: allow manual selection
     * 
     * @param adhocTask the adhoc task
     * @param targetOrgIds target organization IDs
     * @param targetIndicatorIds target indicator IDs
     */
    private void populateCustomTargets(AdhocTask adhocTask, List<Long> targetOrgIds, List<Long> targetIndicatorIds) {
        // Populate target organizations
        if (targetOrgIds != null && !targetOrgIds.isEmpty()) {
            for (Long orgId : targetOrgIds) {
                SysOrg org = orgRepository.findById(orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Target Organization", orgId));
                
                AdhocTaskTarget target = new AdhocTaskTarget();
                target.setAdhocTask(adhocTask);
                target.setTargetOrg(org);
                adhocTaskTargetRepository.save(target);
            }
            log.debug("Populated {} custom target organizations for adhoc task {}", 
                    targetOrgIds.size(), adhocTask.getAdhocTaskId());
        }
        
        // Populate target indicators
        if (targetIndicatorIds != null && !targetIndicatorIds.isEmpty()) {
            for (Long indicatorId : targetIndicatorIds) {
                Indicator indicator = indicatorRepository.findById(indicatorId)
                        .orElseThrow(() -> new ResourceNotFoundException("Target Indicator", indicatorId));
                
                AdhocTaskIndicatorMap mapping = new AdhocTaskIndicatorMap();
                mapping.setAdhocTask(adhocTask);
                mapping.setIndicator(indicator);
                adhocTaskIndicatorMapRepository.save(mapping);
            }
            log.debug("Populated {} custom indicator mappings for adhoc task {}", 
                    targetIndicatorIds.size(), adhocTask.getAdhocTaskId());
        }
    }


    /**
     * Update an existing adhoc task
     * Requirements: 10.4, 10.5 - Update task and status
     * 
     * @param adhocTaskId adhoc task ID
     * @param request adhoc task update request
     * @return updated adhoc task VO
     */
    @Transactional
    public AdhocTaskVO updateAdhocTask(Long adhocTaskId, AdhocTaskUpdateRequest request) {
        AdhocTask adhocTask = findAdhocTaskById(adhocTaskId);

        // Check if task can be updated (not closed or archived)
        if (adhocTask.getStatus() == AdhocTaskStatus.CLOSED || 
            adhocTask.getStatus() == AdhocTaskStatus.ARCHIVED) {
            throw new BusinessException("Cannot update closed or archived adhoc task");
        }

        // Update fields if provided
        if (request.getIndicatorId() != null) {
            Indicator indicator = indicatorRepository.findById(request.getIndicatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Indicator", request.getIndicatorId()));
            adhocTask.setIndicator(indicator);
        }
        if (request.getTaskTitle() != null) {
            adhocTask.setTaskTitle(request.getTaskTitle());
        }
        if (request.getTaskDesc() != null) {
            adhocTask.setTaskDesc(request.getTaskDesc());
        }
        if (request.getOpenAt() != null) {
            adhocTask.setOpenAt(request.getOpenAt());
        }
        if (request.getDueAt() != null) {
            adhocTask.setDueAt(request.getDueAt());
        }
        if (request.getIncludeInAlert() != null) {
            adhocTask.setIncludeInAlert(request.getIncludeInAlert());
        }
        if (request.getRequireIndicatorReport() != null) {
            adhocTask.setRequireIndicatorReport(request.getRequireIndicatorReport());
        }
        if (request.getStatus() != null) {
            adhocTask.setStatus(request.getStatus());
        }

        // Handle scope type change
        if (request.getScopeType() != null && request.getScopeType() != adhocTask.getScopeType()) {
            // Clear existing targets and mappings
            clearTargetsAndMappings(adhocTaskId);
            
            adhocTask.setScopeType(request.getScopeType());
            
            // Re-populate based on new scope type
            handleScopeType(adhocTask, request.getScopeType(), request.getTargetOrgIds(), 
                    request.getTargetIndicatorIds(), adhocTask.getCreatorOrg().getId());
        } else if (request.getScopeType() == AdhocScopeType.CUSTOM) {
            // Update custom targets if scope type is CUSTOM
            if (request.getTargetOrgIds() != null || request.getTargetIndicatorIds() != null) {
                clearTargetsAndMappings(adhocTaskId);
                populateCustomTargets(adhocTask, request.getTargetOrgIds(), request.getTargetIndicatorIds());
            }
        }

        AdhocTask updatedTask = adhocTaskRepository.save(adhocTask);
        log.info("Updated adhoc task: {}", updatedTask.getAdhocTaskId());

        return toAdhocTaskVO(updatedTask, true);
    }

    /**
     * Clear existing targets and indicator mappings for an adhoc task
     * 
     * @param adhocTaskId adhoc task ID
     */
    @Transactional
    private void clearTargetsAndMappings(Long adhocTaskId) {
        adhocTaskTargetRepository.deleteByAdhocTask_AdhocTaskId(adhocTaskId);
        adhocTaskIndicatorMapRepository.deleteByAdhocTask_AdhocTaskId(adhocTaskId);
        log.debug("Cleared targets and mappings for adhoc task {}", adhocTaskId);
    }


    /**
     * Update adhoc task status
     * Requirements: 10.5 - Status update
     * 
     * @param adhocTaskId adhoc task ID
     * @param status new status
     * @return updated adhoc task VO
     */
    @Transactional
    public AdhocTaskVO updateAdhocTaskStatus(Long adhocTaskId, AdhocTaskStatus status) {
        AdhocTask adhocTask = findAdhocTaskById(adhocTaskId);
        
        // Validate status transition
        validateStatusTransition(adhocTask.getStatus(), status);
        
        adhocTask.setStatus(status);
        AdhocTask updatedTask = adhocTaskRepository.save(adhocTask);
        
        log.info("Updated adhoc task {} status from {} to {}", 
                adhocTaskId, adhocTask.getStatus(), status);
        
        return toAdhocTaskVO(updatedTask, false);
    }

    /**
     * Validate status transition
     * 
     * @param currentStatus current status
     * @param newStatus new status
     */
    private void validateStatusTransition(AdhocTaskStatus currentStatus, AdhocTaskStatus newStatus) {
        // Define valid transitions based on PostgreSQL enum values
        // DRAFT -> OPEN, ARCHIVED
        // OPEN -> CLOSED, ARCHIVED
        // CLOSED -> ARCHIVED
        // ARCHIVED -> (terminal state)
        switch (currentStatus) {
            case DRAFT:
                if (newStatus != AdhocTaskStatus.OPEN && newStatus != AdhocTaskStatus.ARCHIVED) {
                    throw new BusinessException("Invalid status transition from DRAFT to " + newStatus);
                }
                break;
            case OPEN:
                if (newStatus != AdhocTaskStatus.CLOSED && newStatus != AdhocTaskStatus.ARCHIVED) {
                    throw new BusinessException("Invalid status transition from OPEN to " + newStatus);
                }
                break;
            case CLOSED:
                if (newStatus != AdhocTaskStatus.ARCHIVED) {
                    throw new BusinessException("Invalid status transition from CLOSED to " + newStatus);
                }
                break;
            case ARCHIVED:
                throw new BusinessException("Cannot change status of archived task");
            default:
                throw new BusinessException("Unknown status: " + currentStatus);
        }
    }

    /**
     * Open an adhoc task (DRAFT -> OPEN)
     * 
     * @param adhocTaskId adhoc task ID
     * @return updated adhoc task VO
     */
    @Transactional
    public AdhocTaskVO openAdhocTask(Long adhocTaskId) {
        return updateAdhocTaskStatus(adhocTaskId, AdhocTaskStatus.OPEN);
    }

    /**
     * Close an adhoc task (OPEN -> CLOSED)
     * 
     * @param adhocTaskId adhoc task ID
     * @return updated adhoc task VO
     */
    @Transactional
    public AdhocTaskVO closeAdhocTask(Long adhocTaskId) {
        return updateAdhocTaskStatus(adhocTaskId, AdhocTaskStatus.CLOSED);
    }

    /**
     * Archive an adhoc task (DRAFT/OPEN/CLOSED -> ARCHIVED)
     * 
     * @param adhocTaskId adhoc task ID
     * @return updated adhoc task VO
     */
    @Transactional
    public AdhocTaskVO archiveAdhocTask(Long adhocTaskId) {
        return updateAdhocTaskStatus(adhocTaskId, AdhocTaskStatus.ARCHIVED);
    }

    /**
     * Delete an adhoc task (only DRAFT tasks can be deleted)
     * 
     * @param adhocTaskId adhoc task ID
     */
    @Transactional
    public void deleteAdhocTask(Long adhocTaskId) {
        AdhocTask adhocTask = findAdhocTaskById(adhocTaskId);
        
        if (adhocTask.getStatus() != AdhocTaskStatus.DRAFT) {
            throw new BusinessException("Only draft adhoc tasks can be deleted");
        }
        
        // Clear targets and mappings first
        clearTargetsAndMappings(adhocTaskId);
        
        // Delete the task
        adhocTaskRepository.delete(adhocTask);
        log.info("Deleted adhoc task: {}", adhocTaskId);
    }


    /**
     * Get adhoc tasks that include in alert calculation
     * Requirements: 10.5 - Include in alert calculations
     * 
     * @return list of adhoc tasks included in alerts
     */
    public List<AdhocTaskVO> getAdhocTasksIncludedInAlert() {
        return adhocTaskRepository.findByIncludeInAlertTrue().stream()
                .map(task -> toAdhocTaskVO(task, false))
                .collect(Collectors.toList());
    }

    /**
     * Get adhoc tasks by cycle ID that include in alert
     * 
     * @param cycleId assessment cycle ID
     * @return list of adhoc tasks
     */
    public List<AdhocTaskVO> getAdhocTasksIncludedInAlertByCycleId(Long cycleId) {
        return adhocTaskRepository.findByCycle_CycleIdAndIncludeInAlertTrue(cycleId).stream()
                .map(task -> toAdhocTaskVO(task, false))
                .collect(Collectors.toList());
    }

    /**
     * Get target organizations for an adhoc task
     * 
     * @param adhocTaskId adhoc task ID
     * @return list of target organization VOs
     */
    public List<AdhocTaskVO.AdhocTaskTargetVO> getTargetOrganizations(Long adhocTaskId) {
        return adhocTaskTargetRepository.findByAdhocTask_AdhocTaskId(adhocTaskId).stream()
                .map(this::toTargetVO)
                .collect(Collectors.toList());
    }

    /**
     * Get mapped indicators for an adhoc task
     * 
     * @param adhocTaskId adhoc task ID
     * @return list of indicator mapping VOs
     */
    public List<AdhocTaskVO.AdhocTaskIndicatorVO> getMappedIndicators(Long adhocTaskId) {
        return adhocTaskIndicatorMapRepository.findByAdhocTask_AdhocTaskId(adhocTaskId).stream()
                .map(this::toIndicatorMappingVO)
                .collect(Collectors.toList());
    }

    /**
     * Find adhoc task entity by ID
     * 
     * @param adhocTaskId adhoc task ID
     * @return adhoc task entity
     * @throws ResourceNotFoundException if adhoc task not found
     */
    public AdhocTask findAdhocTaskById(Long adhocTaskId) {
        return adhocTaskRepository.findById(adhocTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Adhoc Task", adhocTaskId));
    }


    /**
     * Convert AdhocTask entity to AdhocTaskVO
     * 
     * @param adhocTask adhoc task entity
     * @param includeDetails whether to include targets and indicator mappings
     * @return adhoc task VO
     */
    private AdhocTaskVO toAdhocTaskVO(AdhocTask adhocTask, boolean includeDetails) {
        AdhocTaskVO vo = new AdhocTaskVO();
        vo.setAdhocTaskId(adhocTask.getAdhocTaskId());
        vo.setCycleId(adhocTask.getCycle().getCycleId());
        vo.setCycleName(adhocTask.getCycle().getCycleName());
        vo.setCreatorOrgId(adhocTask.getCreatorOrg().getId());
        vo.setCreatorOrgName(adhocTask.getCreatorOrg().getName());
        vo.setScopeType(adhocTask.getScopeType());
        
        if (adhocTask.getIndicator() != null) {
            vo.setIndicatorId(adhocTask.getIndicator().getIndicatorId());
            vo.setIndicatorDesc(adhocTask.getIndicator().getIndicatorDesc());
        }
        
        vo.setTaskTitle(adhocTask.getTaskTitle());
        vo.setTaskDesc(adhocTask.getTaskDesc());
        vo.setOpenAt(adhocTask.getOpenAt());
        vo.setDueAt(adhocTask.getDueAt());
        vo.setIncludeInAlert(adhocTask.getIncludeInAlert());
        vo.setRequireIndicatorReport(adhocTask.getRequireIndicatorReport());
        vo.setStatus(adhocTask.getStatus());
        vo.setCreatedAt(adhocTask.getCreatedAt());
        vo.setUpdatedAt(adhocTask.getUpdatedAt());

        if (includeDetails) {
            // Load targets
            List<AdhocTaskTarget> targets = adhocTaskTargetRepository
                    .findByAdhocTask_AdhocTaskId(adhocTask.getAdhocTaskId());
            vo.setTargets(targets.stream()
                    .map(this::toTargetVO)
                    .collect(Collectors.toList()));

            // Load indicator mappings
            List<AdhocTaskIndicatorMap> mappings = adhocTaskIndicatorMapRepository
                    .findByAdhocTask_AdhocTaskId(adhocTask.getAdhocTaskId());
            vo.setIndicators(mappings.stream()
                    .map(this::toIndicatorMappingVO)
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    /**
     * Convert AdhocTaskTarget to AdhocTaskTargetVO
     */
    private AdhocTaskVO.AdhocTaskTargetVO toTargetVO(AdhocTaskTarget target) {
        AdhocTaskVO.AdhocTaskTargetVO vo = new AdhocTaskVO.AdhocTaskTargetVO();
        vo.setOrgId(target.getTargetOrg().getId());
        vo.setOrgName(target.getTargetOrg().getName());
        return vo;
    }

    /**
     * Convert AdhocTaskIndicatorMap to AdhocTaskIndicatorVO
     */
    private AdhocTaskVO.AdhocTaskIndicatorVO toIndicatorMappingVO(AdhocTaskIndicatorMap mapping) {
        AdhocTaskVO.AdhocTaskIndicatorVO vo = new AdhocTaskVO.AdhocTaskIndicatorVO();
        vo.setIndicatorId(mapping.getIndicator().getIndicatorId());
        vo.setIndicatorDesc(mapping.getIndicator().getIndicatorDesc());
        vo.setOwnerOrgId(mapping.getIndicator().getOwnerOrg().getId());
        vo.setOwnerOrgName(mapping.getIndicator().getOwnerOrg().getName());
        return vo;
    }
}
