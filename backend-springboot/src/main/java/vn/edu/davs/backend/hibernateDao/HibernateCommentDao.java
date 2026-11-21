package vn.edu.davs.backend.hibernateDao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.common.Member2Constants;
import vn.edu.davs.backend.dao.CommentDao;
import vn.edu.davs.backend.dto.CommentDto;
import vn.edu.davs.backend.entity.CommentEntity;
import vn.edu.davs.backend.util.HibernateSessionUtil;

/**
 * Hibernate Session implementation of {@link CommentDao} for UC24.
 */
@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class HibernateCommentDao implements CommentDao {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    private Session session() {
        return HibernateSessionUtil.currentSession(entityManager);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommentDto> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT c.id, c.resource_type, c.resource_id, c.parent_id, c.author_id,
                       u.full_name AS author_name, u.email AS author_email,
                       c.text, c.mentions::text, c.snapshot_url, c.created_at, c.updated_at
                FROM comments c
                JOIN users u ON u.id = c.author_id
                WHERE c.id = :id AND c.deleted_at IS NULL
                """,
                Object[].class
        )
                .setParameter("id", id)
                .getResultList();
        return rows.stream().findFirst().map(row -> mapRow(row, null, false));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> findTreeByResource(
            String resourceType,
            UUID resourceId,
            UUID currentUserId,
            boolean isAdminOrOwner
    ) {
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT c.id, c.resource_type, c.resource_id, c.parent_id, c.author_id,
                       u.full_name AS author_name, u.email AS author_email,
                       c.text, c.mentions::text, c.snapshot_url, c.created_at, c.updated_at
                FROM comments c
                JOIN users u ON u.id = c.author_id
                WHERE c.resource_type = :resType AND c.resource_id = :resId AND c.deleted_at IS NULL
                ORDER BY c.created_at ASC
                """,
                Object[].class
        )
                .setParameter("resType", resourceType)
                .setParameter("resId", resourceId)
                .getResultList();

        List<CommentDto> all = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            all.add(mapRow(row, currentUserId, isAdminOrOwner));
        }

        Map<UUID, List<CommentDto>> replyMap = new HashMap<>();
        List<CommentDto> topLevel = new ArrayList<>();

        for (CommentDto c : all) {
            if (c.parentId() == null) {
                topLevel.add(c);
            } else {
                replyMap.computeIfAbsent(c.parentId(), k -> new ArrayList<>()).add(c);
            }
        }

        return topLevel.stream()
                .map(parent -> new CommentDto(
                        parent.id(),
                        parent.resourceType(),
                        parent.resourceId(),
                        parent.parentId(),
                        parent.authorId(),
                        parent.authorName(),
                        parent.authorEmail(),
                        parent.text(),
                        parent.mentions(),
                        parent.snapshotUrl(),
                        parent.editable(),
                        parent.deletable(),
                        parent.createdAt(),
                        parent.updatedAt(),
                        replyMap.getOrDefault(parent.id(), Collections.emptyList())
                ))
                .toList();
    }

    @Override
    public CommentDto create(
            String resourceType,
            UUID resourceId,
            UUID parentId,
            UUID authorId,
            String text,
            String mentionsJson,
            String snapshotUrl,
            String authorName,
            String authorEmail
    ) {
        Instant now = Instant.now();
        String json = (mentionsJson != null && !mentionsJson.isBlank())
                ? mentionsJson
                : Member2Constants.EMPTY_JSON_ARRAY;

        CommentEntity entity = new CommentEntity();
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setParentId(parentId);
        entity.setAuthorId(authorId);
        entity.setText(text);
        entity.setMentions(json);
        entity.setSnapshotUrl(snapshotUrl);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        Session s = session();
        s.persist(entity);
        s.flush();

        return new CommentDto(
                entity.getId(),
                resourceType,
                resourceId,
                parentId,
                authorId,
                authorName,
                authorEmail,
                text,
                parseList(json),
                snapshotUrl,
                true,
                true,
                now,
                now,
                Collections.emptyList()
        );
    }

    @Override
    public void update(UUID id, String text) {
        session().createMutationQuery(
                "UPDATE CommentEntity c SET c.text = :text, c.updatedAt = :now "
                        + "WHERE c.id = :id AND c.deletedAt IS NULL"
        )
                .setParameter("text", text)
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void delete(UUID id) {
        session().createMutationQuery("UPDATE CommentEntity c SET c.deletedAt = :now WHERE c.id = :id")
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .executeUpdate();
    }

    private CommentDto mapRow(Object[] row, UUID currentUserId, boolean isAdminOrOwner) {
        UUID authorId = HibernateSessionUtil.toUuid(row[4]);
        Instant createdAt = HibernateSessionUtil.toInstant(row[10]);
        Instant now = Instant.now();
        // BUS-SHR-11: Author can edit within 24 hours; author anytime, owner or admin can delete anytime
        boolean isAuthor = currentUserId != null && currentUserId.equals(authorId);
        boolean within24Hours = createdAt != null && createdAt.plusSeconds(24L * 3600L).isAfter(now);
        boolean editable = isAuthor && within24Hours;
        boolean deletable = isAuthor || isAdminOrOwner;

        return new CommentDto(
                HibernateSessionUtil.toUuid(row[0]),
                row[1] != null ? String.valueOf(row[1]) : null,
                HibernateSessionUtil.toUuid(row[2]),
                HibernateSessionUtil.toUuid(row[3]),
                authorId,
                row[5] != null ? String.valueOf(row[5]) : null,
                row[6] != null ? String.valueOf(row[6]) : null,
                row[7] != null ? String.valueOf(row[7]) : null,
                parseList(row[8] != null ? String.valueOf(row[8]) : null),
                row[9] != null ? String.valueOf(row[9]) : null,
                editable,
                deletable,
                createdAt,
                HibernateSessionUtil.toInstant(row[11]),
                Collections.emptyList()
        );
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse string list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
