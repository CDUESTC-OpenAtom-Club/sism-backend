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

    /**
     * Create a new audit instance and start the approval workflow
     * 
     * @param flowCode The flow code (e.g., "PLAN_DISPATCH_STRATEGY")
     * @param entityType The entity type (e.g., PLAN)
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

        // Resolve approval context (supervisors)
        ApprovalResolverService.ApprovalContext context = 
                approvalResolverService.resolveApprovalContext(submitterId, submitterDeptId);

        log.debug("Resolved approval context: {}", context);

        // Validate that required approvers exist
        if (context.getDirectSupervisorId() == null) {
            throw new BusinessException("Direct supervisor not found for user: " + submitterId);
        }

        // Create the audit instance
        AuditInstance instance = new AuditInstance();
        instance.setFlowId(flowDef.getId());
        instance.setEntityType(entityType);
        instance.setEntityId(entityId);
        instance.setStatus("IN_PROGRESS");
        instance.setInitiatedBy(submitterId);
        instance.setInitiatedAt(LocalDateTime.now());
        
        // Set approval chain
        instance.setCurrentStepOrder(1); // Start at step 1 (direct supervisor)
        instance.setSubmitterDeptId(submitterDeptId);
        instance.setDirectSupervisorId(context.getDirectSupervisorId());
        instance.setLevel2SupervisorId(context.getLevel2SupervisorId());
        instance.setSuperiorDeptId(context.getSuperiorDeptId());
        
        // Initialize approval tracking lists
        instance.setPendingApprovers(new ArrayList<>());
        instance.setApprovedApprovers(new ArrayList<>());
        instance.setRejectedApprovers(new ArrayList<>());

        AuditInstance savedInstance = auditInstanceRepository.save(instance);
        log.info("Created audit instance with ID: {}", savedInstance.getId());

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
        if (!approvalResolverService.isAuthorizedApprover(instance, approverId)) {
            throw new BusinessException("User is not authorized to approve at this step");
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

        // Progress to next step or complete
        Integer currentStep = instance.getCurrentStepOrder();
        
        if (currentStep == 1) {
            // Move to level-2 supervisor
            if (instance.getLevel2SupervisorId() != null) {
                instance.setCurrentStepOrder(2);
                log.info("Progressed to step 2 (level-2 supervisor)");
            } else {
                // No level-2 supervisor, complete approval
                completeApproval(instance);
            }
        } else if (currentStep == 2) {
            // Check if there's a step 3 (superior department)
            if (instance.getSuperiorDeptId() != null) {
                instance.setCurrentStepOrder(3);
                // Resolve superior department approvers
                List<Long> superiorApprovers = approvalResolverService
                        .resolveSuperiorDeptApprovers(instance.getSubmitterDeptId(), "functional_dept");
                instance.setPendingApprovers(superiorApprovers);
                log.info("Progressed to step 3 (superior department), pending approvers: {}", superiorApprovers);
            } else {
                // No step 3, complete approval
                completeApproval(instance);
            }
        } else if (currentStep == 3) {
            // Final step - complete approval
            completeApproval(instance);
        }

        AuditInstance updatedInstance = auditInstanceRepository.save(instance);
        log.info("Approval processed successfully for instance: {}", instanceId);

        return updatedInstance;
    }

    /**
     * Reject the current step and terminate the workflow
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

        // Validate approver is authorized for current step
        if (!approvalResolverService.isAuthorizedApprover(instance, approverId)) {
            throw new BusinessException("User is not authorized to reject at this step");
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
        return approvalResolverService.getStepName(instance.getCurrentStepOrder());
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
