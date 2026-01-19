package com.sism.enums;

/**
 * 进度审批状态枚举
 * 
 * Requirements: data-alignment-sop 3.4
 */
public enum ProgressApprovalStatus {
    /**
     * 无待审批
     */
    NONE,
    
    /**
     * 草稿
     */
    DRAFT,
    
    /**
     * 待审批
     */
    PENDING,
    
    /**
     * 已通过
     */
    APPROVED,
    
    /**
     * 已驳回
     */
    REJECTED
}
