package com.sism.iam.application;

public interface EmailService {

    enum SenderType {
        NOREPLY,
        NOTICE
    }

    void sendTextEmail(String to, String subject, String content);

    void sendTextEmailFrom(SenderType senderType, String to, String subject, String content);

    void sendHtmlEmailFrom(SenderType senderType, String to, String subject, String templateName, java.util.Map<String, Object> variables);

    default void sendPasswordResetCode(String to, String code) {
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("title", "安全验证 / Secure Verification");
        variables.put("message", "您好！我们收到您在 SISM 战略指标管理系统中重置账户密码的请求。为确保您的账户安全，请使用以下验证码完成相关操作：");
        variables.put("code", code);
        variables.put("warning", "如果您未曾发起此请求，请忽略此邮件。请您务必注意，为保障您的个人信息与账户安全，切勿将此验证码透露给任何第三方。");
        
        sendHtmlEmailFrom(SenderType.NOREPLY, to, "SISM系统 - 找回密码验证码", "mail/mail-template", variables);
    }

    default void sendPasswordResetSuccessNotification(String to, String details) {
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("title", "密码重置成功 / Password Reset Successful");
        variables.put("message", String.format("您的账户密码已成功重置。操作详情如下：<br/>%s", details.replace("\n", "<br/>")));
        variables.put("warning", "如非本人操作，请立即联系管理员。");
        
        sendHtmlEmailFrom(SenderType.NOTICE, to, "SISM系统 - 密码重置成功提醒", "mail/mail-template", variables);
    }

    default void sendSystemNotification(String to, String subject, String content) {
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("title", subject);
        variables.put("message", content.replace("\n", "<br/>"));
        
        sendHtmlEmailFrom(SenderType.NOTICE, to, subject, "mail/mail-template", variables);
    }
}
