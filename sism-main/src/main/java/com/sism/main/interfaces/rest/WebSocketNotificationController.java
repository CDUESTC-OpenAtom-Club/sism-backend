package com.sism.main.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.config.WebSocketNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Debug-only helper endpoint for local WebSocket integration verification.
 */
@RestController
@Profile("dev")
@RequestMapping("/api/v1/ws/notifications")
@Tag(name = "WebSocket Notifications", description = "本地WebSocket通知验证接口")
public class WebSocketNotificationController {

    private final WebSocketNotificationService notificationService;

    public WebSocketNotificationController(WebSocketNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/test/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "发送测试通知给特定用户")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestNotification(
            @PathVariable String userId,
            @RequestParam(defaultValue = "APPROVAL_REQUIRED") String type,
            @RequestParam(defaultValue = "测试通知") String title,
            @RequestParam(defaultValue = "前后端 WebSocket 联调成功") String content,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long approvalInstanceId,
            @RequestParam(required = false) String stepName
    ) {
        boolean delivered = notificationService.sendToUser(
                userId,
                type,
                title,
                content,
                entityType,
                entityId,
                approvalInstanceId,
                stepName
        );

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "userId", userId,
                "delivered", delivered
        )));
    }
}
