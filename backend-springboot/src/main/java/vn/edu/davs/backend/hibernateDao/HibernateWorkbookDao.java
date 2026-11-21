package vn.edu.davs.backend.hibernateDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.common.Member2Constants;
import vn.edu.davs.backend.dao.LabelDao;
import vn.edu.davs.backend.dao.WorkbookDao;
import vn.edu.davs.backend.dto.LabelDto;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.entity.WorkbookEntity;
import vn.edu.davs.backend.entity.WorkbookVersionEntity;
import vn.edu.davs.backend.util.HibernateSessionUtil;

/**
 * Hibernate Session implementation of {@link WorkbookDao} for UC08 &amp; UC10.
 */
@Repository
@Transactional
@RequiredArgsConstructor
public class HibernateWorkbookDao implements WorkbookDao {

    @PersistenceContext
    private EntityManager entityManager;

    private final LabelDao labelDao;

    private Session session() {
        return HibernateSessionUtil.currentSession(entityManager);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkbookRow> findActiveByProjectAndName(UUID projectId, String name) {
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT w.id, w.project_id, p.name AS project_name, w.name, w.description,
                       w.current_version, w.owner_id,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS owner_name,
                       w.import_status, w.needs_reconnect, w.created_at, w.updated_at,
                       COALESCE(v.size_bytes, 0) AS size_bytes,
                       COALESCE(v.file_name, w.name || '.xlsx') AS file_name
                FROM workbooks w
                INNER JOIN projects p ON p.id = w.project_id
                LEFT JOIN users u ON u.id = w.owner_id
                LEFT JOIN workbook_versions v
                  ON v.workbook_id = w.id AND v.version_number = w.current_version
                WHERE w.project_id = :projectId AND w.deleted_at IS NULL AND lower(w.name) = lower(:name)
                """,
                Object[].class
        )
                .setParameter("projectId", projectId)
                .setParameter("name", name)
                .getResultList();
        return rows.stream().findFirst().map(HibernateWorkbookDao::mapWorkbookRow);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkbookRow> findById(UUID workbookId) {
        if (workbookId == null) {
            return Optional.empty();
        }
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT w.id, w.project_id, p.name AS project_name, w.name, w.description,
                       w.current_version, w.owner_id,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS owner_name,
                       w.import_status, w.needs_reconnect, w.created_at, w.updated_at,
                       COALESCE(v.size_bytes, 0) AS size_bytes,
                       COALESCE(v.file_name, w.name || '.xlsx') AS file_name
                FROM workbooks w
                INNER JOIN projects p ON p.id = w.project_id
                LEFT JOIN users u ON u.id = w.owner_id
                LEFT JOIN workbook_versions v
                  ON v.workbook_id = w.id AND v.version_number = w.current_version
                WHERE w.id = :workbookId AND w.deleted_at IS NULL
                """,
                Object[].class
        )
                .setParameter("workbookId", workbookId)
                .getResultList();
        return rows.stream().findFirst().map(HibernateWorkbookDao::mapWorkbookRow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkbookRow> listByProject(UUID projectId) {
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT w.id, w.project_id, p.name AS project_name, w.name, w.description,
                       w.current_version, w.owner_id,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS owner_name,
                       w.import_status, w.needs_reconnect, w.created_at, w.updated_at,
                       COALESCE(v.size_bytes, 0) AS size_bytes,
                       COALESCE(v.file_name, w.name || '.xlsx') AS file_name
                FROM workbooks w
                INNER JOIN projects p ON p.id = w.project_id
                LEFT JOIN users u ON u.id = w.owner_id
                LEFT JOIN workbook_versions v
                  ON v.workbook_id = w.id AND v.version_number = w.current_version
                WHERE w.project_id = :projectId AND w.deleted_at IS NULL
                ORDER BY w.updated_at DESC, lower(w.name) ASC
                """,
                Object[].class
        )
                .setParameter("projectId", projectId)
                .getResultList();
        List<WorkbookRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(mapWorkbookRow(row));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkbookRow> listAllActive() {
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT w.id, w.project_id, p.name AS project_name, w.name, w.description,
                       w.current_version, w.owner_id,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS owner_name,
                       w.import_status, w.needs_reconnect, w.created_at, w.updated_at,
                       COALESCE(v.size_bytes, 0) AS size_bytes,
                       COALESCE(v.file_name, w.name || '.xlsx') AS file_name
                FROM workbooks w
                INNER JOIN projects p ON p.id = w.project_id
                LEFT JOIN users u ON u.id = w.owner_id
                LEFT JOIN workbook_versions v
                  ON v.workbook_id = w.id AND v.version_number = w.current_version
                WHERE w.deleted_at IS NULL AND p.deleted_at IS NULL
                ORDER BY w.updated_at DESC, lower(w.name) ASC
                """,
                Object[].class
        ).getResultList();
        List<WorkbookRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(mapWorkbookRow(row));
        }
        return result;
    }

    @Override
    public UUID insertWorkbook(
            UUID projectId,
            String name,
            String description,
            UUID ownerId,
            String definitionJson,
            boolean needsReconnect
    ) {
        Instant now = Instant.now();
        WorkbookEntity entity = new WorkbookEntity();
        entity.setProjectId(projectId);
        entity.setName(name);
        entity.setDescription(description);
        entity.setCurrentVersion(1);
        entity.setOwnerId(ownerId);
        entity.setDefinition(
                definitionJson != null ? definitionJson : Member2Constants.EMPTY_JSON_OBJECT
        );
        entity.setImportStatus("IMPORTED");
        entity.setNeedsReconnect(needsReconnect);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        Session s = session();
        s.persist(entity);
        s.flush();
        return entity.getId();
    }

    @Override
    public void updateWorkbookOnOverwrite(
            UUID workbookId,
            String description,
            int newVersionNumber,
            String definitionJson,
            boolean needsReconnect
    ) {
        Session s = session();
        WorkbookEntity entity = s.find(WorkbookEntity.class, workbookId);
        if (entity == null || entity.getDeletedAt() != null) {
            return;
        }
        if (description != null) {
            entity.setDescription(description);
        }
        entity.setCurrentVersion(newVersionNumber);
        if (definitionJson != null) {
            entity.setDefinition(definitionJson);
        }
        entity.setNeedsReconnect(needsReconnect);
        entity.setUpdatedAt(Instant.now());
        s.flush();
    }

    @Override
    public void updateCurrentVersion(UUID workbookId, int newVersionNumber, boolean needsReconnect) {
        session().createMutationQuery(
                "UPDATE WorkbookEntity w SET w.currentVersion = :versionNumber, "
                        + "w.needsReconnect = :needsReconnect, w.updatedAt = :now "
                        + "WHERE w.id = :id AND w.deletedAt IS NULL"
        )
                .setParameter("versionNumber", newVersionNumber)
                .setParameter("needsReconnect", needsReconnect)
                .setParameter("now", Instant.now())
                .setParameter("id", workbookId)
                .executeUpdate();
    }

    @Override
    public UUID insertVersion(
            UUID workbookId,
            int versionNumber,
            String objectKey,
            String fileName,
            long sizeBytes,
            String note,
            UUID createdBy
    ) {
        WorkbookVersionEntity entity = new WorkbookVersionEntity();
        entity.setWorkbookId(workbookId);
        entity.setVersionNumber(versionNumber);
        entity.setObjectKey(objectKey);
        entity.setFileName(fileName);
        entity.setSizeBytes(sizeBytes);
        entity.setNote(note);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(Instant.now());
        entity.setPurged(false);
        Session s = session();
        s.persist(entity);
        s.flush();
        return entity.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionRow> listVersions(UUID workbookId, int currentVersion) {
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT v.id, v.workbook_id, v.version_number, v.object_key, v.file_name,
                       v.size_bytes, v.note, v.created_by,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS created_by_name,
                       v.created_at, v.deleted_at, v.purged
                FROM workbook_versions v
                LEFT JOIN users u ON u.id = v.created_by
                WHERE v.workbook_id = :workbookId
                ORDER BY v.version_number DESC
                """,
                Object[].class
        )
                .setParameter("workbookId", workbookId)
                .getResultList();
        List<VersionRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(mapVersionRow(row, currentVersion));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VersionRow> findVersion(UUID workbookId, int versionNumber, int currentVersion) {
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT v.id, v.workbook_id, v.version_number, v.object_key, v.file_name,
                       v.size_bytes, v.note, v.created_by,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS created_by_name,
                       v.created_at, v.deleted_at, v.purged
                FROM workbook_versions v
                LEFT JOIN users u ON u.id = v.created_by
                WHERE v.workbook_id = :workbookId AND v.version_number = :versionNumber
                """,
                Object[].class
        )
                .setParameter("workbookId", workbookId)
                .setParameter("versionNumber", versionNumber)
                .getResultList();
        return rows.stream().findFirst().map(row -> mapVersionRow(row, currentVersion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionRow> findActiveVersionsExceedingLimit(
            UUID workbookId,
            int currentVersion,
            int maxVersions
    ) {
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT v.id, v.workbook_id, v.version_number, v.object_key, v.file_name,
                       v.size_bytes, v.note, v.created_by,
                       COALESCE(NULLIF(u.full_name, ''), u.email, 'Hệ thống') AS created_by_name,
                       v.created_at, v.deleted_at, v.purged
                FROM workbook_versions v
                LEFT JOIN users u ON u.id = v.created_by
                WHERE v.workbook_id = :workbookId AND v.deleted_at IS NULL AND v.purged = FALSE
                ORDER BY v.version_number DESC
                """,
                Object[].class
        )
                .setParameter("workbookId", workbookId)
                .setFirstResult(Math.max(1, maxVersions))
                .getResultList();
        List<VersionRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(mapVersionRow(row, currentVersion));
        }
        return result;
    }

    @Override
    public void markVersionPurged(UUID versionId) {
        session().createMutationQuery(
                "UPDATE WorkbookVersionEntity v SET v.purged = true, v.deletedAt = :now WHERE v.id = :id"
        )
                .setParameter("now", Instant.now())
                .setParameter("id", versionId)
                .executeUpdate();
    }

    @Override
    public void deleteVersionRow(UUID versionId) {
        session().createMutationQuery("DELETE FROM WorkbookVersionEntity v WHERE v.id = :id")
                .setParameter("id", versionId)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public int countOtherVersionsUsingObjectKey(
            UUID workbookId,
            String objectKey,
            UUID excludeVersionId
    ) {
        Long count = session()
                .createSelectionQuery(
                        "SELECT COUNT(v) FROM WorkbookVersionEntity v "
                                + "WHERE v.workbookId = :wbId AND v.objectKey = :objectKey "
                                + "AND v.id <> :excludeId AND v.deletedAt IS NULL AND v.purged = false",
                        Long.class
                )
                .setParameter("wbId", workbookId)
                .setParameter("objectKey", objectKey)
                .setParameter("excludeId", excludeVersionId)
                .uniqueResult();
        return count != null ? count.intValue() : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkbookDto toDto(WorkbookRow row) {
        List<LabelDto> labels = labelDao.listLabelsForTarget(Member2Constants.RESOURCE_WORKBOOK, row.id());
        return new WorkbookDto(
                row.id(),
                row.projectId(),
                row.projectName(),
                row.name(),
                row.description(),
                row.currentVersion(),
                row.ownerId(),
                row.ownerName(),
                row.importStatus(),
                row.needsReconnect(),
                row.sizeBytes(),
                row.fileName(),
                labels,
                row.createdAt(),
                row.updatedAt()
        );
    }

    private static WorkbookRow mapWorkbookRow(Object[] row) {
        return new WorkbookRow(
                HibernateSessionUtil.toUuid(row[0]),
                HibernateSessionUtil.toUuid(row[1]),
                row[2] != null ? String.valueOf(row[2]) : null,
                row[3] != null ? String.valueOf(row[3]) : null,
                row[4] != null ? String.valueOf(row[4]) : null,
                HibernateSessionUtil.toInt(row[5]),
                HibernateSessionUtil.toUuid(row[6]),
                row[7] != null ? String.valueOf(row[7]) : Member2Constants.DEFAULT_SYSTEM_OWNER_NAME,
                row[8] != null ? String.valueOf(row[8]) : null,
                HibernateSessionUtil.toBoolean(row[9]),
                HibernateSessionUtil.toLong(row[12]),
                row[13] != null ? String.valueOf(row[13]) : null,
                HibernateSessionUtil.toInstant(row[10]),
                HibernateSessionUtil.toInstant(row[11])
        );
    }

    private static VersionRow mapVersionRow(Object[] row, int currentVersion) {
        int vNum = HibernateSessionUtil.toInt(row[2]);
        boolean isPurged = HibernateSessionUtil.toBoolean(row[11]) || row[10] != null;
        return new VersionRow(
                HibernateSessionUtil.toUuid(row[0]),
                HibernateSessionUtil.toUuid(row[1]),
                vNum,
                row[3] != null ? String.valueOf(row[3]) : null,
                row[4] != null ? String.valueOf(row[4]) : null,
                HibernateSessionUtil.toLong(row[5]),
                row[6] != null ? String.valueOf(row[6]) : null,
                HibernateSessionUtil.toUuid(row[7]),
                row[8] != null ? String.valueOf(row[8]) : Member2Constants.DEFAULT_SYSTEM_OWNER_NAME,
                HibernateSessionUtil.toInstant(row[9]),
                vNum == currentVersion,
                isPurged
        );
    }
}
