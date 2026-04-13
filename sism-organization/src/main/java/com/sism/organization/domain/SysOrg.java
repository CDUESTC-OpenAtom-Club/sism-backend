package com.sism.organization.domain;

import com.sism.organization.domain.event.OrgCreatedEvent;
import com.sism.organization.domain.event.OrgActivatedEvent;
import com.sism.organization.domain.event.OrgDeactivatedEvent;
import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "sys_org", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sys_org_name", columnNames = "name")
}, indexes = {
        @Index(name = "idx_sys_org_type", columnList = "type"),
        @Index(name = "idx_sys_org_active", columnList = "is_active"),
        @Index(name = "idx_sys_org_sort", columnList = "sort_order"),
        @Index(name = "idx_sys_org_parent", columnList = "parent_org_id"),
        @Index(name = "idx_sys_org_level", columnList = "level")
})
@Access(AccessType.FIELD)
public class SysOrg extends AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "org_type")
    private OrgType type;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "parent_org_id")
    private Long parentOrgId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 子组织列表（ transient，不持久化到数据库）
    @Transient
    private List<SysOrg> children = new ArrayList<>();

    // ==================== Factory Methods ====================

    public static SysOrg create(String name, OrgType type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization name cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Organization type cannot be null");
        }

        String normalizedName = name.trim();
        validateNameLength(normalizedName);

        SysOrg org = new SysOrg();
        org.name = normalizedName;
        org.type = type;
        org.isActive = true;
        org.sortOrder = 0;
        org.level = 1;
        org.createdAt = LocalDateTime.now();
        org.updatedAt = LocalDateTime.now();
        org.isDeleted = false;
        return org;
    }

    // ==================== Business Methods ====================

    public void activate() {
        if (Boolean.TRUE.equals(this.isActive)) {
            throw new IllegalStateException("Organization is already active");
        }
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
        if (this.id != null) {
            this.addEvent(new OrgActivatedEvent(this.id, this.name));
        }
    }

    public void deactivate() {
        if (Boolean.FALSE.equals(this.isActive)) {
            throw new IllegalStateException("Organization is already inactive");
        }
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
        if (this.id != null) {
            this.addEvent(new OrgDeactivatedEvent(this.id, this.name));
        }
    }

    public void registerCreatedEvent() {
        if (this.id == null) {
            throw new IllegalStateException("Organization must be persisted before publishing created event");
        }
        this.addEvent(new OrgCreatedEvent(this.id, this.name, this.type.name()));
    }

    public void rename(String newName) {
        if (Objects.isNull(newName) || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization name cannot be empty");
        }
        String normalizedName = newName.trim();
        validateNameLength(normalizedName);
        this.name = normalizedName;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeType(OrgType newType) {
        if (Objects.isNull(newType)) {
            throw new IllegalArgumentException("Organization type cannot be null");
        }
        this.type = newType;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSortOrder(Integer sortOrder) {
        if (Objects.isNull(sortOrder) || sortOrder < 0) {
            throw new IllegalArgumentException("Sort order must be a non-negative integer");
        }
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateName(String name) {
        rename(name);
    }

    public void updateParent(SysOrg parentOrg) {
        if (parentOrg != null && parentOrg.id != null && parentOrg.id.equals(this.id)) {
            throw new IllegalArgumentException("Organization cannot be its own parent");
        }
        this.parentOrgId = parentOrg != null ? parentOrg.id : null;
        if (parentOrg != null) {
            this.level = parentOrg.level + 1;
        } else {
            this.level = 1;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Helper Methods ====================

    public String getOrgName() {
        return this.name;
    }

    public String getOrgCode() {
        return this.type.name() + "_" + this.id;
    }

    public String getOrgType() {
        return this.type.name();
    }

    public void setOrgName(String orgName) {
        rename(orgName);
    }

    public boolean isTopLevel() {
        return parentOrgId == null;
    }

    public boolean isFirstLevel() {
        return level != null && level == 1;
    }

    public boolean isSecondLevel() {
        return level != null && level == 2;
    }

    @Override
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization name is required");
        }
        validateNameLength(name.trim());
        if (type == null) {
            throw new IllegalArgumentException("Organization type is required");
        }
        if (level == null || level < 1) {
            throw new IllegalArgumentException("Organization level must be at least 1");
        }
    }

    private static void validateNameLength(String name) {
        if (name != null && name.length() > 100) {
            throw new IllegalArgumentException("Organization name cannot exceed 100 characters");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysOrg sysOrg = (SysOrg) o;
        return Objects.equals(id, sysOrg.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
