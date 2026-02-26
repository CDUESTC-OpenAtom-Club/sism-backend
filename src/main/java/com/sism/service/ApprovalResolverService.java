package com.sism.service;

import com.sism.entity.SysOrgHierarchy;
import com.sism.entity.SysUser;
import com.sism.entity.SysUserSupervisor;
import com.sism.repository.SysOrgHierarchyRepository;
import com.sism.repository.SysUserSupervisorRepository;
import com.sism.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for resolving approvers based on submitter information
 * 
 * This service handles the dynamic resolution of approvers for the multi-level approval flow:
 * 1. Direct supervisor (level 1) - from sys_user_supervisor
 * 2. Level-2 supervisor - from sys_user_supervisor
 * 3. Superior department approvers - from sys_org_hierarchy + user roles
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalResolverService {

    private final SysUserSupervisorRepository supervisorRepository;
    private final SysOrgHierarchyRepository orgHierarchyRepository;
    private final UserRepository userRepository;

    /**
     * Resolve the direct supervisor (level 1) for a user
     * 
     * @param userId The user ID
     * @return The supervisor user ID, or empty if not found
     */
    public Optional<Long> resolveDirectSupervisor(Long userId) {
        log.debug("Resolving direct supervisor for user: {}", userId);
        
        return supervisorRepository.findByUserIdAndLevel(userId, 1)
                .map(SysUserSupervisor::getSupervisorId);
    }

    /**
     * Resolve the level-2 supervisor for a user
     * 
     * @param userId The user ID
     * @return The level-2 supervisor user ID, or empty if not found
     */
    public Optional<Long> resolveLevel2Supervisor(Long userId) {
        log.debug("Resolving level-2 supervisor for user: {}", userId);
        
        return supervisorRepository.findByUserIdAndLevel(userId, 2)
                .map(SysUserSupervisor::getSupervisorId);
    }

    /**
     * Resolve all supervisors for a user (both level 1 and level 2)
     * 
     * @param userId The user ID
     * @return List of supervisor user IDs
     */
    public List<Long> resolveAllSupervisors(Long userId) {
        log.debug("Resolving all supervisors for user: {}", userId);
        
        List<Long> supervisors = new ArrayList<>();
        
        supervisorRepository.findByUserId(userId).forEach(rel -> {
            supervisors.add(rel.getSupervisorId());
        });
        
        return supervisors;
    }

    /**
     * Resolve the superior department ID for a given department
     * 
     * @param deptId The department ID
     * @return The superior department ID, or empty if not found
     */
    public Optional<Long> resolveSuperiorDeptId(Long deptId) {
        log.debug("Resolving superior department for dept: {}", deptId);
        
        return orgHierarchyRepository.findByOrgId(deptId)
                .map(SysOrgHierarchy::getParentOrgId);
    }

    /**
     * Resolve approvers from the superior department
     * 
     * @param deptId The submitter's department ID
     * @param roleCode The role code to filter approvers (e.g., "functional_dept")
     * @return List of approver user IDs from the superior department
     */
    public List<Long> resolveSuperiorDeptApprovers(Long deptId, String roleCode) {
        log.debug("Resolving superior department approvers for dept: {}, role: {}", deptId, roleCode);
        
        // First get the superior department
        Optional<Long> superiorDeptId = resolveSuperiorDeptId(deptId);
        
        if (superiorDeptId.isEmpty()) {
            log.warn("No superior department found for dept: {}", deptId);
            return List.of();
        }
        
        // Find all users in the superior department with the specified role
        // This would require joining with user_roles table
        // For now, we'll return all active users in that department
        List<SysUser> users = userRepository.findByOrg_IdAndIsActiveTrue(superiorDeptId.get());
        
        return users.stream()
                .map(SysUser::getId)
                .toList();
    }

    /**
     * Resolve all approvers for a new approval instance
     * 
     * @param submitterId The submitter's user ID
     * @param submitterDeptId The submitter's department ID
     * @return ApprovalContext containing all resolved approver information
     */
    public ApprovalContext resolveApprovalContext(Long submitterId, Long submitterDeptId) {
        log.info("Resolving approval context for user: {}, dept: {}", submitterId, submitterDeptId);
        
        ApprovalContext context = new ApprovalContext();
        context.setSubmitterId(submitterId);
        context.setSubmitterDeptId(submitterDeptId);
        
        // Resolve direct supervisor
        resolveDirectSupervisor(submitterId).ifPresent(context::setDirectSupervisorId);
        
        // Resolve level-2 supervisor
        resolveLevel2Supervisor(submitterId).ifPresent(context::setLevel2SupervisorId);
        
        // Resolve superior department
        Optional<Long> superiorDeptId = resolveSuperiorDeptId(submitterDeptId);
        superiorDeptId.ifPresent(context::setSuperiorDeptId);
        
        return context;
    }

    /**
     * Check if a user is authorized to approve at the current step
     * 
     * @param instance The audit instance
     * @param approverId The potential approver's user ID
     * @return true if authorized, false otherwise
     */
    public boolean isAuthorizedApprover(com.sism.entity.AuditInstance instance, Long approverId) {
        Integer currentStep = instance.getCurrentStepOrder();
        
        if (currentStep == null || currentStep < 1 || currentStep > 3) {
            return false;
        }
        
        return switch (currentStep) {
            case 1 -> instance.getDirectSupervisorId() != null 
                    && instance.getDirectSupervisorId().equals(approverId);
            case 2 -> instance.getLevel2SupervisorId() != null 
                    && instance.getLevel2SupervisorId().equals(approverId);
            case 3 -> instance.getPendingApprovers() != null 
                    && instance.getPendingApprovers().contains(approverId);
            default -> false;
        };
    }

    /**
     * Get the step name for a given step order
     */
    public String getStepName(Integer stepOrder) {
        return switch (stepOrder) {
            case 1 -> "直接主管审批";
            case 2 -> "二级主管审批";
            case 3 -> "上级部门审批";
            default -> "未知步骤";
        };
    }

    /**
     * Get the step description for a given step order
     */
    public String getStepDescription(Integer stepOrder) {
        return switch (stepOrder) {
            case 1 -> "提交人所在部门的直接主管审批";
            case 2 -> "提交人所在部门的二级主管审批";
            case 3 -> "上级主管部门的审批（会签）";
            default -> "";
        };
    }

    // ==================== Inner Class ====================

    /**
     * Context class to hold resolved approval information
     */
    public static class ApprovalContext {
        private Long submitterId;
        private Long submitterDeptId;
        private Long directSupervisorId;
        private Long level2SupervisorId;
        private Long superiorDeptId;
        private List<Long> superiorDeptApprovers = new ArrayList<>();

        // Getters and Setters
        public Long getSubmitterId() { return submitterId; }
        public void setSubmitterId(Long submitterId) { this.submitterId = submitterId; }
        
        public Long getSubmitterDeptId() { return submitterDeptId; }
        public void setSubmitterDeptId(Long submitterDeptId) { this.submitterDeptId = submitterDeptId; }
        
        public Long getDirectSupervisorId() { return directSupervisorId; }
        public void setDirectSupervisorId(Long directSupervisorId) { this.directSupervisorId = directSupervisorId; }
        
        public Long getLevel2SupervisorId() { return level2SupervisorId; }
        public void setLevel2SupervisorId(Long level2SupervisorId) { this.level2SupervisorId = level2SupervisorId; }
        
        public Long getSuperiorDeptId() { return superiorDeptId; }
        public void setSuperiorDeptId(Long superiorDeptId) { this.superiorDeptId = superiorDeptId; }
        
        public List<Long> getSuperiorDeptApprovers() { return superiorDeptApprovers; }
        public void setSuperiorDeptApprovers(List<Long> superiorDeptApprovers) { this.superiorDeptApprovers = superiorDeptApprovers; }

        @Override
        public String toString() {
            return "ApprovalContext{" +
                    "submitterId=" + submitterId +
                    ", submitterDeptId=" + submitterDeptId +
                    ", directSupervisorId=" + directSupervisorId +
                    ", level2SupervisorId=" + level2SupervisorId +
                    ", superiorDeptId=" + superiorDeptId +
                    ", superiorDeptApprovers=" + superiorDeptApprovers +
                    '}';
        }
    }
}
