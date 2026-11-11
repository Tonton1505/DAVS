package com.davs.notification;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public NotificationService(
            JavaMailSender mailSender,
            @Value("${davs.mail.from:noreply@davs.local}") String fromEmail
    ) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender must not be null");
        this.fromEmail = fromEmail;
    }

    public void send(EmailTemplate template, String to, Object... modelArgs) {
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(to, "recipient 'to' must not be null");

        String body = template.formatBody(modelArgs);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(template.getDefaultSubject());
        message.setText(body);

        mailSender.send(message);
    }
}
