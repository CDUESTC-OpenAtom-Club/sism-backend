package com.sism.main.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.main.application.MessageCenterApplicationService;
import com.sism.main.interfaces.dto.MessageCenterModels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/message-center")
@RequiredArgsConstructor
@Tag(name = "消息中心", description = "统一消息中心聚合接口")
public class MessageCenterController {

    private final MessageCenterApplicationService messageCenterApplicationService;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取消息中心摘要")
    public ResponseEntity<ApiResponse<MessageCenterModels.Summary>> getSummary(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(2000, "未登录"));
        }
        return ResponseEntity.ok(ApiResponse.success(messageCenterApplicationService.getSummary(currentUser.getId())));
    }

    @GetMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取消息中心列表")
    public ResponseEntity<ApiResponse<MessageCenterModels.ListResponse>> getMessages(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeRisk) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(2000, "未登录"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                messageCenterApplicationService.getMessages(currentUser.getId(), category, page, size, keyword, includeRisk)
        ));
    }

    @GetMapping("/messages/{messageId:.+}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取消息详情")
    public ResponseEntity<ApiResponse<MessageCenterModels.Item>> getMessageDetail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String messageId) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(2000, "未登录"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    messageCenterApplicationService.getMessageDetail(currentUser.getId(), messageId)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, ex.getMessage()));
        }
    }

    @PostMapping("/messages/{messageId:.+}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记消息已读")
    public ResponseEntity<ApiResponse<MessageCenterModels.ReadResult>> markMessageAsRead(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String messageId) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(2000, "未登录"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    messageCenterApplicationService.markMessageAsRead(currentUser.getId(), messageId)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    @PostMapping("/messages/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "全部标记已读")
    public ResponseEntity<ApiResponse<MessageCenterModels.ReadResult>> markAllAsRead(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(2000, "未登录"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                messageCenterApplicationService.markAllAsRead(currentUser.getId())
        ));
    }
}
