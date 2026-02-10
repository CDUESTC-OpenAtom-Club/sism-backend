package com.sism.entity;

import com.sism.enums.OrgType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * System Organization Entity
 * Flat structure with three types: STRATEGY_DEPT, FUNCTIONAL_DEPT, COLLEGE
 * Replaces the old Org entity, now using sys_org table
 */
@Getter
@Setter
@Entity
@Table(name = "sys_org", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sys_org_name", columnNames = "name")
}, indexes = {
    @Index(name = "idx_sys_org_type", columnList = "type"),
    @Index(name = "idx_sys_org_active", columnList = "is_active"),
    @Index(name = "idx_sys_org_sort", columnList = "sort_order")
})
public class SysOrg extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OrgType type;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
