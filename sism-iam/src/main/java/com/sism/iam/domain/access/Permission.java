package com.sism.iam.domain.access;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "sys_permission", schema = "public")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perm_code", nullable = false, unique = true)
    private String permissionCode;

    @Column(name = "perm_name", nullable = false)
    private String permissionName;

    @Column(name = "perm_type", nullable = false)
    private String permType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "route_path")
    private String routePath;

    @Column(name = "page_key")
    private String pageKey;

    @Column(name = "action_key")
    private String actionKey;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "remark")
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getPermCode() {
        return permissionCode;
    }

    public void setPermCode(String permCode) {
        this.permissionCode = permCode;
    }
}
