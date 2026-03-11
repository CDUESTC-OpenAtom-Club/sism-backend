package com.sism.entity;

import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.ProgressApprovalStatus;
import com.sism.vo.IndicatorVO;
import com.sism.vo.MilestoneVO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Column(name="task_id")
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
     * Indicator status (DRAFT, PENDING_REVIEW, DISTRIBUTED, ARCHIVED)
     * Default to DRAFT as the initial lifecycle state
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private IndicatorStatus status = IndicatorStatus.DRAFT;

    /**
     * Distribution status (DISTRIBUTED, etc.)
     * Separate field for tracking distribution state
     */
    @Column(name = "distribution_status", length = 20)
    private String distributionStatus;

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
    @Enumerated(EnumType.STRING)
    @Column(name = "progress_approval_status", length = 20)
    @Builder.Default
    private ProgressApprovalStatus progressApprovalStatus = ProgressApprovalStatus.NONE;

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
            status = IndicatorStatus.DRAFT;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    // ==================== toDTO Method ====================

    /**
     * Convert this Indicator entity to IndicatorVO
     *
     * **Validates: Requirements 4.2**
     *
     * @return IndicatorVO with all field mappings
     */
    public IndicatorVO toDTO() {
        // 如果 ownerDept 为空,从 ownerOrg 关联对象中获取
        String ownerDeptName = this.ownerDept;
        if (ownerDeptName == null && this.ownerOrg != null) {
            ownerDeptName = this.ownerOrg.getName();
        }
        
        // 如果 responsibleDept 为空,从 targetOrg 关联对象中获取
        String responsibleDeptName = this.responsibleDept;
        if (responsibleDeptName == null && this.targetOrg != null) {
            responsibleDeptName = this.targetOrg.getName();
        }
        
        return new IndicatorVO(
            this.indicatorId,
            this.taskId,
            this.parentIndicatorId,
            this.indicatorDesc,
            this.weightPercent,
            this.sortOrder,
            this.remark,
            this.type,
            this.progress,
            this.createdAt,
            this.updatedAt,
            this.year,
            ownerDeptName,
            responsibleDeptName,
            this.targetOrg != null ? this.targetOrg.getId() : null, // targetOrgId
            this.ownerOrg != null ? this.ownerOrg.getId() : null, // ownerOrgId
            this.weightPercent, // weight alias
            null, // taskName - should be set by caller
            this.canWithdraw,
            this.status,
            this.distributionStatus,
            this.isQualitative,
            this.type1,
            this.type2,
            this.level != null ? this.level.name() : null, // level
            this.unit, // unit
            this.actualValue, // actualValue
            this.targetValue, // targetValue
            this.responsiblePerson, // responsiblePerson
            // isStrategic - 判断逻辑: owner_dept = '战略发展部' 且 responsible_dept 不包含"学院"
            "战略发展部".equals(ownerDeptName) && responsibleDeptName != null && !responsibleDeptName.contains("学院"),
            this.statusAudit, // statusAudit - JSON string
            this.progressApprovalStatus != null ? this.progressApprovalStatus.name() : null, // progressApprovalStatus - convert enum to string
            this.pendingProgress, // pendingProgress
            this.pendingRemark, // pendingRemark
            this.pendingAttachments, // pendingAttachments - JSON string
            this.childIndicators != null
                ? this.childIndicators.stream()
                    .map(Indicator::toDTO)
                    .collect(Collectors.toList())
                : List.of(),
            this.milestones != null
                ? this.milestones.stream()
                    .map(Milestone::toDTO)
                    .collect(Collectors.toList())
                : List.of()
        );
    }

    /**
     * Convert this Indicator entity to IndicatorVO with task name
     *
     * **Validates: Requirements 4.2**
     *
     * @param taskName Task name to include
     * @return IndicatorVO with all field mappings
     */
    public IndicatorVO toDTO(String taskName) {
        // 如果 ownerDept 为空,从 ownerOrg 关联对象中获取
        String ownerDeptName = this.ownerDept;
        if (ownerDeptName == null && this.ownerOrg != null) {
            ownerDeptName = this.ownerOrg.getName();
        }
        
        // 如果 responsibleDept 为空,从 targetOrg 关联对象中获取
        String responsibleDeptName = this.responsibleDept;
        if (responsibleDeptName == null && this.targetOrg != null) {
            responsibleDeptName = this.targetOrg.getName();
        }
        
        return new IndicatorVO(
            this.indicatorId,
            this.taskId,
            this.parentIndicatorId,
            this.indicatorDesc,
            this.weightPercent,
            this.sortOrder,
            this.remark,
            this.type,
            this.progress,
            this.createdAt,
            this.updatedAt,
            this.year,
            ownerDeptName,
            responsibleDeptName,
            this.targetOrg != null ? this.targetOrg.getId() : null, // targetOrgId
            this.ownerOrg != null ? this.ownerOrg.getId() : null, // ownerOrgId
            this.weightPercent, // weight alias
            taskName,
            this.canWithdraw,
            this.status,
            this.distributionStatus,
            this.isQualitative,
            this.type1,
            this.type2,
            this.level != null ? this.level.name() : null, // level
            this.unit, // unit
            this.actualValue, // actualValue
            this.targetValue, // targetValue
            this.responsiblePerson, // responsiblePerson
            // isStrategic - 判断逻辑: owner_dept = '战略发展部' 且 responsible_dept 不包含"学院"
            "战略发展部".equals(ownerDeptName) && responsibleDeptName != null && !responsibleDeptName.contains("学院"),
            this.statusAudit, // statusAudit - JSON string
            this.progressApprovalStatus != null ? this.progressApprovalStatus.name() : null, // progressApprovalStatus - convert enum to string (second toDTO method)
            this.pendingProgress, // pendingProgress
            this.pendingRemark, // pendingRemark
            this.pendingAttachments, // pendingAttachments - JSON string
            this.childIndicators != null
                ? this.childIndicators.stream()
                    .map(Indicator::toDTO)
                    .collect(Collectors.toList())
                : List.of(),
            this.milestones != null
                ? this.milestones.stream()
                    .map(Milestone::toDTO)
                    .collect(Collectors.toList())
                : List.of()
        );
    }
}