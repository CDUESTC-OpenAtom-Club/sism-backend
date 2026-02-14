package com.sism.vo;

import com.sism.enums.OrgType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * System Organization Value Object
 */
@Data
public class SysOrgVO {

    /**
     * Organization ID
     */
    private Long id;

    /**
     * Organization name (unique)
     */
    private String name;

    /**
     * Organization type
     * STRATEGY_DEPT - Strategic Development Department (System Admin)
     * FUNCTIONAL_DEPT - Functional Department (Mid-level Management)
     * COLLEGE - Secondary College (Execution Layer)
     */
    private OrgType type;

    /**
     * Organization type display name
     */
    private String typeDisplay;

    /**
     * Is active
     */
    private Boolean isActive;

    /**
     * Sort order (smaller number appears first)
     */
    private Integer sortOrder;

    /**
     * Created at
     */
    private LocalDateTime createdAt;

    /**
     * Updated at
     */
    private LocalDateTime updatedAt;
}
