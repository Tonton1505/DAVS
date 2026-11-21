package vn.edu.davs.backend.hibernateDao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
import vn.edu.davs.backend.dao.CustomViewDao;
import vn.edu.davs.backend.dto.CustomViewDto;
import vn.edu.davs.backend.entity.CustomViewEntity;
import vn.edu.davs.backend.util.HibernateSessionUtil;

/**
 * Hibernate Session implementation of {@link CustomViewDao} for UC20.
 */
@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class HibernateCustomViewDao implements CustomViewDao {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    private Session session() {
        return HibernateSessionUtil.currentSession(entityManager);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomViewDto> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT cv.id, cv.dashboard_id, cv.name, cv.filters::text, cv.parameters::text,
                       cv.is_default, cv.is_shared, cv.owner_id, u.full_name AS owner_name,
                       cv.created_at, cv.updated_at
                FROM custom_views cv
                JOIN users u ON u.id = cv.owner_id
                WHERE cv.id = :id
                """,
                Object[].class
        )
                .setParameter("id", id)
                .getResultList();
        return rows.stream().findFirst().map(this::mapRow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomViewDto> findVisibleForUser(UUID dashboardId, UUID userId) {
        // BUS-VIZ-18: Custom View only visible to its author unless is_shared = true
        List<Object[]> rows = session().createNativeQuery(
                """
                SELECT cv.id, cv.dashboard_id, cv.name, cv.filters::text, cv.parameters::text,
                       cv.is_default, cv.is_shared, cv.owner_id, u.full_name AS owner_name,
                       cv.created_at, cv.updated_at
                FROM custom_views cv
                JOIN users u ON u.id = cv.owner_id
                WHERE cv.dashboard_id = :dashboardId AND (cv.owner_id = :userId OR cv.is_shared = TRUE)
                ORDER BY cv.is_default DESC, cv.created_at DESC
                """,
                Object[].class
        )
                .setParameter("dashboardId", dashboardId)
                .setParameter("userId", userId)
                .getResultList();
        List<CustomViewDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    @Override
    public CustomViewDto create(
            UUID dashboardId,
            String name,
            String filtersJson,
            String paramsJson,
            boolean isDefault,
            boolean isShared,
            UUID ownerId,
            String ownerName
    ) {
        Instant now = Instant.now();
        if (isDefault) {
            unsetDefault(dashboardId, ownerId);
        }
        String resolvedFilters = (filtersJson != null && !filtersJson.isBlank())
                ? filtersJson
                : Member2Constants.EMPTY_JSON_OBJECT;
        String resolvedParams = (paramsJson != null && !paramsJson.isBlank())
                ? paramsJson
                : Member2Constants.EMPTY_JSON_OBJECT;

        CustomViewEntity entity = new CustomViewEntity();
        entity.setDashboardId(dashboardId);
        entity.setName(name);
        entity.setFilters(resolvedFilters);
        entity.setParameters(resolvedParams);
        entity.setDefaultView(isDefault);
        entity.setShared(isShared);
        entity.setOwnerId(ownerId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        Session s = session();
        s.persist(entity);
        s.flush();

        return new CustomViewDto(
                entity.getId(),
                dashboardId,
                name,
                parseMap(resolvedFilters),
                parseMap(resolvedParams),
                isDefault,
                isShared,
                ownerId,
                ownerName,
                now,
                now
        );
    }

    @Override
    public void setDefault(UUID id, UUID dashboardId, UUID ownerId) {
        unsetDefault(dashboardId, ownerId);
        session().createMutationQuery(
                "UPDATE CustomViewEntity cv SET cv.defaultView = true, cv.updatedAt = :now "
                        + "WHERE cv.id = :id AND cv.ownerId = :ownerId"
        )
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .setParameter("ownerId", ownerId)
                .executeUpdate();
    }

    @Override
    public void unsetDefault(UUID dashboardId, UUID ownerId) {
        session().createMutationQuery(
                "UPDATE CustomViewEntity cv SET cv.defaultView = false, cv.updatedAt = :now "
                        + "WHERE cv.dashboardId = :dashboardId AND cv.ownerId = :ownerId"
        )
                .setParameter("now", Instant.now())
                .setParameter("dashboardId", dashboardId)
                .setParameter("ownerId", ownerId)
                .executeUpdate();
    }

    @Override
    public void setShared(UUID id, UUID ownerId, boolean shared) {
        session().createMutationQuery(
                "UPDATE CustomViewEntity cv SET cv.shared = :shared, cv.updatedAt = :now "
                        + "WHERE cv.id = :id AND cv.ownerId = :ownerId"
        )
                .setParameter("shared", shared)
                .setParameter("now", Instant.now())
                .setParameter("id", id)
                .setParameter("ownerId", ownerId)
                .executeUpdate();
    }

    @Override
    public boolean delete(UUID id, UUID ownerId) {
        return session()
                .createMutationQuery(
                        "DELETE FROM CustomViewEntity cv WHERE cv.id = :id AND cv.ownerId = :ownerId"
                )
                .setParameter("id", id)
                .setParameter("ownerId", ownerId)
                .executeUpdate() > 0;
    }

    private CustomViewDto mapRow(Object[] row) {
        String filtersJson = row[3] != null ? String.valueOf(row[3]) : null;
        String paramsJson = row[4] != null ? String.valueOf(row[4]) : null;
        return new CustomViewDto(
                HibernateSessionUtil.toUuid(row[0]),
                HibernateSessionUtil.toUuid(row[1]),
                row[2] != null ? String.valueOf(row[2]) : null,
                parseMap(filtersJson),
                parseMap(paramsJson),
                HibernateSessionUtil.toBoolean(row[5]),
                HibernateSessionUtil.toBoolean(row[6]),
                HibernateSessionUtil.toUuid(row[7]),
                row[8] != null ? String.valueOf(row[8]) : null,
                HibernateSessionUtil.toInstant(row[9]),
                HibernateSessionUtil.toInstant(row[10])
        );
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse json map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
