package com.davs.user;

import com.davs.audit.AuditEvent;
import com.davs.audit.AuditService;
import com.davs.common.error.ErrorCode;
import com.davs.common.security.SiteRole;
import com.davs.job.JobHandler;
import com.davs.job.JobRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background {@link JobHandler} executing {@code IMPORT_USERS} CSV import jobs (UC02).
 */
@Component
@RequiredArgsConstructor
public class ImportUsersJobHandler implements JobHandler {

    private final JdbcUserCreationRepository repository;
    private final UserCreationService userCreationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    public String type() {
        return UserCreationService.IMPORT_USERS_JOB_TYPE;
    }

    @Override
    @Transactional
    public void handle(JobRecord job) throws Exception {
        repository.markImportJobRunning(job.id());

        Map<String, Object> payload = objectMapper.readValue(
                job.payloadJson(),
                new TypeReference<Map<String, Object>>() { }
        );
        AuthMethod authMethod = AuthMethod.valueOf(String.valueOf(payload.get("authMethod")));
        SiteRole defaultRole = SiteRole.valueOf(String.valueOf(payload.getOrDefault("defaultRole", "VIEWER")));
        String csvContent = String.valueOf(payload.getOrDefault("csvContent", ""));
        UUID actorId = payload.get("actorId") != null ? UUID.fromString(String.valueOf(payload.get("actorId"))) : null;
        String actorEmail = payload.get("actorEmail") != null ? String.valueOf(payload.get("actorEmail")) : null;

        List<String> dataLines = UserCreationService.extractDataLines(csvContent);
        StringBuilder errorCsv = new StringBuilder("line,email,error_code,message\n");

        int createdCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (int i = 0; i < dataLines.size(); i++) {
            int lineNumber = i + 1;
            String rawLine = dataLines.get(i);
            String[] cols = rawLine.split(",", -1);

            String email = cols.length > 0 ? cols[0].trim() : "";
            String fullName = cols.length > 2 ? cols[1].trim() : "";
            String roleStr = cols.length > 2 ? cols[2].trim() : (cols.length == 2 ? cols[1].trim() : "");

            if (!UserCreationService.isValidEmail(email)) {
                errorCount++;
                appendError(errorCsv, lineNumber, email, ErrorCode.ERR_USR_01,
                        ErrorCode.ERR_USR_01.formatMessage(email));
                continue;
            }

            SiteRole role = defaultRole;
            if (!roleStr.isEmpty()) {
                try {
                    role = SiteRole.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    errorCount++;
                    appendError(errorCsv, lineNumber, email, ErrorCode.ERR_USR_01,
                            "Vai trò không hợp lệ: " + roleStr);
                    continue;
                }
            }

            if (repository.existsActiveEmail(email)) {
                skippedCount++;
                errorCount++;
                appendError(errorCsv, lineNumber, email, ErrorCode.ERR_USR_02,
                        ErrorCode.ERR_USR_02.formatMessage(email));
                continue;
            }

            if (!repository.reserveLicenses(role, 1)) {
                errorCount++;
                appendError(errorCsv, lineNumber, email, ErrorCode.ERR_USR_04,
                        ErrorCode.ERR_USR_04.formatMessage(role.name()));
                continue;
            }

            userCreationService.provisionSingleUser(email, fullName, authMethod, role, actorId);
            createdCount++;
        }

        String finalCsv = errorCount > 0 ? errorCsv.toString() : null;
        repository.completeImportJob(job.id(), "SUCCEEDED", createdCount, skippedCount, errorCount, finalCsv);

        auditService.log(AuditEvent.of("USER_IMPORT_COMPLETED", actorId, actorEmail, "JOB", job.id().toString())
                .withStates(null, Map.of(
                        "totalRows", dataLines.size(),
                        "createdCount", createdCount,
                        "skippedCount", skippedCount,
                        "errorCount", errorCount
                )));
    }

    private static void appendError(
            StringBuilder sb,
            int lineNumber,
            String email,
            ErrorCode code,
            String message
    ) {
        sb.append(lineNumber)
                .append(',')
                .append(escapeCsv(email))
                .append(',')
                .append(escapeCsv(code.getCode()))
                .append(',')
                .append(escapeCsv(message))
                .append('\n');
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
