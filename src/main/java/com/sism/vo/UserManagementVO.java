package com.sism.vo;

import com.sism.enums.OrgType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理响应VO
 * 对应前端 UserManagementItem 接口
 *
 * @author SISM Development Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementVO {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Long orgId;
    private String orgName;
    private OrgType orgType;
    private List<RoleSummary> roles;
    private String status;  // "active" | "disabled"
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 角色摘要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleSummary {
        private Long roleId;
        private String roleCode;
        private String roleName;
    }
}
