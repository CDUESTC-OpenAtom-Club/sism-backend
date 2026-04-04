package com.sism.iam.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.iam.application.service.NotificationService;
import com.sism.iam.application.service.UserNotificationService;
import com.sism.iam.domain.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NotificationController - 通知（告警事件）控制器
 * 处理告警事件相关的REST API
 * 
 * 说明：此Controller处理的是 alert_event 表中的数据
 * 代表指标监控中的告警事件，而非用户消息通知
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "告警事件通知管理接口")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;

    // ==================== User Notification Operations ====================

    @GetMapping("/my")
    @Operation(summary = "查询我的通知", description = "查询当前用户的通知列表，支持分页和状态筛选")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(2000, "未登录"));
        }
        Long userId = currentUser.getId();
        Page<Map<String, Object>> notifications = userNotificationService.getMyNotifications(userId, page, size, status);

        Map<String, Object> response = new HashMap<>();
        response.put("content", notifications.getContent());
        response.put("totalElements", notifications.getTotalElements());
        response.put("totalPages", notifications.getTotalPages());
        response.put("number", notifications.getNumber());
        response.put("size", notifications.getSize());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记已读", description = "将所有通知标记为已读")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllNotificationsAsRead(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(2000, "未登录"));
        }
        return ResponseEntity.ok(ApiResponse.success(userNotificationService.markAllNotificationsAsRead(currentUser.getId())));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记单条已读", description = "将通知标记为已读")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markNotificationAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(2000, "未登录"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success(userNotificationService.markNotificationAsRead(id, currentUser.getId())));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    // ==================== Alert Event Operations ====================

    /**
     * 根据指标ID查询告警事件
     */
    @GetMapping("/indicator/{indicatorId}")
    @Operation(summary = "按指标ID分页查询告警事件")
    public ResponseEntity<Page<Notification>> getNotificationsByIndicatorId(
            @PathVariable Long indicatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                notificationService.getNotificationsByIndicatorId(indicatorId, PageRequest.of(page, size))
        );
    }

    /**
     * 根据规则ID查询告警事件
     */
    @GetMapping("/rule/{ruleId}")
    @Operation(summary = "按规则ID查询告警事件")
    public ResponseEntity<List<Notification>> getNotificationsByRuleId(@PathVariable Long ruleId) {
        return ResponseEntity.ok(notificationService.getNotificationsByRuleId(ruleId));
    }

    /**
     * 根据窗口ID查询告警事件
     */
    @GetMapping("/window/{windowId}")
    @Operation(summary = "按时间窗口ID查询告警事件")
    public ResponseEntity<List<Notification>> getNotificationsByWindowId(@PathVariable Long windowId) {
        return ResponseEntity.ok(notificationService.getNotificationsByWindowId(windowId));
    }

    /**
     * 获取指定指标的告警事件统计
     */
    @GetMapping("/indicator/{indicatorId}/count")
    @Operation(summary = "统计指标的告警事件数量")
    public ResponseEntity<Long> countNotificationsByIndicatorId(@PathVariable Long indicatorId) {
        return ResponseEntity.ok(notificationService.countNotificationsByIndicatorId(indicatorId));
    }

    /**
     * 获取指定规则和状态的告警事件统计
     */
    @GetMapping("/rule/{ruleId}/count")
    @Operation(summary = "按规则和状态统计告警事件数量")
    public ResponseEntity<Long> countNotificationsByRuleIdAndStatus(
            @PathVariable Long ruleId,
            @RequestParam String status) {
        return ResponseEntity.ok(notificationService.countNotificationsByRuleIdAndStatus(ruleId, status));
    }

    /**
     * 根据ID查询单个告警事件
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取告警事件")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Long id) {
        return notificationService.getNotificationById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 查询所有告警事件
     */
    @GetMapping
    @Operation(summary = "获取所有告警事件")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    /**
     * 创建告警事件
     */
    @PostMapping
    @Operation(summary = "创建新告警事件")
    public ResponseEntity<Notification> createNotification(
            @RequestParam Long indicatorId,
            @RequestParam Long ruleId,
            @RequestParam Long windowId,
            @RequestParam String severity,
            @RequestParam String status,
            @RequestParam(required = false) BigDecimal actualPercent,
            @RequestParam(required = false) BigDecimal expectedPercent,
            @RequestParam(required = false) BigDecimal gapPercent) {

        Notification notification = notificationService.createNotification(
                indicatorId, ruleId, windowId, severity, status
        );

        if (actualPercent != null) notification.setActualPercent(actualPercent);
        if (expectedPercent != null) notification.setExpectedPercent(expectedPercent);
        if (gapPercent != null) notification.setGapPercent(gapPercent);

        return ResponseEntity.status(201).body(notification);
    }

    /**
     * 更新告警事件状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新告警事件状态")
    public ResponseEntity<Notification> updateNotificationStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) {
        return ResponseEntity.ok(notificationService.updateNotificationStatus(id, newStatus));
    }

    /**
     * 标记告警事件为已处理
     */
    @PostMapping("/{id}/handle")
    @Operation(summary = "标记告警事件为已处理")
    public ResponseEntity<Notification> handleNotification(
            @PathVariable Long id,
            @RequestParam Long handledByUserId,
            @RequestParam(required = false) String handledNote) {
        return ResponseEntity.ok(
                notificationService.handleNotification(id, handledByUserId, handledNote)
        );
    }

    /**
     * 删除告警事件
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除告警事件")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
