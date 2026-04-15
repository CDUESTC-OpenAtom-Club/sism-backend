package com.sism.workflow.application.support;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.execution.application.ReportApplicationService;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.interfaces.dto.ApproverCandidateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class ApproverResolver {

    private static final String PLAN_ENTITY_TYPE = "PLAN";
    private static final String PLAN_REPORT_ENTITY_TYPE = "PLAN_REPORT";
    private static final String COLLEGE_FINAL_APPROVAL_STEP_NAME = "职能部门终审";

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final ReportApplicationService reportApplicationService;
    private final WorkflowApproverProperties workflowApproverProperties;

    public ApproverResolver(
            UserRepository userRepository,
            PlanRepository planRepository,
            ReportApplicationService reportApplicationService,
            WorkflowApproverProperties workflowApproverProperties
    ) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.reportApplicationService = reportApplicationService;
        this.workflowApproverProperties = workflowApproverProperties;
    }

    public Long resolveApproverId(AuditStepDef stepDef, Long requesterId, Long requesterOrgId) {
        return resolveApproverId(stepDef, requesterId, requesterOrgId, null);
    }

    public Long resolveApproverId(AuditStepDef stepDef, Long requesterId, Long requesterOrgId, AuditInstance instance) {
        Long roleId = stepDef.getRoleId();
        if (roleId == null || roleId <= 0) {
            throw new IllegalStateException("Workflow step is missing role assignment: " + stepDef.getStepName());
        }

        Long scopeOrgId = resolveScopeOrgId(stepDef, requesterOrgId, instance);
        return resolveByRoleId(roleId, scopeOrgId, stepDef.getStepName());
    }

    private Long resolveByRoleId(Long roleId, Long requesterOrgId, String stepName) {
        List<User> candidates = findScopedActiveUsersByRole(roleId, requesterOrgId);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No available approver candidates for step: " + stepName);
        }

        return candidates.stream()
                .map(User::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No available approver candidates for step: " + stepName));
    }

    public String resolveApproverName(Long userId) {
        if (userId == null || userId <= 0) {
            return "Unknown";
        }
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getRealName() != null && !user.getRealName().isBlank()) {
                        return user.getRealName();
                    }
                    return user.getUsername() != null ? user.getUsername() : "User-" + userId;
                })
                .orElse("User-" + userId);
    }

    public Long resolveApproverOrgId(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        return userRepository.findById(userId)
                .map(User::getOrgId)
                .orElse(null);
    }

    public Long resolveApproverOrgId(AuditStepDef stepDef, Long requesterOrgId, AuditInstance instance) {
        if (stepDef == null) {
            return requesterOrgId;
        }
        if (stepDef.isSubmitStep()) {
            return requesterOrgId;
        }

        Long scopeOrgId = resolveScopeOrgId(stepDef, requesterOrgId, instance);
        Long roleId = stepDef.getRoleId();
        if (getStrategyDeptHeadRoleId().equals(roleId)) {
            return getStrategyOrgId();
        }
        if (getVicePresidentRoleId().equals(roleId)) {
            return resolveVicePresidentScopeOrgId(scopeOrgId);
        }
        return scopeOrgId;
    }

    public List<ApproverCandidateResponse> resolveCandidates(AuditStepDef stepDef, Long requesterOrgId) {
        Long roleId = stepDef.getRoleId();
        if (roleId == null || roleId <= 0) {
            throw new IllegalStateException("Workflow step is missing role assignment: " + stepDef.getStepName());
        }

        List<ApproverCandidateResponse> candidates = findScopedActiveUsersByRole(roleId, requesterOrgId).stream()
                .sorted(Comparator.comparing(User::getId))
                .map(user -> ApproverCandidateResponse.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .orgId(user.getOrgId())
                        .build())
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No available approver candidates for step: " + stepDef.getStepName());
        }
        return candidates;
    }

    public boolean canUserApprove(AuditStepDef stepDef, Long userId, Long requesterOrgId) {
        return canUserApprove(stepDef, userId, requesterOrgId, null);
    }

    public boolean canUserApprove(AuditStepDef stepDef, Long userId, Long requesterOrgId, AuditInstance instance) {
        if (stepDef == null || stepDef.isSubmitStep() || userId == null || userId <= 0) {
            return false;
        }

        Long roleId = stepDef.getRoleId();
        if (roleId == null || roleId <= 0) {
            return false;
        }

        Long scopeOrgId = resolveScopeOrgId(stepDef, requesterOrgId, instance);
        List<Long> roleIds = userRepository.findRoleIdsByUserId(userId);

        return userRepository.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .filter(user -> roleIds.contains(roleId))
                .filter(user -> matchesRoleScope(user, roleId, scopeOrgId))
                .isPresent();
    }

    public void validateSelectedApprover(AuditStepDef stepDef, Long approverId) {
        validateSelectedApprover(stepDef, approverId, null);
    }

    public void validateSelectedApprover(AuditStepDef stepDef, Long approverId, Long requesterOrgId) {
        if (stepDef == null || stepDef.isSubmitStep()) {
            return;
        }
        if (approverId == null || approverId <= 0) {
            throw new IllegalArgumentException("Approver selection is required for step: " + stepDef.getStepName());
        }

        Long roleId = stepDef.getRoleId();
        if (roleId == null || roleId <= 0) {
            userRepository.findById(approverId)
                    .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid approver for step: " + stepDef.getStepName()));
            return;
        }

        boolean valid = findScopedActiveUsersByRole(roleId, requesterOrgId).stream()
                .anyMatch(user -> approverId.equals(user.getId()));
        if (!valid) {
            throw new IllegalArgumentException("Selected approver does not match step role: " + stepDef.getStepName());
        }
    }

    private List<User> findScopedActiveUsersByRole(Long roleId, Long requesterOrgId) {
        return userRepository.findByRoleId(roleId).stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .filter(user -> matchesRoleScope(user, roleId, requesterOrgId))
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    private Long resolveScopeOrgId(AuditStepDef stepDef, Long requesterOrgId, AuditInstance instance) {
        if (!isCollegeFinalApprovalStep(stepDef, instance)) {
            return requesterOrgId;
        }

        if (PLAN_ENTITY_TYPE.equalsIgnoreCase(instance.getEntityType())) {
            return planRepository.findById(instance.getEntityId())
                    .map(plan -> plan.getCreatedByOrgId() != null ? plan.getCreatedByOrgId() : requesterOrgId)
                    .orElse(requesterOrgId);
        }

        if (PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(instance.getEntityType())) {
            Long planId = reportApplicationService.findReportById(instance.getEntityId())
                    .map(report -> report.getPlanId())
                    .orElse(null);
            if (planId == null || planId <= 0) {
                return requesterOrgId;
            }

            return planRepository.findById(planId)
                    .map(plan -> plan.getCreatedByOrgId() != null ? plan.getCreatedByOrgId() : requesterOrgId)
                    .orElse(requesterOrgId);
        }

        return requesterOrgId;
    }

    private boolean isCollegeFinalApprovalStep(AuditStepDef stepDef, AuditInstance instance) {
        if (stepDef == null || instance == null) {
            return false;
        }
        if (!PLAN_ENTITY_TYPE.equalsIgnoreCase(instance.getEntityType())
                && !PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(instance.getEntityType())) {
            return false;
        }

        if (!getApproverRoleId().equals(stepDef.getRoleId())) {
            return false;
        }

        if (Boolean.TRUE.equals(stepDef.getIsTerminal())) {
            return true;
        }

        String stepName = stepDef.getStepName();
        boolean fallbackMatched = stepName != null && stepName.contains(COLLEGE_FINAL_APPROVAL_STEP_NAME);
        if (fallbackMatched) {
            log.warn("Using legacy step-name fallback for college final approval scope: stepName={}", stepName);
        }
        return fallbackMatched;
    }

    private boolean matchesRoleScope(User user, Long roleId, Long requesterOrgId) {
        Long userOrgId = user.getOrgId();

        if (getApproverRoleId().equals(roleId)) {
            return requesterOrgId != null && requesterOrgId.equals(userOrgId);
        }
        if (getStrategyDeptHeadRoleId().equals(roleId)) {
            return isStrategyOrg(userOrgId);
        }
        if (getVicePresidentRoleId().equals(roleId)) {
            Long scopeOrgId = resolveVicePresidentScopeOrgId(requesterOrgId);
            return scopeOrgId != null && scopeOrgId.equals(userOrgId);
        }

        return true;
    }

    private Long resolveVicePresidentScopeOrgId(Long requesterOrgId) {
        Map<Long, Long> scopeByOrg = workflowApproverProperties.getFunctionalVicePresidentScopeByOrg();
        if (scopeByOrg == null || scopeByOrg.isEmpty()) {
            return requesterOrgId;
        }
        return scopeByOrg.getOrDefault(requesterOrgId, requesterOrgId);
    }

    private boolean isStrategyOrg(Long orgId) {
        return orgId != null && orgId.equals(getStrategyOrgId());
    }

    private Long getApproverRoleId() {
        return requireConfigured(workflowApproverProperties.getApproverRoleId(), "workflow.approver.approver-role-id");
    }

    private Long getStrategyDeptHeadRoleId() {
        return requireConfigured(
                workflowApproverProperties.getStrategyDeptHeadRoleId(),
                "workflow.approver.strategy-dept-head-role-id"
        );
    }

    private Long getVicePresidentRoleId() {
        return requireConfigured(
                workflowApproverProperties.getVicePresidentRoleId(),
                "workflow.approver.vice-president-role-id"
        );
    }

    private Long getStrategyOrgId() {
        return requireConfigured(workflowApproverProperties.getStrategyOrgId(), "workflow.approver.strategy-org-id");
    }

    private Long requireConfigured(Long value, String propertyName) {
        return Objects.requireNonNull(value, propertyName + " is not configured");
    }
}
