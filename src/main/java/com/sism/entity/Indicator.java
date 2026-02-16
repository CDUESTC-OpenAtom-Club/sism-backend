package com.sism.entity;

import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Indicator entity - simplified to match actual database schema
 * Supports hierarchical indicator structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "indicator", schema = "public")
public class Indicator {

    @Id
    @SequenceGenerator(name="Indicator_IdSeq", sequenceName="public.indicator_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="Indicator_IdSeq")
    @Column(name="id")
    private Long indicatorId;

    @Column(name="task_id", nullable=false)
    private Long taskId;

    @Column(name="parent_indicator_id", insertable = false, updatable = false)
    private Long parentIndicatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_indicator_id")
    private Indicator parentIndicator;

    @OneToMany(mappedBy = "parentIndicator")
    private List<Indicator> childIndicators = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndicatorLevel level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_org_id", nullable = false)
    private SysOrg ownerOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_org_id", nullable = false)
    private SysOrg targetOrg;

    @Column(name = "indicator_desc", nullable = false, columnDefinition = "TEXT")
    private String indicatorDesc;

    @Column(name="weight_percent", nullable=false)
    private BigDecimal weightPercent;

    @Column(name="sort_order", nullable=false)
    private Integer sortOrder;

    @Column(name="remark")
    private String remark;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    @Column(name="type", nullable=false)
    private String type;

    @Column(name="progress")
    private Integer progress;

    @Column(name="is_deleted", nullable=false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Indicator status (ACTIVE, ARCHIVED, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private IndicatorStatus status = IndicatorStatus.ACTIVE;

    // ==================== 扩展字段 (前端数据对齐 2026-01-19) ====================

    /**
     * 实际完成值
     */
    @Column(name = "actual_value")
    private BigDecimal actualValue;

    /**
     * 目标值
     */
    @Column(name = "target_value")
    private BigDecimal targetValue;

    /**
     * 单位
     */
    @Column(name = "unit", length = 50)
    private String unit;

    /**
     * 负责人
     */
    @Column(name = "responsible_person", length = 100)
    private String responsiblePerson;

    /**
     * 是否可撤回
     */
    @Column(name = "can_withdraw")
    private Boolean canWithdraw;

    /**
     * 是否定性指标
     */
    @Column(name = "is_qualitative")
    private Boolean isQualitative;

    /**
     * 待审批进度
     */
    @Column(name = "pending_progress")
    private Integer pendingProgress;

    /**
     * 待审批备注
     */
    @Column(name = "pending_remark", columnDefinition = "TEXT")
    private String pendingRemark;

    /**
     * 待审批附件 (JSON格式)
     */
    @Column(name = "pending_attachments", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String pendingAttachments;

    /**
     * 进度审批状态
     */
    @Column(name = "progress_approval_status", length = 20)
    private String progressApprovalStatus;

    /**
     * 审批状态历史 (JSON格式)
     */
    @Column(name = "status_audit", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String statusAudit;

    /**
     * 类型1 (定量/定性)
     */
    @Column(name = "type1", length = 20)
    private String type1;

    /**
     * 类型2 (基础性/发展性)
     */
    @Column(name = "type2", length = 20)
    private String type2;

    // ==================== 部门字段 (数据看板支持 2026-02-08) ====================

    /**
     * 来源部门/下发部门（职能部门）
     */
    @Column(name = "owner_dept", length = 100)
    private String ownerDept;

    /**
     * 责任部门/承接部门（可以是职能部门或二级学院）
     */
    @Column(name = "responsible_dept", length = 100)
    private String responsibleDept;

    /**
     * 指标所属年份
     */
    @Column(name = "`year`")  // Quoted for H2 compatibility (reserved keyword)
    private Integer year;

    // 关联关系 (用于Service层查询)
    @OneToMany(mappedBy = "indicator", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Milestone> milestones = new ArrayList<>();

    /**
     * Set default values before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = IndicatorStatus.ACTIVE;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}
