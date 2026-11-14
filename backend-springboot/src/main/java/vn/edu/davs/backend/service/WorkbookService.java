package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.JdbcProjectRepository;
import vn.edu.davs.backend.dao.JdbcWorkbookRepository;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.dto.RestoreWorkbookVersionRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.dto.WorkbookVersionDto;
import vn.edu.davs.backend.enu.Bucket;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SettingKey;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.exception.FieldErrorItem;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import vn.edu.davs.backend.model.StoredObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Core business logic for UC08 Workbook Upload and UC10 Workbook Version History.
 */
@Service
@RequiredArgsConstructor
public class WorkbookService {

    private static final int MAX_WORKBOOK_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_RESTORE_NOTE_LENGTH = 250;

    private final JdbcWorkbookRepository workbookRepository;
    private final JdbcProjectRepository projectRepository;
    private final ProjectService projectService;
    private final PermissionService permissionService;
    private final StorageService storageService;
    private final SettingsService settingsService;
    private final AuditService auditService;

    @Transactional
    public WorkbookDto uploadWorkbook(
            UUID projectId,
            String name,
            String description,
             boolean overwrite,
            String note,
            MultipartFile file,
            DavsUser actor
    ) {
        if (actor == null) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        ProjectDto project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
        if (!projectService.canPublishToProject(actor, project)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        if (file == null || file.isEmpty()) {
            throw new DavsException(ErrorCode.ERR_PRJ_04);
        }
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().trim() : "";
        String lowerFilename = originalFilename.toLowerCase(Locale.ROOT);
        if (!lowerFilename.endsWith(".twb") && !lowerFilename.endsWith(".twbx")) {
            throw new DavsException(ErrorCode.ERR_PRJ_04);
        }

        String resolvedName = (name != null && !name.isBlank())
                ? name.trim()
                : stripExtension(originalFilename);
        if (resolvedName.isEmpty() || resolvedName.length() > MAX_WORKBOOK_NAME_LENGTH) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("name", "Tên workbook phải từ 1 đến 255 ký tự"))
            );
        }

        String normalizedDesc = normalizeDescription(description);
        Optional<JdbcWorkbookRepository.WorkbookRow> existingOpt =
                workbookRepository.findActiveByProjectAndName(projectId, resolvedName);
        if (existingOpt.isPresent() && !overwrite) {
            throw new DavsException(ErrorCode.ERR_PRJ_06);
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            throw new DavsException(ErrorCode.ERR_PRJ_04, ex);
        }

        StoredObject stored;
        try {
            stored = storageService.put(
                    Bucket.WORKBOOKS,
                    "projects/" + projectId,
                    originalFilename,
                    file.getContentType(),
                    new ByteArrayInputStream(fileBytes),
                    fileBytes.length
            );
        } catch (DavsException ex) {
            if (ex.getErrorCode() == ErrorCode.ERR_PRJ_08) {
                auditService.log(AuditEvent.of(
                        "WORKBOOK_UPLOAD_REJECTED",
                        actor.getId(),
                        actor.getEmail(),
                        "PROJECT",
                        projectId.toString()
                ).withStates(null, Map.of("fileName", originalFilename, "reason", "VIRUS_SCAN_FAILED")));
            }
            throw ex;
        }

        boolean needsReconnect = detectNeedsReconnect(fileBytes);
        String definitionJson = "{\"fileName\":\"" + stored.sanitizedFileName() + "\"}";
        String versionNote = (note != null && !note.isBlank())
                ? note.trim()
                : (existingOpt.isPresent() ? "Ghi đè phiên bản mới" : "Phiên bản tải lên ban đầu");

        UUID workbookId;
        int versionNumber;
        if (existingOpt.isPresent()) {
            JdbcWorkbookRepository.WorkbookRow existing = existingOpt.get();
            workbookId = existing.id();
            versionNumber = existing.currentVersion() + 1;
            workbookRepository.updateWorkbookOnOverwrite(
                    workbookId,
                    normalizedDesc,
                    versionNumber,
                    definitionJson,
                    needsReconnect
            );
            workbookRepository.insertVersion(
                    workbookId,
                    versionNumber,
                    stored.objectKey(),
                    stored.sanitizedFileName(),
                    stored.sizeBytes(),
                    versionNote,
                    actor.getId()
            );
            enforceVersionRetention(workbookId, versionNumber);
        } else {
            workbookId = workbookRepository.insertWorkbook(
                    projectId,
                    resolvedName,
                    normalizedDesc,
                    actor.getId(),
                    definitionJson,
                    needsReconnect
            );
            versionNumber = 1;
            workbookRepository.insertVersion(
                    workbookId,
                    versionNumber,
                    stored.objectKey(),
                    stored.sanitizedFileName(),
                    stored.sizeBytes(),
                    versionNote,
                    actor.getId()
            );
        }

        JdbcWorkbookRepository.WorkbookRow savedRow = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new IllegalStateException("Workbook not found after upload: " + workbookId));

        auditService.log(AuditEvent.of(
                "WORKBOOK_UPLOAD",
                actor.getId(),
                actor.getEmail(),
                "WORKBOOK",
                workbookId.toString()
        ).withStates(null, Map.of(
                "name", savedRow.name(),
                "projectId", projectId.toString(),
                "version", versionNumber,
                "overwrite", existingOpt.isPresent(),
                "sizeBytes", stored.sizeBytes()
        )));

        return workbookRepository.toDto(savedRow);
    }

    @Transactional(readOnly = true)
    public WorkbookDto getWorkbook(UUID workbookId, DavsUser actor) {
        JdbcWorkbookRepository.WorkbookRow row = getWorkbookRowOrThrow(workbookId);
        requireViewPermission(actor, row);
        return workbookRepository.toDto(row);
    }

    @Transactional(readOnly = true)
    public List<WorkbookDto> listProjectWorkbooks(UUID projectId, DavsUser actor) {
        ProjectDto project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
        if (!projectService.canViewProject(actor, project)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        return workbookRepository.listByProject(projectId).stream()
                .filter(row -> canViewWorkbook(actor, row))
                .map(workbookRepository::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkbookVersionDto> listVersions(UUID workbookId, DavsUser actor) {
        JdbcWorkbookRepository.WorkbookRow row = getWorkbookRowOrThrow(workbookId);
        requireViewPermission(actor, row);
        return workbookRepository.listVersions(workbookId, row.currentVersion())
                .stream()
                .map(JdbcWorkbookRepository.VersionRow::toDto)
                .toList();
    }

    @Transactional
    public WorkbookVersionDto restoreVersion(
            UUID workbookId,
            int versionNumber,
            RestoreWorkbookVersionRequest request,
            DavsUser actor
    ) {
        JdbcWorkbookRepository.WorkbookRow workbook = getWorkbookRowOrThrow(workbookId);
        requireWritePermission(actor, workbook);

        String note = request != null && request.note() != null ? request.note().trim() : "";
        if (note.isEmpty() || note.length() > MAX_RESTORE_NOTE_LENGTH) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("note", "Ghi chú khôi phục phải từ 1 đến 250 ký tự"))
            );
        }

        JdbcWorkbookRepository.VersionRow targetVersion = workbookRepository
                .findVersion(workbookId, versionNumber, workbook.currentVersion())
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_09));
        if (targetVersion.purged()) {
            throw new DavsException(ErrorCode.ERR_PRJ_09);
        }

        int newVersionNumber = workbook.currentVersion() + 1;
        workbookRepository.insertVersion(
                workbookId,
                newVersionNumber,
                targetVersion.objectKey(),
                targetVersion.fileName(),
                targetVersion.sizeBytes(),
                note,
                actor.getId()
        );
        workbookRepository.updateCurrentVersion(workbookId, newVersionNumber, workbook.needsReconnect());
        enforceVersionRetention(workbookId, newVersionNumber);

        auditService.log(AuditEvent.of(
                "WORKBOOK_VERSION_RESTORE",
                actor.getId(),
                actor.getEmail(),
                "WORKBOOK",
                workbookId.toString()
        ).withStates(
                Map.of("previousVersion", workbook.currentVersion()),
                Map.of("restoredFromVersion", versionNumber, "newVersion", newVersionNumber, "note", note)
        ));

        return workbookRepository.findVersion(workbookId, newVersionNumber, newVersionNumber)
                .map(JdbcWorkbookRepository.VersionRow::toDto)
                .orElseThrow(() -> new IllegalStateException("Restored version not found: " + newVersionNumber));
    }

    @Transactional
    public DownloadedVersion downloadVersion(UUID workbookId, int versionNumber, DavsUser actor) {
        JdbcWorkbookRepository.WorkbookRow workbook = getWorkbookRowOrThrow(workbookId);
        requireDownloadPermission(actor, workbook);

        JdbcWorkbookRepository.VersionRow version = workbookRepository
                .findVersion(workbookId, versionNumber, workbook.currentVersion())
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_09));
        if (version.purged()) {
            throw new DavsException(ErrorCode.ERR_PRJ_09);
        }

        byte[] content;
        try (InputStream in = storageService.get(Bucket.WORKBOOKS, version.objectKey())) {
            content = in != null
                    ? in.readAllBytes()
                    : "<workbook/>".getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            content = "<workbook/>".getBytes(StandardCharsets.UTF_8);
        }

        auditService.log(AuditEvent.of(
                "WORKBOOK_VERSION_DOWNLOAD",
                actor.getId(),
                actor.getEmail(),
                "WORKBOOK",
                workbookId.toString()
        ).withStates(null, Map.of("versionNumber", versionNumber, "fileName", version.fileName())));

        return new DownloadedVersion(version.fileName(), content);
    }

    @Transactional
    public void deleteVersion(UUID workbookId, int versionNumber, DavsUser actor) {
        JdbcWorkbookRepository.WorkbookRow workbook = getWorkbookRowOrThrow(workbookId);
        requireWritePermission(actor, workbook);

        if (versionNumber == workbook.currentVersion()) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("version", "Không thể xóa phiên bản hiện hành của workbook"))
            );
        }

        JdbcWorkbookRepository.VersionRow version = workbookRepository
                .findVersion(workbookId, versionNumber, workbook.currentVersion())
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_09));
        if (version.purged()) {
            throw new DavsException(ErrorCode.ERR_PRJ_09);
        }

        workbookRepository.deleteVersionRow(version.id());
        if (workbookRepository.countOtherVersionsUsingObjectKey(workbookId, version.objectKey(), version.id()) == 0) {
            try {
                storageService.delete(Bucket.WORKBOOKS, version.objectKey());
            } catch (Exception ignored) {
                // Ignore storage cleanup error if object was already removed
            }
        }

        auditService.log(AuditEvent.of(
                "WORKBOOK_VERSION_DELETE",
                actor.getId(),
                actor.getEmail(),
                "WORKBOOK",
                workbookId.toString()
        ).withStates(Map.of("deletedVersion", versionNumber), null));
    }

    private void enforceVersionRetention(UUID workbookId, int currentVersion) {
        int maxVersions = settingsService.getInt(SettingKey.WORKBOOK_MAX_VERSIONS);
        List<JdbcWorkbookRepository.VersionRow> excess = workbookRepository
                .findActiveVersionsExceedingLimit(workbookId, currentVersion, maxVersions);
        for (JdbcWorkbookRepository.VersionRow oldVersion : excess) {
            if (oldVersion.versionNumber() == currentVersion) {
                continue;
            }
            workbookRepository.markVersionPurged(oldVersion.id());
            if (workbookRepository.countOtherVersionsUsingObjectKey(
                    workbookId,
                    oldVersion.objectKey(),
                    oldVersion.id()
            ) == 0) {
                try {
                    storageService.delete(Bucket.WORKBOOKS, oldVersion.objectKey());
                } catch (Exception ignored) {
                    // Best-effort MinIO cleanup
                }
            }
        }
    }

    private JdbcWorkbookRepository.WorkbookRow getWorkbookRowOrThrow(UUID workbookId) {
        return workbookRepository.findById(workbookId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
    }

    private boolean canViewWorkbook(DavsUser actor, JdbcWorkbookRepository.WorkbookRow row) {
        if (actor == null) {
            return false;
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (row.ownerId() != null && row.ownerId().equals(actor.getId()))) {
            return true;
        }
        ProjectDto parentProject = projectRepository.findById(row.projectId()).orElse(null);
        if (parentProject != null && parentProject.isDefault()) {
            return true;
        }
        return permissionService == null
                || permissionService.can(actor, Capability.VIEW, ResourceRef.workbook(row.id()));
    }

    private void requireViewPermission(DavsUser actor, JdbcWorkbookRepository.WorkbookRow row) {
        if (!canViewWorkbook(actor, row)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
    }

    private void requireDownloadPermission(DavsUser actor, JdbcWorkbookRepository.WorkbookRow row) {
        if (actor == null || !actor.getSiteRole().supports(Capability.DOWNLOAD)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (row.ownerId() != null && row.ownerId().equals(actor.getId()))) {
            return;
        }
        ProjectDto parentProject = projectRepository.findById(row.projectId()).orElse(null);
        if (parentProject != null && parentProject.isDefault()) {
            return;
        }
        if (permissionService != null
                && permissionService.can(actor, Capability.DOWNLOAD, ResourceRef.workbook(row.id()))) {
            return;
        }
        throw new DavsException(ErrorCode.ERR_PRJ_03);
    }

    private void requireWritePermission(DavsUser actor, JdbcWorkbookRepository.WorkbookRow row) {
        if (actor == null || !actor.getSiteRole().supports(Capability.SAVE)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN
                || (row.ownerId() != null && row.ownerId().equals(actor.getId()))) {
            return;
        }
        if (permissionService != null
                && (permissionService.can(actor, Capability.SAVE, ResourceRef.workbook(row.id()))
                || permissionService.can(actor, Capability.PUBLISH, ResourceRef.project(row.projectId())))) {
            return;
        }
        throw new DavsException(ErrorCode.ERR_PRJ_03);
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot).trim() : fileName.trim();
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("description", "Mô tả tối đa 500 ký tự"))
            );
        }
        return trimmed;
    }

    private static boolean detectNeedsReconnect(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            return false;
        }
        String sample = new String(
                fileBytes,
                0,
                Math.min(fileBytes.length, 4096),
                StandardCharsets.UTF_8
        ).toLowerCase(Locale.ROOT);
        return sample.contains("missing-datasource") || sample.contains("needs-reconnect");
    }

    public record DownloadedVersion(String fileName, byte[] content) {
    }
}
