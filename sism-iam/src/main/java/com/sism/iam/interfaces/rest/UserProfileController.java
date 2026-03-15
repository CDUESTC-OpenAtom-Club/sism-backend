package com.sism.iam.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.application.service.UserService;
import com.sism.iam.domain.Role;
import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * UserProfileController - 用户个人中心控制器
 * 提供用户个人资料管理、密码修改等API
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management endpoints")
public class UserProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ========== 个人资料查询 ==========

    @GetMapping
    @Operation(summary = "获取当前用户个人资料")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        return ResponseEntity.ok(ApiResponse.success(convertToProfileResponse(user)));
    }

    // ========== 个人资料更新 ==========

    @PutMapping
    @Operation(summary = "更新当前用户个人资料")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUpdatedAt(LocalDateTime.now());

        // TODO: 更新 avatar 需要单独的文件上传接口

        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(convertToProfileResponse(user)));
    }

    // ========== 密码修改 ==========

    @PostMapping("/password")
    @Operation(summary = "修改当前用户密码")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Current password is incorrect"));
        }

        // 验证两次输入的新密码一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("New password and confirm password do not match"));
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

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
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setOrgId(user.getOrgId());
        response.setStatus(user.getStatus().name());
        response.setRoles(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList()));
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
        private String email;
        private String phone;
        private Long orgId;
        private String status;
        private List<String> roles;
        private String avatar;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginTime;
    }

    @lombok.Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "Real name is required")
        private String realName;

        @Email(message = "Invalid email format")
        private String email;

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
        private String phone;

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
