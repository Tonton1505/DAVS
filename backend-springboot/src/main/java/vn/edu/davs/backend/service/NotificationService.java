package vn.edu.davs.backend.service;

import vn.edu.davs.backend.config.MailProperties;
import vn.edu.davs.backend.enu.EmailTemplate;

import vn.edu.davs.backend.service.EmailTemplateRenderer.RenderedEmail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails synchronously via {@link JavaMailSender} or enqueues them on {@code jobs}.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final EmailTemplateRenderer templateRenderer;
    private final JobQueueService jobQueueService;

    /**
     * Renders {@code template} with {@code model} and sends it immediately to {@code to}.
     */
    public void send(EmailTemplate template, String to, Map<String, Object> model) {
        if (template == null) {
            throw new IllegalArgumentException("Email template must not be null");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email must not be blank");
        }

        RenderedEmail rendered = templateRenderer.render(template, model);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to.trim());
            helper.setSubject(rendered.subject());
            helper.setText(rendered.htmlBody(), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to build email message for " + to, ex);
        }
    }

    /**
     * Enqueues an email delivery job for asynchronous execution with automatic retries.
     */
    public UUID sendAsync(EmailTemplate template, String to, Map<String, Object> model) {
        if (template == null) {
            throw new IllegalArgumentException("Email template must not be null");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email must not be blank");
        }
        return jobQueueService.enqueue(
                SendEmailJobHandler.JOB_TYPE,
                Map.of(
                        "template", template.name(),
                        "to", to.trim(),
                        "model", model == null ? Map.of() : model));
    }
}
