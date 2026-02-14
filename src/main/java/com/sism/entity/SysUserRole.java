package com.sism.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "sys_user_role", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysUserRole {

    @Id
    @SequenceGenerator(name="SysUserRole_IdSeq", sequenceName="public.sys_user_role_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SysUserRole_IdSeq")
    @Column(name="id")
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(name="role_id", nullable=false)
    private Long roleId;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

}
