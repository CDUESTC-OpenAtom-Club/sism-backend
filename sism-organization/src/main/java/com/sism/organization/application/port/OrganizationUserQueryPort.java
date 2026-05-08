package com.sism.organization.application.port;

import com.sism.organization.interfaces.dto.OrgUserResponse;

import java.util.List;

public interface OrganizationUserQueryPort {

    List<OrgUserResponse> findUsersByOrgId(Long orgId);
}
