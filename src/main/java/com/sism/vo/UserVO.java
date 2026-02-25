package com.sism.vo;

import com.sism.enums.OrgType;

/**
 * Value Object for user data
 * Used for API responses, includes organization information
 *
 * **Validates: Requirements 4.1**
 *
 * @author SISM Team
 */
public class UserVO {

    private Long userId;
    private String username;
    private String realName;
    private Boolean isActive;
    private Long orgId;
    private String orgName;
    private OrgType orgType;

    /**
     * Default constructor
     */
    public UserVO() {
    }

    /**
     * Full constructor with validation
     */
    public UserVO(Long userId, String username, String realName, Boolean isActive,
                  Long orgId, String orgName, OrgType orgType) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.isActive = isActive != null ? isActive : true;
        this.orgId = orgId;
        this.orgName = orgName;
        this.orgType = orgType;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public OrgType getOrgType() {
        return orgType;
    }

    public void setOrgType(OrgType orgType) {
        this.orgType = orgType;
    }
}