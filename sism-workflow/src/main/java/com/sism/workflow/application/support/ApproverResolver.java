package com.sism.workflow.application.support;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.interfaces.dto.ApproverCandidateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApproverResolver {

    private static final Long ROLE_APPROVER = 2L;
    private static final Long ROLE_STRATEGY_DEPT_HEAD = 3L;
    private static final Long ROLE_VICE_PRESIDENT = 4L;
    private static final Long STRATEGY_ORG_ID = 35L;
    private static final String PLAN_ENTITY_TYPE = "PLAN";
    private static final String COLLEGE_FINAL_APPROVAL_STEP_NAME = "职能部门终审";
    private static final Map<Long, Long> FUNCTIONAL_VICE_PRESIDENT_SCOPE_BY_ORG = Map.ofEntries(
            Map.entry(35L, 35L),
            Map.entry(36L, 36L),
            Map.entry(37L, 37L),
            Map.entry(38L, 38L),
            Map.entry(39L, 39L),
            Map.entry(40L, 40L),
            Map.entry(41L, 41L),
            Map.entry(42L, 42L),
            Map.entry(43L, 43L),
            Map.entry(44L, 44L),
            Map.entry(45L, 45L),
            Map.entry(46L, 46L),
            Map.entry(47L, 47L),
            Map.entry(48L, 48L),
            Map.entry(49L, 49L),
            Map.entry(50L, 50L),
            Map.entry(51L, 51L),
            Map.entry(52L, 52L),
            Map.entry(53L, 53L),
            Map.entry(54L, 54L)
    );

    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public Long resolveApproverId(AuditStepDef stepDef, Long requesterId, Long requesterOrgId) {
        return resolveApproverId(stepDef, requesterId, requesterOrgId, null);
    }

    public Long resolveApproverId(AuditStepDef stepDef, Long requesterId, Long requesterOrgId, AuditInstance instance) {
        Long roleId = stepDef.getRoleId();
        if (roleId == null || roleId <= 0) {
            throw new IllegalStateException("Workflow step is missing role assignment: " + stepDef.getStepName());
        }

        Long scopeOrgId = resolveScopeOrgId(stepDef, requesterOrgId, instance);
        return resolveByRoleId(roleId, requesterId, scopeOrgId, stepDef.getStepName());
    }

    private Long resolveByRoleId(Long roleId, Long requesterId, Long requesterOrgId, String stepName) {
        List<User> candidates = userRepository.findByRoleId(roleId).stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .filter(user -> matchesRoleScope(user, roleId, requesterOrgId, stepName))
                .sorted(Comparator.comparing(User::getId))
                .toList();
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
        if (ROLE_STRATEGY_DEPT_HEAD.equals(roleId)) {
            return STRATEGY_ORG_ID;
        }
        if (ROLE_VICE_PRESIDENT.equals(roleId)) {
            return resolveVicePresidentScopeOrgId(stepDef.getStepName(), scopeOrgId);
        }
        return scopeOrgId;
    }

    public List<ApproverCandidateResponse> resolveCandidates(AuditStepDef stepDef, Long requesterOrgId) {
        Long roleId = stepDef.getRoleId();
        if (roleId == null || roleId <= 0) {
            throw new IllegalStateException("Workflow step is missing role assignment: " + stepDef.getStepName());
        }

        List<ApproverCandidateResponse> candidates = userRepository.findByRoleId(roleId).stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .filter(user -> matchesRoleScope(user, roleId, requesterOrgId, stepDef.getStepName()))
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

        return userRepository.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .filter(user -> userRepository.findRoleIdsByUserId(userId).contains(roleId))
                .filter(user -> matchesRoleScope(user, roleId, scopeOrgId, stepDef.getStepName()))
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

        boolean valid = userRepository.findByRoleId(roleId).stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .filter(user -> matchesRoleScope(user, roleId, requesterOrgId, stepDef.getStepName()))
                .anyMatch(user -> approverId.equals(user.getId()));
        if (!valid) {
            throw new IllegalArgumentException("Selected approver does not match step role: " + stepDef.getStepName());
        }
    }

    private Long resolveScopeOrgId(AuditStepDef stepDef, Long requesterOrgId, AuditInstance instance) {
        if (!isCollegeFinalApprovalStep(stepDef, instance)) {
            return requesterOrgId;
        }

        return planRepository.findById(instance.getEntityId())
                .map(plan -> plan.getCreatedByOrgId() != null ? plan.getCreatedByOrgId() : requesterOrgId)
                .orElse(requesterOrgId);
    }

    private boolean isCollegeFinalApprovalStep(AuditStepDef stepDef, AuditInstance instance) {
        if (stepDef == null || instance == null) {
            return false;
        }
        if (!PLAN_ENTITY_TYPE.equalsIgnoreCase(instance.getEntityType())) {
            return false;
        }
        String stepName = stepDef.getStepName();
        return stepName != null && stepName.contains(COLLEGE_FINAL_APPROVAL_STEP_NAME);
    }

    private boolean matchesRoleScope(User user, Long roleId, Long requesterOrgId, String stepName) {
        Long userOrgId = user.getOrgId();

        if (ROLE_APPROVER.equals(roleId)) {
            return requesterOrgId != null && requesterOrgId.equals(userOrgId);
        }
        if (ROLE_STRATEGY_DEPT_HEAD.equals(roleId)) {
            return isStrategyOrg(userOrgId);
        }
        if (ROLE_VICE_PRESIDENT.equals(roleId)) {
            Long scopeOrgId = resolveVicePresidentScopeOrgId(stepName, requesterOrgId);
            return scopeOrgId != null && scopeOrgId.equals(userOrgId);
        }

        return true;
    }

    private Long resolveVicePresidentScopeOrgId(String stepName, Long requesterOrgId) {
        if (stepName != null && stepName.contains("学院院长")) {
            return requesterOrgId;
        }
        return FUNCTIONAL_VICE_PRESIDENT_SCOPE_BY_ORG.getOrDefault(requesterOrgId, requesterOrgId);
    }

    private boolean isStrategyOrg(Long orgId) {
        return orgId != null && orgId.equals(STRATEGY_ORG_ID);
    }
}
