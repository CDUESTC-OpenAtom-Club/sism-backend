package com.sism.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "sys_role", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysRole {

    @Id
    @SequenceGenerator(name="SysRole_IdSeq", sequenceName="public.sys_role_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SysRole_IdSeq")
    @Column(name="id")
    private Long id;

    @Column(name="role_code", nullable=false)
    private String roleCode;

    @Column(name="role_name", nullable=false)
    private String roleName;

    @Column(name="data_access_mode", nullable=false)
    private String dataAccessMode;

    @Column(name="is_enabled", nullable=false)
    private Boolean isEnabled;

    @Column(name="remark")
    private String remark;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;

}
