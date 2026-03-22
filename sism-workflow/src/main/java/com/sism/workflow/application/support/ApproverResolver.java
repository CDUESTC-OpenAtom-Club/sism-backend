package com.sism.workflow.application.support;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.interfaces.dto.ApproverCandidateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApproverResolver {

    private static final Long ROLE_APPROVER = 2L;
    private static final Long ROLE_STRATEGY_DEPT_HEAD = 3L;
    private static final Long ROLE_VICE_PRESIDENT = 4L;
    private static final Long STRATEGY_ORG_ID = 35L;

    private final UserRepository userRepository;

    public Long resolveApproverId(AuditStepDef stepDef, Long requesterId, Long requesterOrgId) {
        Long roleId = stepDef.getRoleId();
        if (roleId == null || roleId <= 0) {
            throw new IllegalStateException("Workflow step is missing role assignment: " + stepDef.getStepName());
        }

        return resolveByRoleId(roleId, requesterId, requesterOrgId, stepDef.getStepName());
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
        if (stepDef == null || stepDef.isSubmitStep() || userId == null || userId <= 0) {
            return false;
        }

        Long roleId = stepDef.getRoleId();
        if (roleId == null || roleId <= 0) {
            return false;
        }

        return userRepository.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .filter(user -> userRepository.findRoleIdsByUserId(userId).contains(roleId))
                .filter(user -> matchesRoleScope(user, roleId, requesterOrgId, stepDef.getStepName()))
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

    private boolean matchesRoleScope(User user, Long roleId, Long requesterOrgId, String stepName) {
        Long userOrgId = user.getOrgId();

        if (ROLE_APPROVER.equals(roleId)) {
            return requesterOrgId != null && requesterOrgId.equals(userOrgId);
        }
        if (ROLE_STRATEGY_DEPT_HEAD.equals(roleId)) {
            return isStrategyOrg(userOrgId);
        }
        if (ROLE_VICE_PRESIDENT.equals(roleId)) {
            if (stepName != null && stepName.contains("学院院长")) {
                return requesterOrgId != null && requesterOrgId.equals(userOrgId);
            }
            return isStrategyOrg(userOrgId);
        }

        return true;
    }

    private boolean isStrategyOrg(Long orgId) {
        return orgId != null && orgId.equals(STRATEGY_ORG_ID);
    }
}
