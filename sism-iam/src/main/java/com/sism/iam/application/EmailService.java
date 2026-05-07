package com.sism.iam.application;

public interface EmailService {

    void sendTextEmail(String to, String subject, String content);

    default void sendPasswordResetCode(String to, String code) {
        String content = String.format(
                "【SISM系统】您正在申请重置密码，验证码为：%s，请在10分钟内完成操作。如非本人操作，请忽略此邮件。",
                code
        );
        sendTextEmail(to, "SISM系统 - 找回密码验证码", content);
    }

    default void sendPasswordResetSuccessNotification(String to, String details) {
        String content = String.format(
                "【SISM系统】您的账号密码已成功重置。操作详情如下：%n%s%n%n如非本人操作，请立即联系管理员。",
                details
        );
        sendTextEmail(to, "SISM系统 - 密码重置成功提醒", content);
    }
}
