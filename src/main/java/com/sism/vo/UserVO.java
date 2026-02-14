package com.sism.vo;

import com.sism.enums.OrgType;
import lombok.Data;

/**
 * Value Object for user data
 * Used for API responses, includes organization information
 */
@Data
public class UserVO {
    
    private Long userId;
    
    private String username;
    
    private String realName;
    
    private Boolean isActive;
    
    // Organization information
    private Long orgId;
    
    private String orgName;
    
    private OrgType orgType;
}
