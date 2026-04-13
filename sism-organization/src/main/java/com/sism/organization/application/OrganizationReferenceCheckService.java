package com.sism.organization.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationReferenceCheckService {

    private final JdbcTemplate jdbcTemplate;

    public boolean hasActiveReferences(Long orgId) {
        return hasPlanReferences(orgId) || hasIndicatorReferences(orgId) || hasTaskReferences(orgId);
    }

    private boolean hasPlanReferences(Long orgId) {
        return count("""
                SELECT COUNT(1)
                FROM public.plan p
                WHERE p.is_deleted = false
                  AND (p.target_org_id = ? OR p.created_by_org_id = ?)
                """, orgId, orgId) > 0;
    }

    private boolean hasIndicatorReferences(Long orgId) {
        return count("""
                SELECT COUNT(1)
                FROM public.indicator i
                WHERE i.is_deleted = false
                  AND (i.owner_org_id = ? OR i.target_org_id = ?)
                """, orgId, orgId) > 0;
    }

    private boolean hasTaskReferences(Long orgId) {
        return count("""
                SELECT COUNT(1)
                FROM public.sys_task t
                WHERE t.is_deleted = false
                  AND (t.org_id = ? OR t.created_by_org_id = ?)
                """, orgId, orgId) > 0;
    }

    private long count(String sql, Long firstParam, Long secondParam) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, firstParam, secondParam);
        return count == null ? 0L : count;
    }
}
