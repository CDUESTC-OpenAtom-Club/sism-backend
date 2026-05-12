package com.sism.iam.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.mail.internet.MimeMessage;
import java.time.Year;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.port:25}")
    private int smtpPort;

    @Value("${spring.mail.protocol:smtp}")
    private String smtpProtocol;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean smtpStartTls;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:false}")
    private boolean smtpStartTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private boolean smtpSsl;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:5000}")
    private int smtpConnectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:5000}")
    private int smtpTimeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:5000}")
    private int smtpWriteTimeout;

    @Value("${mail.sender.noreply.address:${MAIL_NOREPLY_USERNAME:${spring.mail.username:}}}")
    private String noreplyAddress;

    @Value("${MAIL_NOREPLY_PASSWORD:${spring.mail.password:}}")
    private String noreplyPassword;

    @Value("${mail.sender.notice.address:${MAIL_NOTICE_USERNAME:${spring.mail.username:}}}")
    private String noticeAddress;

    @Value("${MAIL_NOTICE_PASSWORD:${MAIL_NOREPLY_PASSWORD:${spring.mail.password:}}}")
    private String noticePassword;

    private final Map<SenderType, JavaMailSender> dedicatedMailSenders = new ConcurrentHashMap<>();

    @Async
    @Override
    public void sendTextEmail(String to, String subject, String content) {
        sendTextEmailFrom(SenderType.NOREPLY, to, subject, content);
    }

    @Async
    @Override
    public void sendTextEmailFrom(SenderType senderType, String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String fromAddress = senderType == SenderType.NOTICE ? noticeAddress : noreplyAddress;
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            resolveMailSender(senderType).send(message);
            log.info("文本邮件发送成功: senderType={}, from={}, to={}", senderType, fromAddress, to);
        } catch (MailException ex) {
            log.error("异步文本邮件发送失败: senderType={}, to={}, error={}", senderType, to, ex.getMessage(), ex);
        }
    }

    @Async
    @Override
    public void sendHtmlEmailFrom(SenderType senderType, String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String fromAddress = senderType == SenderType.NOTICE ? noticeAddress : noreplyAddress;
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(to);
            helper.setSubject(subject);

            // 自动注入当前年份用于版权显示
            variables.putIfAbsent("currentYear", Year.now().getValue());
            
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            resolveMailSender(senderType).send(mimeMessage);
            log.info("HTML邮件发送成功: senderType={}, from={}, to={}", senderType, fromAddress, to);
        } catch (Exception ex) {
            log.error("异步HTML邮件发送失败: senderType={}, to={}, error={}", senderType, to, ex.getMessage(), ex);
        }
    }

    private JavaMailSender resolveMailSender(SenderType senderType) {
        return dedicatedMailSenders.computeIfAbsent(senderType, this::createDedicatedMailSender);
    }

    private JavaMailSender createDedicatedMailSender(SenderType senderType) {
        String username = senderType == SenderType.NOTICE ? noticeAddress : noreplyAddress;
        String password = senderType == SenderType.NOTICE ? noticePassword : noreplyPassword;

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return mailSender;
        }

        JavaMailSenderImpl dedicatedSender = new JavaMailSenderImpl();
        dedicatedSender.setHost(smtpHost);
        dedicatedSender.setPort(smtpPort);
        dedicatedSender.setProtocol(smtpProtocol);
        dedicatedSender.setUsername(username);
        dedicatedSender.setPassword(password);

        Properties properties = dedicatedSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", String.valueOf(smtpAuth));
        properties.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTls));
        properties.put("mail.smtp.starttls.required", String.valueOf(smtpStartTlsRequired));
        properties.put("mail.smtp.ssl.enable", String.valueOf(smtpSsl));
        properties.put("mail.smtp.connectiontimeout", String.valueOf(smtpConnectionTimeout));
        properties.put("mail.smtp.timeout", String.valueOf(smtpTimeout));
        properties.put("mail.smtp.writetimeout", String.valueOf(smtpWriteTimeout));
        return dedicatedSender;
    }
}
