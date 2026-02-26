package com.sism.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "sys_permission", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysPermission {

    @Id
    @SequenceGenerator(name="SysPermission_IdSeq", sequenceName="public.sys_permission_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SysPermission_IdSeq")
    @Column(name="id")
    private Long id;

    @Column(name="perm_code", nullable=false)
    private String permCode;

    @Column(name="perm_name", nullable=false)
    private String permName;

    @Column(name="perm_type", nullable=false)
    private String permType;

    @Column(name="parent_id")
    private Long parentId;

    @Column(name="route_path")
    private String routePath;

    @Column(name="page_key")
    private String pageKey;

    @Column(name="action_key")
    private String actionKey;

    @Column(name="sort_order", nullable=false)
    private Integer sortOrder;

    @Column(name="is_enabled", nullable=false)
    private Boolean isEnabled;

    @Column(name="remark")
    private String remark;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;

}
