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
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No available approver candidates for step: " + stepName);
        }

        Optional<User> sameOrgCandidate = candidates.stream()
                .filter(user -> requesterOrgId != null && requesterOrgId.equals(user.getOrgId()))
                .min(Comparator.comparing(User::getId));
        if (sameOrgCandidate.isPresent()) {
            return sameOrgCandidate.get().getId();
        }

        return candidates.stream()
                .min(Comparator.comparing(User::getId))
                .map(User::getId)
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
                .sorted(Comparator
                        .comparing((User user) -> requesterOrgId == null || !requesterOrgId.equals(user.getOrgId()))
                        .thenComparing(User::getId))
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
}
