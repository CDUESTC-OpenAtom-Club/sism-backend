package com.sism.iam.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.domain.Role;
import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UserProfileController - 用户个人中心控制器
 * 提供用户个人资料管理、密码修改等API
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户中心", description = "用户个人资料管理接口")
public class UserProfileController {

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
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(convertToProfileResponse(user)));
    }

    // ========== 头像上传 ==========

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传用户头像")
    public ResponseEntity<ApiResponse<AvatarResponse>> uploadAvatar(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        // 验证文件
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请选择要上传的文件"));
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (!ALLOWED_AVATAR_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("头像只能是 JPG/PNG/GIF/WebP 格式的图片"));
        }

        // 验证文件大小
        if (file.getSize() > MAX_AVATAR_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("头像大小不能超过 2MB"));
        }

        // 获取当前用户
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        // 保存文件
        String avatarUrl = saveAvatarFile(file, user.getId());

        // 更新用户头像URL
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} uploaded avatar: {}", username, avatarUrl);

        return ResponseEntity.ok(ApiResponse.success(new AvatarResponse(avatarUrl)));
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

        // 验证密码复杂度（手动验证，确保生效）
        String newPassword = request.getNewPassword();
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!newPassword.matches(passwordPattern)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("密码必须包含大小写字母、数字和特殊字符(@$!%*?&)，至少8个字符"));
        }

        // 更新密码
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
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

    private static final List<String> ALLOWED_AVATAR_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB

    private UserProfileResponse convertToProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setOrgId(user.getOrgId());
        response.setIsActive(user.getIsActive());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRoles(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList()));
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginTime(null); // TODO: 需要记录最后登录时间

        return response;
    }

    private String saveAvatarFile(MultipartFile file, Long userId) throws IOException {
        // 创建上传目录
        Path uploadDir = Paths.get("uploads", "avatars");
        Files.createDirectories(uploadDir);

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "user_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        // 保存文件
        Path targetPath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // 返回访问 URL
        return "/uploads/avatars/" + filename;
    }

    // ========== 请求和响应 DTO ==========

    @lombok.Data
    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String realName;
        private Long orgId;
        private Boolean isActive;  // 新版本改为 isActive 布尔字段
        private List<String> roles;
        private String avatarUrl;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginTime;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AvatarResponse {
        private String avatarUrl;
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
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                 message = "密码必须包含大小写字母、数字和特殊字符,至少8个字符")
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
