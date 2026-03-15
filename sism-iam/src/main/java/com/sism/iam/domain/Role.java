package com.sism.iam.domain;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "sys_role")
public class Role extends AggregateRoot<Long> {
    // ID is inherited from AggregateRoot - do not redeclare

    @Column(name = "role_code", nullable = false, unique = true)
    private String roleCode;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "sys_role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @Override
    public void validate() {
        // 角色验证逻辑
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Role code is required");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role name is required");
        }
    }
}
