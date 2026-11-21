package vn.edu.davs.backend.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.dto.CommentDto;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.davs.backend.dao.ICommentDao;

@Slf4j
@RequiredArgsConstructor
@Repository
public class CommentDaoImpl implements ICommentDao {


    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public Optional<CommentDto> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT c.id, c.resource_type, c.resource_id, c.parent_id, c.author_id,
                       u.full_name AS author_name, u.email AS author_email,
                       c.text, c.mentions, c.snapshot_url, c.created_at, c.updated_at
                FROM comments c
                JOIN users u ON u.id = c.author_id
                WHERE c.id = ? AND c.deleted_at IS NULL
                """;
        List<CommentDto> list = jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs, null), id);
        return list.stream().findFirst();
    }

    @Override
    public List<CommentDto> findTreeByResource(String resourceType, UUID resourceId, UUID currentUserId, boolean isAdminOrOwner) {
        String sql = """
                SELECT c.id, c.resource_type, c.resource_id, c.parent_id, c.author_id,
                       u.full_name AS author_name, u.email AS author_email,
                       c.text, c.mentions, c.snapshot_url, c.created_at, c.updated_at
                FROM comments c
                JOIN users u ON u.id = c.author_id
                WHERE c.resource_type = ? AND c.resource_id = ? AND c.deleted_at IS NULL
                ORDER BY c.created_at ASC
                """;
        List<CommentDto> all = jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs, currentUserId, isAdminOrOwner), resourceType, resourceId);

        // Group into top-level and replies
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
                        parent.id(), parent.resourceType(), parent.resourceId(), parent.parentId(),
                        parent.authorId(), parent.authorName(), parent.authorEmail(), parent.text(),
                        parent.mentions(), parent.snapshotUrl(), parent.editable(), parent.deletable(),
                        parent.createdAt(), parent.updatedAt(),
                        replyMap.getOrDefault(parent.id(), Collections.emptyList())
                ))
                .toList();
    }

    @Override
    public CommentDto create(String resourceType, UUID resourceId, UUID parentId, UUID authorId,
                             String text, String mentionsJson, String snapshotUrl, String authorName, String authorEmail) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String json = (mentionsJson != null && !mentionsJson.isBlank()) ? mentionsJson : "[]";
        String sql = """
                INSERT INTO comments (
                    id, resource_type, resource_id, parent_id, author_id, text, mentions, snapshot_url, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, id, resourceType, resourceId, parentId, authorId, text, json, snapshotUrl, now, now);
        return new CommentDto(
                id, resourceType, resourceId, parentId, authorId, authorName, authorEmail, text,
                parseList(json), snapshotUrl, true, true, now, now, Collections.emptyList()
        );
    }

    @Override
    public void update(UUID id, String text) {
        String sql = "UPDATE comments SET text = ?, updated_at = now() WHERE id = ? AND deleted_at IS NULL";
        jdbcTemplate.update(sql, text, id);
    }

    @Override
    public void delete(UUID id) {
        String sql = "UPDATE comments SET deleted_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }


    private CommentDto mapRow(ResultSet rs, UUID currentUserId) throws SQLException {
        return mapRow(rs, currentUserId, false);
    }


    private CommentDto mapRow(ResultSet rs, UUID currentUserId, boolean isAdminOrOwner) throws SQLException {
        UUID authorId = rs.getObject("author_id", UUID.class);
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant now = Instant.now();
        // BUS-SHR-11: Author can edit within 24 hours; author anytime, owner or admin can delete anytime
        boolean isAuthor = currentUserId != null && currentUserId.equals(authorId);
        boolean within24Hours = createdAt.plusSeconds(24 * 3600).isAfter(now);
        boolean editable = isAuthor && within24Hours;
        boolean deletable = isAuthor || isAdminOrOwner;

        return new CommentDto(
                rs.getObject("id", UUID.class),
                rs.getString("resource_type"),
                rs.getObject("resource_id", UUID.class),
                rs.getObject("parent_id", UUID.class),
                authorId,
                rs.getString("author_name"),
                rs.getString("author_email"),
                rs.getString("text"),
                parseList(rs.getString("mentions")),
                rs.getString("snapshot_url"),
                editable,
                deletable,
                createdAt,
                rs.getTimestamp("updated_at").toInstant(),
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
