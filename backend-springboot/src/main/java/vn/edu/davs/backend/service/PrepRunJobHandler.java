package vn.edu.davs.backend.service;

import vn.edu.davs.backend.model.JobRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Background {@link JobHandler} executing {@code PREP_RUN} jobs (UC12).
 */
@Component
@RequiredArgsConstructor
public class PrepRunJobHandler implements JobHandler {

    private final DataPrepService dataPrepService;
    private final ObjectMapper objectMapper;

    @Override
    public String type() {
        return DataPrepService.PREP_RUN_JOB_TYPE;
    }

    @Override
    public void handle(JobRecord job) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(
                job.payloadJson(),
                new TypeReference<Map<String, Object>>() { }
        );
        UUID runId = UUID.fromString(String.valueOf(payload.get("runId")));
        boolean simulateFailure = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("simulateFailure", false)));
        dataPrepService.executeRun(runId, simulateFailure);
    }
}
