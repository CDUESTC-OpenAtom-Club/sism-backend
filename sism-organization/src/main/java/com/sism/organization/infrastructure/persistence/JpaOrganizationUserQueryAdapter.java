package com.sism.organization.infrastructure.persistence;

import com.sism.iam.domain.repository.UserRepository;
import com.sism.organization.application.port.OrganizationUserQueryPort;
import com.sism.organization.interfaces.dto.OrgUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaOrganizationUserQueryAdapter implements OrganizationUserQueryPort {

    private final UserRepository userRepository;

    @Override
    public List<OrgUserResponse> findUsersByOrgId(Long orgId) {
        return userRepository.findByOrgId(orgId).stream()
                .map(OrgUserResponse::fromUser)
                .toList();
    }
}
