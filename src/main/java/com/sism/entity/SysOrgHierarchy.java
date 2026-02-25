package com.sism.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Organization Hierarchy Entity
 * Stores the hierarchical relationships between organizations
 * 
 * Fields:
 * - orgId: The organization ID
 * - parentOrgId: The parent organization ID
 * - level: The organization level in the hierarchy
 */
@Getter
@Setter
@Entity
@Table(name = "sys_org_hierarchy", indexes = {
    @Index(name = "idx_org_hierarchy_org", columnList = "org_id"),
    @Index(name = "idx_org_hierarchy_parent", columnList = "parent_org_id")
})
public class SysOrgHierarchy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "org_id", nullable = false, unique = true)
    private Long orgId;

    @Column(name = "parent_org_id")
    private Long parentOrgId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ==================== Constructors ====================

    public SysOrgHierarchy() {
    }

    public SysOrgHierarchy(Long orgId, Long parentOrgId, Integer level) {
        this.orgId = orgId;
        this.parentOrgId = parentOrgId;
        this.level = level;
    }

    // ==================== Helper Methods ====================

    /**
     * Check if this is a top-level organization (no parent)
     */
    public boolean isTopLevel() {
        return parentOrgId == null;
    }

    /**
     * Check if this is a first-level organization
     */
    public boolean isFirstLevel() {
        return level != null && level == 1;
    }

    /**
     * Check if this is a second-level organization
     */
    public boolean isSecondLevel() {
        return level != null && level == 2;
    }
}
