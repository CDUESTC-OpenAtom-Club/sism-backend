package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.UserCreateRequest;
import com.sism.dto.UserPasswordResetRequest;
import com.sism.dto.UserUpdateRequest;
import com.sism.service.UserManagementService;
import com.sism.vo.UserManagementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User Management Controller
 * Provides CRUD operations for user management
 *
 * @author SISM Development Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "用户管理API")
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * Search users with pagination and filters
     */
    @GetMapping
    @Operation(summary = "查询用户列表", description = "支持分页、筛选、排序")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<ApiResponse<Page<UserManagementVO>>> searchUsers(
            @Parameter(description = "用户名（模糊搜索）")
            @RequestParam(required = false) String username,

            @Parameter(description = "真实姓名（模糊搜索）")
            @RequestParam(required = false) String realName,

            @Parameter(description = "组织ID")
            @RequestParam(required = false) Long orgId,

            @Parameter(description = "启用状态")
            @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "页码（从0开始）")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(description = "排序字段")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "排序方向")
            @RequestParam(defaultValue = "asc") String sortOrder) {

        log.info("Search users request - username: {}, realName: {}, orgId: {}, isActive: {}, page: {}, size: {}",
                username, realName, orgId, isActive, page, size);

        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserManagementVO> result = userManagementService.searchUsers(
                username, realName, orgId, isActive, pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<ApiResponse<UserManagementVO>> getUserById(
            @Parameter(description = "用户ID")
            @PathVariable Long id) {

        log.info("Get user by ID: {}", id);
        UserManagementVO result = userManagementService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Create new user
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public ResponseEntity<ApiResponse<UserManagementVO>> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        log.info("Create user request: {}", request.getUsername());
        UserManagementVO result = userManagementService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("用户创建成功", result));
    }

    /**
     * Update user
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新用户信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<ApiResponse<UserManagementVO>> updateUser(
            @Parameter(description = "用户ID")
            @PathVariable Long id,

            @Valid @RequestBody UserUpdateRequest request) {

        log.info("Update user ID: {}", id);
        UserManagementVO result = userManagementService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("用户更新成功", result));
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除用户（软删除）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "用户ID")
            @PathVariable Long id) {

        log.info("Delete user ID: {}", id);
        userManagementService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("用户删除成功", null));
    }

    /**
     * Toggle user status
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户", description = "切换用户启用状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "状态更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
            @Parameter(description = "用户ID")
            @PathVariable Long id,

            @Parameter(description = "启用状态（true=启用, false=禁用）")
            @RequestParam Boolean isActive) {

        log.info("Toggle user {} status to: {}", id, isActive);
        userManagementService.toggleUserStatus(id, isActive);
        return ResponseEntity.ok(ApiResponse.success("用户状态更新成功", null));
    }

    /**
     * Reset user password
     */
    @PutMapping("/{id}/password")
    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "密码重置成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Parameter(description = "用户ID")
            @PathVariable Long id,

            @Valid @RequestBody UserPasswordResetRequest request) {

        log.info("Reset password for user ID: {}", id);
        userManagementService.resetPassword(id, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
    }
}
