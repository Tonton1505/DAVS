package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.CollectionDto;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.enu.SiteRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO interface for UC05 &amp; UC06 project creation, hierarchy management, soft-delete, and collections.
 */
public interface ProjectDao {

    List<ProjectDto> listActiveProjects();

    Optional<ProjectDto> findById(UUID id);

    Optional<DeletedProjectRow> findDeletedById(UUID id);

    boolean existsSiblingName(UUID parentId, String name);

    boolean existsSiblingNameExcluding(UUID parentId, String name, UUID excludeId);

    int computeDepth(UUID projectId);

    int computeSubtreeHeight(UUID projectId);

    boolean isDescendantOf(UUID candidateId, UUID ancestorId);

    UUID insertProject(
            String name,
            String description,
            UUID parentId,
            UUID ownerId,
            String permissionMode
    );

    void updateProject(UUID id, String name, String description);

    void moveProject(UUID id, UUID targetParentId);

    void updateOwner(UUID id, UUID newOwnerId);

    void softDeleteProject(UUID id);

    void restoreProject(UUID id);

    void copyParentPermissionRules(UUID parentId, UUID childId);

    void replaceProjectRulesFromParent(UUID parentId, UUID childId);

    Optional<OwnerCandidateRow> findUserForOwnership(UUID userId);

    String findOwnerDisplayName(UUID ownerId);

    int countChildItems(UUID projectId);

    List<CollectionDto> listCollectionsByOwner(UUID ownerId);

    Optional<CollectionDto> findCollectionById(UUID collectionId);

    UUID insertCollection(String name, String description, UUID ownerId);

    void addCollectionItem(UUID collectionId, String resourceType, UUID resourceId);

    List<CollectionDto.CollectionItemDto> listCollectionItems(UUID collectionId);

    record DeletedProjectRow(ProjectDto project, Instant deletedAt) {
    }

    record OwnerCandidateRow(
            UUID id,
            String email,
            String fullName,
            SiteRole siteRole,
            String status
    ) {
    }
}
