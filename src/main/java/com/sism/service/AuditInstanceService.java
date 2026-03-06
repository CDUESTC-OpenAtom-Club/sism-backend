package com.sism.service;

import com.sism.entity.*;
import com.sism.enums.AuditEntityType;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing audit workflow instances
 * Handles instance creation, step progression, and approval tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditInstanceService {

    private final AuditInstanceRepository auditInstanceRepository;
    private final AuditFlowDefRepository auditFlowDefRepository;
    private final AuditStepDefRepository auditStepDefRepository;
    private final ApprovalResolverService approvalResolverService;
    private final UserRepository userRepository;
    private final IndicatorRepository indicatorRepository;

    /**
     * Create a new audit instance and start the approval workflow
     * 
     * @param flowCode The flow code (e.g., "PLAN_DISPATCH_STRATEGY")
     * @param entityType The entity type (e.g., PLAN)
     * @param entityId The entity ID
     * @param submitterId The user who initiated the workflow
     * @return The created audit instance
     */
    
        /**
         * Create a new audit instance and start the approval workflow
         * 
         * @param flowCode The flow code (e.g., "INDICATOR_DEFAULT_APPROVAL")
         * @param entityType The entity type (e.g., INDICATOR)
         * @param entityId The entity ID
         * @param submitterId The user who initiated the workflow
         * @return The created audit instance
         */
        @Transactional
        public AuditInstance createAuditInstance(String flowCode, AuditEntityType entityType, 
                                                Long entityId, Long submitterId) {
            log.info("Creating audit instance: flowCode={}, entityType={}, entityId={}, submitterId={}", 
                    flowCode, entityType, entityId, submitterId);

            // Check if there's already an active instance
            Optional<AuditInstance> existingInstance = auditInstanceRepository
                    .findActiveInstanceByEntity(entityType, entityId);

            if (existingInstance.isPresent()) {
                throw new BusinessException("An active approval workflow already exists for this entity");
            }

            // Find the flow definition
            AuditFlowDef flowDef = auditFlowDefRepository.findByFlowCode(flowCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Audit flow not found: " + flowCode));

            // Get submitter information
            SysUser submitter = userRepository.findById(submitterId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", submitterId));

            Long submitterDeptId = submitter.getOrg() != null ? submitter.getOrg().getId() : null;
            if (submitterDeptId == null) {
                throw new BusinessException("Submitter must belong to an organization");
            }

            // Resolve approval context based on entity type
            Long superiorDeptId = null;
            boolean isCollege = false;

            if (entityType == AuditEntityType.INDICATOR) {
                // For indicators, get superior_dept_id from indicator.owner_org_id
                // and determine if it's a college based on responsible_dept
                try {
                    com.sism.entity.Indicator indicator = indicatorRepository.findById(entityId)
                            .orElseThrow(() -> new ResourceNotFoundException("Indicator", entityId));

                    superiorDeptId = indicator.getOwnerOrg() != null ? indicator.getOwnerOrg().getId() : null;
                    String responsibleDept = indicator.getResponsibleDept();
                    isCollege = responsibleDept != null && responsibleDept.contains("学院");

                    log.info("Indicator context: superiorDeptId={}, responsibleDept={}, isCollege={}", 
                            superiorDeptId, responsibleDept, isCollege);
                } catch (Exception e) {
                    log.error("Failed to resolve indicator context: {}", e.getMessage(), e);
                    throw new BusinessException("Failed to resolve approval context for indicator");
                }
            }

            // Resolve approvers using role-based logic
            ApprovalResolverService.ApprovalContext approvalContext = approvalResolverService
                    .resolveApprovalContext(submitterId, submitterDeptId, superiorDeptId, isCollege);

            // Validate that we have at least the direct supervisor
            if (approvalContext.getDirectSupervisorId() == null) {
                String roleType = isCollege ? "学院院长" : "部门负责人";
                throw new BusinessException("无法找到" + roleType + "审批人，请联系管理员配置");
            }

            // Validate that we have level-2 supervisor
            if (approvalContext.getLevel2SupervisorId() == null) {
                throw new BusinessException("无法找到分管校领导审批人，请联系管理员配置");
            }

            // Create the audit instance with multi-level approval fields
            AuditInstance instance = new AuditInstance();
            instance.setFlowId(flowDef.getId());
            instance.setEntityType(entityType);
            instance.setEntityId(entityId);
            instance.setStatus("PENDING");
            instance.setInitiatedBy(submitterId);
            instance.setInitiatedAt(LocalDateTime.now());

            // Set multi-level approval fields
            instance.setCurrentStepOrder(1); // Start at level 1
            instance.setSubmitterDeptId(submitterDeptId);
            instance.setDirectSupervisorId(approvalContext.getDirectSupervisorId());
            instance.setLevel2SupervisorId(approvalContext.getLevel2SupervisorId());
            instance.setSuperiorDeptId(superiorDeptId);

            // Initialize approval tracking lists
            List<Long> pendingApprovers = new ArrayList<>();
            pendingApprovers.add(approvalContext.getDirectSupervisorId()); // Start with level 1
            instance.setPendingApprovers(pendingApprovers);
            instance.setApprovedApprovers(new ArrayList<>());
            instance.setRejectedApprovers(new ArrayList<>());

            AuditInstance savedInstance = auditInstanceRepository.save(instance);
            log.info("Created audit instance with ID: {}, currentStep: 1, directSupervisor: {}, level2Supervisor: {}", 
                    savedInstance.getId(), approvalContext.getDirectSupervisorId(), approvalContext.getLevel2SupervisorId());

            return savedInstance;
        }


    /**
     * Approve the current step and progress to next step
     * 
     * @param instanceId The audit instance ID
     * @param approverId The approver's user ID
     * @param comment Optional approval comment
     * @return The updated audit instance
     */
    @Transactional
    public AuditInstance approve(Long instanceId, Long approverId, String comment) {
        log.info("Processing approval: instanceId={}, approverId={}, comment={}", 
                instanceId, approverId, comment);

        AuditInstance instance = auditInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit instance", instanceId));

        // Validate instance is still pending
        if (!instance.isPending()) {
            throw new BusinessException("Audit instance is not in pending status");
        }

        // Validate approver is authorized for current step
        Integer currentStep = instance.getCurrentStepOrder();
        if (currentStep == null) {
            currentStep = 1; // Default to step 1 if not set
        }
        
        Long expectedApproverId = null;
        if (currentStep == 1) {
            expectedApproverId = instance.getDirectSupervisorId();
        } else if (currentStep == 2) {
            expectedApproverId = instance.getLevel2SupervisorId();
        }
        
        if (expectedApproverId == null || !expectedApproverId.equals(approverId)) {
            throw new BusinessException("You are not authorized to approve at this step");
        }

        // Record the approval
        List<Long> approvedList = instance.getApprovedApprovers();
        if (approvedList == null) {
            approvedList = new ArrayList<>();
        }
        if (!approvedList.contains(approverId)) {
            approvedList.add(approverId);
            instance.setApprovedApprovers(approvedList);
        }
        
        // Remove from pending list
        List<Long> pendingList = instance.getPendingApprovers();
        if (pendingList != null) {
            pendingList.remove(approverId);
            instance.setPendingApprovers(pendingList);
        }

        // Progress to next step or complete
        if (currentStep == 1) {
            // Move to level 2 approval
            instance.setCurrentStepOrder(2);
            instance.setStatus("LEVEL2_PENDING");
            
            // Add level 2 approver to pending list
            if (pendingList == null) {
                pendingList = new ArrayList<>();
            }
            if (instance.getLevel2SupervisorId() != null) {
                pendingList.add(instance.getLevel2SupervisorId());
                instance.setPendingApprovers(pendingList);
            }
            
            log.info("Progressed to level 2 approval for instance: {}", instanceId);
        } else if (currentStep == 2) {
            // Complete the approval workflow
            completeApproval(instance);
        }

        AuditInstance updatedInstance = auditInstanceRepository.save(instance);
        log.info("Approval processed successfully for instance: {}, new step: {}", 
                instanceId, updatedInstance.getCurrentStepOrder());

        return updatedInstance;
    }

    /**
     * Reject the current step and terminate the workflow
     * NOTE: Simplified version - multi-level approval not supported by database schema
     * 
     * @param instanceId The audit instance ID
     * @param approverId The approver's user ID
     * @param comment Rejection reason (required)
     * @return The updated audit instance
     */
    @Transactional
    public AuditInstance reject(Long instanceId, Long approverId, String comment) {
        log.info("Processing rejection: instanceId={}, approverId={}, comment={}", 
                instanceId, approverId, comment);

        if (comment == null || comment.trim().isEmpty()) {
            throw new BusinessException("Rejection comment is required");
        }

        AuditInstance instance = auditInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit instance", instanceId));

        // Validate instance is still pending
        if (!instance.isPending()) {
            throw new BusinessException("Audit instance is not in pending status");
        }

        // Record the rejection
        List<Long> rejectedList = instance.getRejectedApprovers();
        if (rejectedList == null) {
            rejectedList = new ArrayList<>();
        }
        if (!rejectedList.contains(approverId)) {
            rejectedList.add(approverId);
            instance.setRejectedApprovers(rejectedList);
        }

        // Terminate the workflow
        instance.setStatus("REJECTED");
        instance.setCompletedAt(LocalDateTime.now());

        AuditInstance updatedInstance = auditInstanceRepository.save(instance);
        log.info("Rejection processed successfully for instance: {}", instanceId);

        return updatedInstance;
    }

    /**
     * Get audit instance by ID
     */
    public AuditInstance getAuditInstanceById(Long instanceId) {
        return auditInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit instance", instanceId));
    }

    /**
     * Get active audit instance for an entity
     */
    public Optional<AuditInstance> getActiveInstanceByEntity(AuditEntityType entityType, Long entityId) {
        return auditInstanceRepository.findActiveInstanceByEntity(entityType, entityId);
    }

    /**
     * Get all audit instances for an entity
     */
    public List<AuditInstance> getInstancesByEntity(AuditEntityType entityType, Long entityId) {
        return auditInstanceRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get pending approvals for a user
     */
    public List<AuditInstance> getPendingApprovalsForUser(Long userId) {
        return auditInstanceRepository.findPendingApprovalsForUser(userId);
    }

    /**
     * Count pending approvals for a user
     */
    public long countPendingApprovalsForUser(Long userId) {
        return auditInstanceRepository.countPendingApprovalsForUser(userId);
    }

    /**
     * Get current step description for an instance
     */
    public String getCurrentStepDescription(AuditInstance instance) {
        if (instance.isCompleted()) {
            return instance.isRejected() ? "已驳回" : "已完成";
        }
        
        Integer currentStep = instance.getCurrentStepOrder();
        if (currentStep == null) {
            return "审批中";
        }
        
        return switch (currentStep) {
            case 1 -> "一级审批中（部门负责人）";
            case 2 -> "二级审批中（分管校领导）";
            default -> "审批中";
        };
    }

    // ==================== Private Helper Methods ====================

    /**
     * Complete the approval workflow
     */
    private void completeApproval(AuditInstance instance) {
        instance.setStatus("APPROVED");
        instance.setCompletedAt(LocalDateTime.now());
        log.info("Approval workflow completed for instance: {}", instance.getId());
    }
}
