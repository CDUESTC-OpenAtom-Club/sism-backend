package com.sism.iam.interfaces.rest;

import com.sism.iam.application.service.AuthService;
import com.sism.iam.application.service.UserService;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.iam.application.dto.LoginRequest;
import com.sism.iam.application.dto.LoginResponse;
import com.sism.iam.domain.User;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AuthController - 认证API控制器
 * 提供用户认证相关的REST API端点
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody RegisterRequest request) {
        User user = authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getRealName()
        );
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getCurrentUser(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(2000, "未登录"));
        }
        return userService.findById(currentUser.getId())
                .map(user -> ResponseEntity.ok(ApiResponse.success(UserSummaryResponse.fromUser(user))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取当前用户权限编码列表
     */
    @GetMapping("/permissions")
    @Operation(summary = "获取当前用户权限编码列表")
    public ResponseEntity<ApiResponse<List<String>>> getCurrentUserPermissions(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(2000, "未登录"));
        }
        return ResponseEntity.ok(ApiResponse.success(authService.getPermissionCodes(currentUser.getId())));
    }

    /**
     * 验证Token
     */
    @GetMapping("/validate")
    @Operation(summary = "验证Token")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String token) {
        boolean valid = authService.validateToken(token.replace("Bearer ", ""));
        return ResponseEntity.ok(ApiResponse.success(valid));
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "UP",
                "timestamp", java.time.Instant.now().toString()
        )));
    }

    /**
     * 查询所有用户
     */
    @GetMapping("/users")
    @Operation(summary = "查询所有用户")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(
                userService.findAll().stream().map(UserSummaryResponse::fromUser).collect(Collectors.toList())
        ));
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "根据ID查询用户")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(UserSummaryResponse.fromUser(user))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/users/username/{username}")
    @Operation(summary = "根据用户名查询用户")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(ApiResponse.success(UserSummaryResponse.fromUser(user))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据组织ID查询用户
     */
    @GetMapping("/users/org/{orgId}")
    @Operation(summary = "根据组织ID查询用户")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getUsersByOrgId(@PathVariable Long orgId) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.findByOrgId(orgId).stream().map(UserSummaryResponse::fromUser).collect(Collectors.toList())
        ));
    }

    /**
     * 创建用户
     */
    @PostMapping("/users")
    @Operation(summary = "创建用户")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getRealName(),
                request.getEmail(),
                request.getOrgId()
        );
        return ResponseEntity.ok(ApiResponse.success(UserSummaryResponse.fromUser(user)));
    }

    /**
     * 更新用户
     */
    @PutMapping("/users/{id}")
    @Operation(summary = "更新用户")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(
                id,
                request.getRealName(),
                request.getEmail(),
                request.getOrgId()
        );
        return ResponseEntity.ok(ApiResponse.success(UserSummaryResponse.fromUser(user)));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    @Operation(summary = "删除用户")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 锁定用户
     */
    @PostMapping("/users/{id}/lock")
    @Operation(summary = "锁定用户")
    public ResponseEntity<ApiResponse<Void>> lockUser(@PathVariable Long id) {
        userService.lockUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 解锁用户
     */
    @PostMapping("/users/{id}/unlock")
    @Operation(summary = "解锁用户")
    public ResponseEntity<ApiResponse<Void>> unlockUser(@PathVariable Long id) {
        userService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== Request DTOs ====================

    @lombok.Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String realName;
    }

    @lombok.Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String realName;
        private String email;
        private Long orgId;
    }

    @lombok.Data
    public static class UpdateUserRequest {
        private String realName;
        private String email;
        private Long orgId;
    }

    @lombok.Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserSummaryResponse {
        private Long id;
        private String username;
        private String realName;
        private Long orgId;
        private Boolean isActive;
        private List<String> roles;

        public static UserSummaryResponse fromUser(User user) {
            return new UserSummaryResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRealName(),
                    user.getOrgId(),
                    user.getIsActive(),
                    user.getRoles().stream().map(role -> role.getRoleCode()).collect(Collectors.toList())
            );
        }
    }

    /**
     * 刷新访问令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Refresh token is required"));
        }
        try {
            Map<String, Object> result = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
