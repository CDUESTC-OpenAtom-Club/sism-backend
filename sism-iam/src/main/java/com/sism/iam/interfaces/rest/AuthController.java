package com.sism.iam.interfaces.rest;

import com.sism.iam.application.service.AuthService;
import com.sism.iam.application.service.UserService;
import com.sism.iam.application.dto.LoginRequest;
import com.sism.iam.application.dto.LoginResponse;
import com.sism.iam.domain.User;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@RequestHeader("Authorization") String token) {
        Long userId = authService.getUserIdFromToken(token.replace("Bearer ", ""));
        return userService.findById(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
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
     * 查询所有用户
     */
    @GetMapping("/users")
    @Operation(summary = "查询所有用户")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.findAll()));
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "根据ID查询用户")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/users/username/{username}")
    @Operation(summary = "根据用户名查询用户")
    public ResponseEntity<ApiResponse<User>> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据组织ID查询用户
     */
    @GetMapping("/users/org/{orgId}")
    @Operation(summary = "根据组织ID查询用户")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByOrgId(@PathVariable Long orgId) {
        return ResponseEntity.ok(ApiResponse.success(userService.findByOrgId(orgId)));
    }

    /**
     * 创建用户
     */
    @PostMapping("/users")
    @Operation(summary = "创建用户")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getRealName(),
                request.getEmail(),
                request.getOrgId()
        );
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 更新用户
     */
    @PutMapping("/users/{id}")
    @Operation(summary = "更新用户")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(
                id,
                request.getRealName(),
                request.getEmail(),
                request.getOrgId()
        );
        return ResponseEntity.ok(ApiResponse.success(user));
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
}
