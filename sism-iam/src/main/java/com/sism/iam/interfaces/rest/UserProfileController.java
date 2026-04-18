package com.sism.iam.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.application.service.UserProfileService;
import com.sism.iam.domain.Role;
import com.sism.iam.domain.User;
import com.sism.organization.domain.repository.OrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserProfileController - 用户个人中心控制器
 * 提供用户个人资料管理、密码修改等API
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "用户中心", description = "用户个人资料管理接口")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final OrganizationRepository organizationRepository;

    // ========== 个人资料查询 ==========

    @GetMapping
    @Operation(summary = "获取当前用户个人资料")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication authentication) {
        User user = userProfileService.findCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(convertToProfileResponse(user)));
    }

    // ========== 个人资料更新 ==========

    @PutMapping
    @Operation(summary = "更新当前用户个人资料")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        User user = userProfileService.findCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user = userProfileService.updateProfile(user, request.getRealName());
        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            user = userProfileService.updateAvatar(user, request.getAvatar());
        }
        return ResponseEntity.ok(ApiResponse.success(convertToProfileResponse(user)));
    }

    // ========== 密码修改 ==========

    @PostMapping("/password")
    @Operation(summary = "修改当前用户密码")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        User user = userProfileService.findCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        userProfileService.changePassword(
                user,
                request.getOldPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ========== 第三方账号绑定 (TODO：需要相应的实体和服务) ==========

    @PostMapping("/bind-account")
    @Operation(summary = "绑定第三方账号 (待实现)")
    public ResponseEntity<ApiResponse<Void>> bindThirdPartyAccount(
            @Valid @RequestBody BindAccountRequest request,
            Authentication authentication
    ) {
        // TODO: 实现第三方账号绑定逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/accounts/{accountId}")
    @Operation(summary = "解绑第三方账号 (待实现)")
    public ResponseEntity<ApiResponse<Void>> unbindAccount(
            @PathVariable Long accountId,
            Authentication authentication
    ) {
        // TODO: 实现第三方账号解绑逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/accounts")
    @Operation(summary = "获取绑定的第三方账号列表 (待实现)")
    public ResponseEntity<ApiResponse<List<LinkedAccountResponse>>> getLinkedAccounts(
            Authentication authentication
    ) {
        // TODO: 实现获取绑定账号列表逻辑
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    // ========== 内部辅助方法 ==========

    private UserProfileResponse convertToProfileResponse(User user) {
        var organization = organizationRepository.findById(user.getOrgId()).orElse(null);
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setOrgId(user.getOrgId());
        response.setOrgName(organization != null ? organization.getName() : null);
        response.setOrgType(organization != null ? organization.getType().name() : null);
        response.setIsActive(user.getIsActive());
        response.setRoles(user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList()));
        response.setAvatar(user.getAvatarUrl());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginTime(null); // TODO: 需要记录最后登录时间

        return response;
    }

    // ========== 请求和响应 DTO ==========

    @lombok.Data
    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String realName;
        private Long orgId;
        private String orgName;
        private String orgType;
        private Boolean isActive;  // 新版本改为 isActive 布尔字段
        private List<String> roles;
        private String avatar;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginTime;
    }

    @lombok.Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "Real name is required")
        private String realName;

        private String avatar;
    }

    @lombok.Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Old password is required")
        private String oldPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        private String newPassword;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;
    }

    @lombok.Data
    public static class BindAccountRequest {
        @NotBlank(message = "Platform is required")
        private String platform;

        @NotBlank(message = "Account ID is required")
        private String accountId;
    }

    @lombok.Data
    public static class LinkedAccountResponse {
        private Long id;
        private String platform;
        private String platformNickname;
        private LocalDateTime bindTime;
    }
}
