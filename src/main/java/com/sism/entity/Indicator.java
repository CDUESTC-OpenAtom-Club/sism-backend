package com.sism.entity;

import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.ProgressApprovalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Indicator entity with self-referential relationship
 * Supports hierarchical indicator structure
 * 
 * Updated: 2026-01-19 - Added fields for frontend data alignment
 * Requirements: data-alignment-sop 3.4, 5.1
 */
@Getter
@Setter
@Entity
@Table(name = "indicator")
public class Indicator extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_id")
    private Long indicatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private StrategicTask task;

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

    @Column(name = "weight_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal weightPercent = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndicatorStatus status = IndicatorStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @OneToMany(mappedBy = "indicator", cascade = CascadeType.ALL)
    private List<Milestone> milestones = new ArrayList<>();

    // ==================== 新增字段 (前端数据对齐) ====================

    /**
     * 是否为定性指标
     * true: 定性指标, false: 定量指标
     */
    @Column(name = "is_qualitative")
    private Boolean isQualitative = false;

    /**
     * 指标类型1: 定性/定量
     */
    @Column(name = "type1", length = 20)
    private String type1 = "定量";

    /**
     * 指标类型2: 发展性/基础性
     */
    @Column(name = "type2", length = 20)
    private String type2 = "基础性";

    /**
     * 是否可撤回
     */
    @Column(name = "can_withdraw")
    private Boolean canWithdraw = false;

    /**
     * 目标值
     */
    @Column(name = "target_value", precision = 10, scale = 2)
    private BigDecimal targetValue;

    /**
     * 实际值
     */
    @Column(name = "actual_value", precision = 10, scale = 2)
    private BigDecimal actualValue;

    /**
     * 单位 (%, 篇, 人, 家/专业 等)
     */
    @Column(name = "unit", length = 50)
    private String unit;

    /**
     * 责任人姓名
     */
    @Column(name = "responsible_person", length = 100)
    private String responsiblePerson;

    /**
     * 当前进度百分比 (0-100)
     */
    @Column(name = "progress")
    private Integer progress = 0;

    /**
     * 状态审计日志 (JSONB)
     * 存储 StatusAuditEntry 数组的 JSON 格式
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_audit", columnDefinition = "jsonb")
    private String statusAudit = "[]";

    /**
     * 进度审批状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "progress_approval_status", length = 20)
    private ProgressApprovalStatus progressApprovalStatus = ProgressApprovalStatus.NONE;

    /**
     * 待审批的进度值 (0-100)
     */
    @Column(name = "pending_progress")
    private Integer pendingProgress;

    /**
     * 待审批的说明
     */
    @Column(name = "pending_remark", columnDefinition = "TEXT")
    private String pendingRemark;

    /**
     * 待审批的附件URL列表 (JSONB)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pending_attachments", columnDefinition = "jsonb")
    private String pendingAttachments = "[]";
}
