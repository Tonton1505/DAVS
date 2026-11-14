package vn.edu.davs.backend.service;

import vn.edu.davs.backend.enu.EmailTemplate;

import vn.edu.davs.backend.model.JobRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link JobHandler} that delivers queued {@code SEND_EMAIL} jobs through {@link NotificationService}.
 */
@Component
@RequiredArgsConstructor
public class SendEmailJobHandler implements JobHandler {

    public static final String JOB_TYPE = "SEND_EMAIL";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public String type() {
        return JOB_TYPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(JobRecord job) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(job.payloadJson(), MAP_TYPE);
        EmailTemplate template = EmailTemplate.valueOf(String.valueOf(payload.get("template")));
        String to = String.valueOf(payload.get("to"));
        Object rawModel = payload.get("model");
        Map<String, Object> model = rawModel instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        notificationService.send(template, to, model);
    }
}
