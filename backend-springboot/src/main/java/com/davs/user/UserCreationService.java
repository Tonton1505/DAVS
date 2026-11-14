package com.davs.user;

import com.davs.audit.AuditEvent;
import com.davs.audit.AuditService;
import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.security.DavsUser;
import com.davs.common.security.SiteRole;
import com.davs.common.service.SettingKey;
import com.davs.common.service.SettingsService;
import com.davs.job.JobQueueService;
import com.davs.notification.EmailTemplate;
import com.davs.notification.NotificationService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Core UC02 business logic for creating users individually/in batch and queueing CSV imports.
 */
@Service
@RequiredArgsConstructor
public class UserCreationService {

    public static final String IMPORT_USERS_JOB_TYPE = "IMPORT_USERS";
    private static final long MAX_CSV_BYTES = 1_048_576L;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int INVITATION_TTL_DAYS = 7;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger LOG = LoggerFactory.getLogger(UserCreationService.class);

    private final JdbcUserCreationRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SettingsService settingsService;
    private final JobQueueService jobQueueService;

    @Transactional
    public CreateUsersResponse createUsers(CreateUsersRequest request, DavsUser actor) {
        List<String> normalized = normalizeAndValidateUsernames(request.usernames());

        List<String> toCreate = new ArrayList<>();
        List<CreateUsersResponse.SkippedUserItem> skipped = new ArrayList<>();
        Set<String> seenInBatch = new LinkedHashSet<>();

        for (String email : normalized) {
            if (!seenInBatch.add(email.toLowerCase())) {
                skipped.add(new CreateUsersResponse.SkippedUserItem(
                        email,
                        ErrorCode.ERR_USR_02.getCode(),
                        ErrorCode.ERR_USR_02.formatMessage(email)
                ));
                continue;
            }
            if (repository.existsActiveEmail(email)) {
                skipped.add(new CreateUsersResponse.SkippedUserItem(
                        email,
                        ErrorCode.ERR_USR_02.getCode(),
                        ErrorCode.ERR_USR_02.formatMessage(email)
                ));
            } else {
                toCreate.add(email);
            }
        }

        // [6b] If every submitted username already exists, return 409 ERR-USR-02
        if (toCreate.isEmpty() && !skipped.isEmpty()) {
            throw new DavsException(ErrorCode.ERR_USR_02, skipped.get(0).email());
        }

        // [6c] BUS-USR-01: ensure license pool can accommodate all new users for the role
        if (!repository.reserveLicenses(request.role(), toCreate.size())) {
            throw new DavsException(ErrorCode.ERR_USR_04, request.role().name());
        }

        List<CreateUsersResponse.CreatedUserItem> created = new ArrayList<>();
        int uninvitedCount = 0;

        for (String email : toCreate) {
            ProvisionResult provisioned = provisionSingleUser(
                    email,
                    null,
                    request.authMethod(),
                    request.role(),
                    actor.getId()
            );
            if ("UNINVITED".equals(provisioned.status())) {
                uninvitedCount++;
            }
            created.add(new CreateUsersResponse.CreatedUserItem(provisioned.userId(), email, provisioned.status()));
            auditService.log(AuditEvent.of(
                            "USER_CREATE",
                            actor.getId(),
                            actor.getEmail(),
                            "USER",
                            provisioned.userId().toString())
                    .withStates(null, Map.of(
                            "email", email,
                            "siteRole", request.role().name(),
                            "authMethod", request.authMethod().name(),
                            "status", provisioned.status()
                    )));
        }

        return new CreateUsersResponse(created.size(), skipped.size(), uninvitedCount, created, skipped);
    }

    @Transactional
    public ImportJobResponse enqueueCsvImport(
            MultipartFile file,
            AuthMethod authMethod,
            SiteRole defaultRole,
            DavsUser actor
    ) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_CSV_BYTES) {
            throw new DavsException(ErrorCode.ERR_USR_03, 1);
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!originalName.endsWith(".csv")) {
            throw new DavsException(ErrorCode.ERR_USR_03, 1);
        }

        String csvContent;
        try {
            csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new DavsException(ErrorCode.ERR_USR_03, 1);
        }

        List<String> lines = extractDataLines(csvContent);
        int maxRows = Math.max(1, settingsService.getInt(SettingKey.IMPORT_MAX_USERS_PER_FILE));
        if (lines.isEmpty() || lines.size() > maxRows) {
            throw new DavsException(ErrorCode.ERR_USR_03, Math.max(1, lines.size()));
        }

        UUID jobId = jobQueueService.enqueue(
                IMPORT_USERS_JOB_TYPE,
                Map.of(
                        "authMethod", authMethod.name(),
                        "defaultRole", (defaultRole != null ? defaultRole : SiteRole.VIEWER).name(),
                        "csvContent", csvContent,
                        "actorId", actor.getId().toString(),
                        "actorEmail", actor.getEmail()
                ),
                Instant.now(),
                1,
                actor.getId()
        );
        repository.createImportJobRecord(jobId, authMethod, lines.size(), actor.getId());

        auditService.log(AuditEvent.of(
                "USER_IMPORT_ENQUEUED",
                actor.getId(),
                actor.getEmail(),
                "JOB",
                jobId.toString()
        ).withStates(null, Map.of("totalRows", lines.size(), "authMethod", authMethod.name())));

        return new ImportJobResponse(jobId);
    }

    @Transactional(readOnly = true)
    public Optional<ImportJobStatusResponse> getImportStatus(UUID jobId) {
        return repository.findImportJobStatus(jobId);
    }

    @Transactional(readOnly = true)
    public Optional<String> getImportErrorReportCsv(UUID jobId) {
        return repository.findImportErrorCsv(jobId);
    }

    @Transactional(readOnly = true)
    public List<LicensePoolDto> listLicensePools() {
        return repository.listLicensePools();
    }

    public ProvisionResult provisionSingleUser(
            String email,
            String fullName,
            AuthMethod authMethod,
            SiteRole role,
            UUID actorId
    ) {
        String randomPasswordHash = passwordEncoder.encode(generateOpaqueToken());
        UUID userId = repository.insertInvitedUser(
                email.trim(),
                randomPasswordHash,
                fullName != null && !fullName.isBlank() ? fullName.trim() : null,
                authMethod,
                role,
                actorId
        );
        String rawInviteToken = generateOpaqueToken();
        Instant expiresAt = Instant.now().plus(INVITATION_TTL_DAYS, ChronoUnit.DAYS);
        repository.insertInvitation(userId, sha256Hex(rawInviteToken), expiresAt, actorId);

        String status = "PENDING";
        try {
            notificationService.send(
                    EmailTemplate.INVITE,
                    email.trim(),
                    Map.of(
                            "email", email.trim(),
                            "role", role.name(),
                            "inviteToken", rawInviteToken,
                            "expiresDays", INVITATION_TTL_DAYS
                    )
            );
        } catch (Exception ex) {
            // [7a] Invite mail failed -> mark UNINVITED so admin can resend later in UC03
            LOG.warn("Failed to send invitation email to {}: {}", email, ex.getMessage());
            repository.updateUserStatus(userId, "UNINVITED");
            status = "UNINVITED";
        }
        return new ProvisionResult(userId, status);
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        return !trimmed.isEmpty()
                && trimmed.length() <= MAX_EMAIL_LENGTH
                && EMAIL_PATTERN.matcher(trimmed).matches();
    }

    public static List<String> extractDataLines(String csvContent) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            boolean firstNonEmpty = true;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.replace("\uFEFF", "").trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (firstNonEmpty) {
                    firstNonEmpty = false;
                    if (trimmed.toLowerCase().startsWith("email")) {
                        continue;
                    }
                }
                lines.add(trimmed);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed reading CSV content", ex);
        }
        return lines;
    }

    private List<String> normalizeAndValidateUsernames(List<String> usernames) {
        List<String> result = new ArrayList<>();
        for (String raw : usernames) {
            String trimmed = raw == null ? "" : raw.trim();
            if (!isValidEmail(trimmed)) {
                throw new DavsException(ErrorCode.ERR_USR_01, trimmed);
            }
            result.add(trimmed);
        }
        return result;
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public record ProvisionResult(UUID userId, String status) {
    }
}
