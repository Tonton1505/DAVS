package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.dto.WorkbookVersionDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC persistence for UC08 Workbook Upload and UC10 Workbook Version History.
 */
public interface IWorkbookDao {


    Optional<WorkbookRow> findActiveByProjectAndName(UUID projectId, String name);


    Optional<WorkbookRow> findById(UUID workbookId);

    default Optional<String> findDefinitionById(UUID workbookId) {
        return Optional.empty();
    }


    List<WorkbookRow> listByProject(UUID projectId);


    List<WorkbookRow> listAllActive();


    UUID insertWorkbook(
            UUID projectId,
            String name,
            String description,
            UUID ownerId,
            String definitionJson,
            boolean needsReconnect
    );


    void updateWorkbookOnOverwrite(
            UUID workbookId,
            String description,
            int newVersionNumber,
            String definitionJson,
            boolean needsReconnect
    );


    void updateCurrentVersion(UUID workbookId, int newVersionNumber, boolean needsReconnect);


    UUID insertVersion(
            UUID workbookId,
            int versionNumber,
            String objectKey,
            String fileName,
            long sizeBytes,
            String note,
            UUID createdBy
    );


    List<VersionRow> listVersions(UUID workbookId, int currentVersion);


    Optional<VersionRow> findVersion(UUID workbookId, int versionNumber, int currentVersion);


    List<VersionRow> findActiveVersionsExceedingLimit(
            UUID workbookId,
            int currentVersion,
            int maxVersions
    );


    void markVersionPurged(UUID versionId);


    void deleteVersionRow(UUID versionId);


    int countOtherVersionsUsingObjectKey(UUID workbookId, String objectKey, UUID excludeVersionId);


    WorkbookDto toDto(WorkbookRow row);


    public record WorkbookRow(
            UUID id,
            UUID projectId,
            String projectName,
            String name,
            String description,
            int currentVersion,
            UUID ownerId,
            String ownerName,
            String importStatus,
            boolean needsReconnect,
            long sizeBytes,
            String fileName,
            Instant createdAt,
            Instant updatedAt
    ) {
    }


    public record VersionRow(
            UUID id,
            UUID workbookId,
            int versionNumber,
            String objectKey,
            String fileName,
            long sizeBytes,
            String note,
            UUID createdBy,
            String createdByName,
            Instant createdAt,
            boolean current,
            boolean purged
    ) {
        public WorkbookVersionDto toDto() {
            return new WorkbookVersionDto(
                    id,
                    workbookId,
                    versionNumber,
                    fileName,
                    sizeBytes,
                    note,
                    createdBy,
                    createdByName,
                    createdAt,
                    current,
                    purged
            );
        }
    }
}
