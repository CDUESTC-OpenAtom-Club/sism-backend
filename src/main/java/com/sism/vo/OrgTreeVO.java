package com.sism.vo;

import com.sism.enums.OrgType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Value Object for organization hierarchy tree
 * Used for displaying organizational structure with nested children
 */
@Data
public class OrgTreeVO {
    
    private Long orgId;
    
    private String orgName;
    
    private OrgType orgType;
    
    private Long parentOrgId;
    
    private Integer sortOrder;
    
    private List<OrgTreeVO> children = new ArrayList<>();
}
