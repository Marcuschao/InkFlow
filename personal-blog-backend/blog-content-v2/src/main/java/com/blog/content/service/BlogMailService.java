package com.blog.content.service;

import com.blog.content.config.properties.BlogSiteProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BlogMailService {

    private static final Logger log = LoggerFactory.getLogger(BlogMailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private MailProperties mailProperties;

    @Autowired
    private BlogSiteProperties blogSiteProperties;

    public void sendIfConfigured(String to, String subject, String text) {
        if (!blogSiteProperties.isNotifyMailEnabled()) {
            log.debug("[mail off] to={} subject={}", to, subject);
            return;
        }
        if (mailSender == null) {
            log.warn("[mail skipped] JavaMailSender absent; set spring.mail.host to enable SMTP");
            return;
        }
        String from = resolveFrom();
        if (!StringUtils.hasText(from)) {
            log.warn("[mail skipped] set blog.notify-from or spring.mail.username");
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
