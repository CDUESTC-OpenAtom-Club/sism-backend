package com.sism.entity;

import com.sism.enums.PlanLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "plan", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @SequenceGenerator(name="Plan_IdSeq", sequenceName="public.plan_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="Plan_IdSeq")
    @Column(name="id")
    private Long id;

    @Column(name="cycle_id", nullable=false)
    private Long cycleId;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    @Column(name="is_deleted", nullable=false)
    private Boolean isDeleted;

    @Column(name="target_org_id", nullable=false)
    private Long targetOrgId;

    @Column(name="created_by_org_id", nullable=false)
    private Long createdByOrgId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name="plan_level", columnDefinition="plan_level", nullable=false)
    private PlanLevel planLevel;

    @Column(name="status", nullable=false)
    private String status;

}
