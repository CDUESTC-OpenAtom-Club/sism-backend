package com.sism.vo;

import com.sism.enums.OrgType;
import lombok.Data;

/**
 * Value Object for organization data
 * Used for API responses
 */
@Data
public class OrgVO {
    
    private Long orgId;
    
    private String orgName;
    
    private OrgType orgType;
    
    private Long parentOrgId;
    
    private String parentOrgName;
    
    private Boolean isActive;
    
    private Integer sortOrder;
}
