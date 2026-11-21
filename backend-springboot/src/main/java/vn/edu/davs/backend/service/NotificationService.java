package vn.edu.davs.backend.service;

import vn.edu.davs.backend.enu.EmailTemplate;
import java.util.Map;
import java.util.UUID;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Sends transactional emails synchronously via {@link JavaMailSender} or enqueues them on {@code jobs}.
 */
public interface NotificationService {


    /**
     * Renders {@code template} with {@code model} and sends it immediately to {@code to}.
     */
    void send(EmailTemplate template, String to, Map<String, Object> model);


    /**
     * Enqueues an email delivery job for asynchronous execution with automatic retries.
     */
    UUID sendAsync(EmailTemplate template, String to, Map<String, Object> model);
}
