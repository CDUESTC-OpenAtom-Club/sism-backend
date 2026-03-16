package com.sism.iam.interfaces.rest;

import com.sism.iam.application.service.NotificationService;
import com.sism.iam.domain.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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
@Tag(name = "Notifications", description = "Alert event notification management")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 根据指标ID查询告警事件
     */
    @GetMapping("/indicator/{indicatorId}")
    @Operation(summary = "Get alert events by indicator ID with pagination")
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
    @Operation(summary = "Get alert events by rule ID")
    public ResponseEntity<List<Notification>> getNotificationsByRuleId(@PathVariable Long ruleId) {
        return ResponseEntity.ok(notificationService.getNotificationsByRuleId(ruleId));
    }

    /**
     * 根据窗口ID查询告警事件
     */
    @GetMapping("/window/{windowId}")
    @Operation(summary = "Get alert events by window ID")
    public ResponseEntity<List<Notification>> getNotificationsByWindowId(@PathVariable Long windowId) {
        return ResponseEntity.ok(notificationService.getNotificationsByWindowId(windowId));
    }

    /**
     * 获取指定指标的告警事件统计
     */
    @GetMapping("/indicator/{indicatorId}/count")
    @Operation(summary = "Count alert events for an indicator")
    public ResponseEntity<Long> countNotificationsByIndicatorId(@PathVariable Long indicatorId) {
        return ResponseEntity.ok(notificationService.countNotificationsByIndicatorId(indicatorId));
    }

    /**
     * 获取指定规则和状态的告警事件统计
     */
    @GetMapping("/rule/{ruleId}/count")
    @Operation(summary = "Count alert events by rule and status")
    public ResponseEntity<Long> countNotificationsByRuleIdAndStatus(
            @PathVariable Long ruleId,
            @RequestParam String status) {
        return ResponseEntity.ok(notificationService.countNotificationsByRuleIdAndStatus(ruleId, status));
    }

    /**
     * 根据ID查询单个告警事件
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get alert event by ID")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Long id) {
        return notificationService.getNotificationById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 查询所有告警事件
     */
    @GetMapping
    @Operation(summary = "Get all alert events")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    /**
     * 创建告警事件
     */
    @PostMapping
    @Operation(summary = "Create a new alert event")
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
    @Operation(summary = "Update alert event status")
    public ResponseEntity<Notification> updateNotificationStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) {
        return ResponseEntity.ok(notificationService.updateNotificationStatus(id, newStatus));
    }

    /**
     * 标记告警事件为已处理
     */
    @PostMapping("/{id}/handle")
    @Operation(summary = "Mark alert event as handled")
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
    @Operation(summary = "Delete alert event")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
