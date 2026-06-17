package com.blog.auth.service.impl;

import com.blog.auth.config.properties.BlogSiteProperties;
import com.blog.auth.service.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;
    private final BlogSiteProperties blogSiteProperties;

    @Override
    public void sendPasswordResetLink(String email, String username, String resetLink) {
        String name = StringUtils.hasText(username) ? username : "用户";
        String body = name + "，您好：\n\n"
                + "请在 30 分钟内打开以下链接重置密码：\n" + resetLink + "\n\n"
                + "如果不是您本人操作，请忽略本邮件。";
        sendIfConfigured(email, "密码重置验证", body);
    }

    @Override
    public void sendPasswordResetSuccess(String email, String username) {
        String name = StringUtils.hasText(username) ? username : "用户";
        String body = name + "，您好：\n\n"
                + "您的账号密码已重置成功。如非本人操作，请立即联系管理员。";
        sendIfConfigured(email, "密码重置成功通知", body);
    }

    private void sendIfConfigured(String to, String subject, String text) {
        if (!blogSiteProperties.isNotifyMailEnabled()) {
            log.debug("[mail off] to={} subject={}", to, subject);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("[mail skipped] JavaMailSender absent");
            return;
        }
        String from = resolveFrom();
        if (!StringUtils.hasText(from)) {
            log.warn("[mail skipped] from address absent");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("mail send failed to={}: {}", to, e.toString());
        }
    }

    private String resolveFrom() {
        if (StringUtils.hasText(blogSiteProperties.getNotifyFrom())) {
            return blogSiteProperties.getNotifyFrom();
        }
        return mailProperties.getUsername();
    }
}
