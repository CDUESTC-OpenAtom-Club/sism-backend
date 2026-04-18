package com.sism.organization.infrastructure.persistence;

import com.sism.organization.application.port.OrganizationUserQueryPort;
import com.sism.organization.interfaces.dto.OrgUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaOrganizationUserQueryAdapter implements OrganizationUserQueryPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<OrgUserResponse> findUsersByOrgId(Long orgId) {
        return jdbcTemplate.query(
                """
                SELECT id, username, real_name, is_active
                FROM sys_user
                WHERE org_id = ?
                ORDER BY id ASC
                """,
                (rs, rowNum) -> OrgUserResponse.builder()
                        .id(rs.getLong("id"))
                        .username(rs.getString("username"))
                        .realName(rs.getString("real_name"))
                        .isActive(rs.getBoolean("is_active"))
                        .build(),
                orgId
        );
    }
}
