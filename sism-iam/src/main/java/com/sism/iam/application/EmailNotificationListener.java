package com.sism.iam.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final EmailService emailService;

    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        if (event == null || !event.emailEnabled() || event.userEmail() == null || event.userEmail().isBlank()) {
            return;
        }

        try {
            emailService.sendSystemNotification(
                    event.userEmail(),
                    "SISM系统通知：" + event.title(),
                    event.content()
            );
        } catch (Exception ex) {
            log.warn("通知邮件发送失败: to={}, title={}", event.userEmail(), event.title(), ex);
        }
    }
}
