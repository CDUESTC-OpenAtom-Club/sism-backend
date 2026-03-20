package com.sism.workflow.application.support;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.interfaces.dto.ApproverCandidateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ApproverResolver {

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
                .sorted(candidateComparator(stepName, requesterOrgId))
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

    public List<ApproverCandidateResponse> resolveCandidates(AuditStepDef stepDef, Long requesterOrgId) {
        Long roleId = stepDef.getRoleId();
        if (roleId == null || roleId <= 0) {
            throw new IllegalStateException("Workflow step is missing role assignment: " + stepDef.getStepName());
        }

        List<ApproverCandidateResponse> candidates = userRepository.findByRoleId(roleId).stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .sorted(candidateComparator(stepDef.getStepName(), requesterOrgId))
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

    public void validateSelectedApprover(AuditStepDef stepDef, Long approverId) {
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
                .anyMatch(user -> approverId.equals(user.getId()));
        if (!valid) {
            throw new IllegalArgumentException("Selected approver does not match step role: " + stepDef.getStepName());
        }
    }

    private Comparator<User> candidateComparator(String stepName, Long requesterOrgId) {
        return Comparator
                .comparingInt((User user) -> candidatePriority(user, stepName, requesterOrgId))
                .thenComparing(User::getId);
    }

    private int candidatePriority(User user, String stepName, Long requesterOrgId) {
        Long userOrgId = user.getOrgId();

        if (isVicePresidentStep(stepName)) {
            return isStrategyOrg(userOrgId) ? 0 : 10;
        }
        if (isStrategyDepartmentStep(stepName)) {
            return isStrategyOrg(userOrgId) ? 0 : 10;
        }
        if (isSameOrgStep(stepName)) {
            return requesterOrgId != null && requesterOrgId.equals(userOrgId) ? 0 : 10;
        }

        return requesterOrgId != null && requesterOrgId.equals(userOrgId) ? 0 : 10;
    }

    private boolean isVicePresidentStep(String stepName) {
        return containsAny(stepName, "分管校领导", "校领导");
    }

    private boolean isStrategyDepartmentStep(String stepName) {
        return containsAny(stepName, "战略发展部负责人", "战略发展部终审");
    }

    private boolean isSameOrgStep(String stepName) {
        return containsAny(stepName, "职能部门审批人", "二级学院审批人", "学院院长", "职能部门终审人");
    }

    private boolean isStrategyOrg(Long orgId) {
        return orgId != null && orgId.equals(35L);
    }

    private boolean containsAny(String value, String... keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return Stream.of(keywords).anyMatch(value::contains);
    }
}
