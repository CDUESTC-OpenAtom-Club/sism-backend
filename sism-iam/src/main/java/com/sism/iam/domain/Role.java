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
@Access(AccessType.FIELD)
public class Role extends AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, unique = true)
    private String roleCode;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "remark")
    private String description;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "data_access_mode", nullable = false)
    private String dataAccessMode = "OWN_ORG";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "sys_role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "perm_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

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
