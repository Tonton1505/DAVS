package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.AddCollectionItemRequest;
import vn.edu.davs.backend.dto.ChangeProjectOwnerRequest;
import vn.edu.davs.backend.dto.CollectionDto;
import vn.edu.davs.backend.dto.ContentListResponse;
import vn.edu.davs.backend.dto.CreateCollectionRequest;
import vn.edu.davs.backend.dto.CreateProjectRequest;
import vn.edu.davs.backend.dto.DeleteProjectRequest;
import vn.edu.davs.backend.dto.MoveProjectRequest;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.dto.ProjectTreeNodeDto;
import vn.edu.davs.backend.dto.UpdateProjectRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC05 &amp; UC06 business logic for creating, browsing, renaming, moving,
 * changing ownership, soft-deleting/restoring projects, and managing personal collections.
 */
public interface ProjectService {


    @Transactional(readOnly = true)
    List<ProjectDto> listProjects();


    @Transactional(readOnly = true)
    ProjectDto getProject(UUID id);


    @Transactional(readOnly = true)
    List<ProjectTreeNodeDto> getProjectTree(DavsUser actor);


    @Transactional(readOnly = true)
    ContentListResponse getContent(UUID projectId, String query, String sort, DavsUser actor);


    @Transactional
    ProjectDto createProject(CreateProjectRequest request, DavsUser actor);


    @Transactional
    ProjectDto renameProject(UUID id, UpdateProjectRequest request, DavsUser actor);


    @Transactional
    ProjectDto moveProject(UUID id, MoveProjectRequest request, DavsUser actor);


    @Transactional
    ProjectDto changeOwner(UUID id, ChangeProjectOwnerRequest request, DavsUser actor);


    @Transactional
    void deleteProject(UUID id, DeleteProjectRequest request, DavsUser actor);


    @Transactional
    ProjectDto restoreProject(UUID id, DavsUser actor);


    @Transactional(readOnly = true)
    List<CollectionDto> listCollections(DavsUser actor);


    @Transactional
    CollectionDto createCollection(CreateCollectionRequest request, DavsUser actor);


    @Transactional
    CollectionDto addCollectionItem(UUID collectionId, AddCollectionItemRequest request, DavsUser actor);


    boolean canPublishToProject(DavsUser actor, ProjectDto project);


    boolean canViewProject(DavsUser actor, ProjectDto project);
}
