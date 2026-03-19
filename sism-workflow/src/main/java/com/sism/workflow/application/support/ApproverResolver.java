package com.sism.workflow.application.support;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.workflow.domain.definition.model.AuditStepDef;
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
        if (roleId != null && roleId > 0) {
            return resolveByRoleId(roleId, requesterId, requesterOrgId);
        }

        return requesterId;
    }

    private Long resolveByRoleId(Long roleId, Long requesterId, Long requesterOrgId) {
        List<User> candidates = userRepository.findByRoleId(roleId).stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .toList();

        Optional<User> sameOrgCandidate = candidates.stream()
                .filter(user -> requesterOrgId != null && requesterOrgId.equals(user.getOrgId()))
                .min(Comparator.comparing(User::getId));
        if (sameOrgCandidate.isPresent()) {
            return sameOrgCandidate.get().getId();
        }

        return candidates.stream()
                .min(Comparator.comparing(User::getId))
                .map(User::getId)
                .orElse(requesterId);
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
}
