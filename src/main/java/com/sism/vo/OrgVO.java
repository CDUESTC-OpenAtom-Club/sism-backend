package com.sism.vo;

import com.sism.enums.OrgType;
import lombok.Data;

/**
 * Value Object for organization data
 * Used for API responses
 * Flat structure without parent-child relationships
 */
@Data
public class OrgVO {
    
    private Long orgId;
    
    private String orgName;
    
    private OrgType orgType;
    
    private Boolean isActive;
    
    private Integer sortOrder;
}
