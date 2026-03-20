package com.halleyx.workflow_engine.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.mail.from-name:Workflow Engine}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    @Async
    public void sendPlainText(String toAddress, String toName,
                              String subject, String body) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");

            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(new InternetAddress(toAddress, toName));
            helper.setSubject(subject);
            helper.setText(body, false);   // false = plain text

            mailSender.send(msg);
            log.info("[Email] Sent to {} <{}>: {}", toName, toAddress, subject);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("[Email] Failed to send to {}: {}", toAddress, e.getMessage());
        }
    }
}