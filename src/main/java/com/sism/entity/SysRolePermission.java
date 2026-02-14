package com.sism.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "sys_role_permission", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysRolePermission {

    @Id
    @SequenceGenerator(name="SysRolePermission_IdSeq", sequenceName="public.sys_role_permission_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SysRolePermission_IdSeq")
    @Column(name="id")
    private Long id;

    @Column(name="role_id", nullable=false)
    private Long roleId;

    @Column(name="perm_id", nullable=false)
    private Long permId;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

}
