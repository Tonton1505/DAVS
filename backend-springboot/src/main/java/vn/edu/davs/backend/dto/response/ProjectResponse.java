package vn.edu.davs.backend.dto.response;

import vn.edu.davs.backend.enu.PermissionTemplate;

/**
 * DTO Response cho UC05/UC06 – Tách biệt hoàn toàn với Project Entity (Rule #3).
 */
public class ProjectResponse {
    private Long id;
    private String projectName;
    private String description;
    private Long parentProjectId;
    private Integer depthLevel;
    private PermissionTemplate permissionTemplate;
    private Long ownerId;

    public ProjectResponse() {}

    public ProjectResponse(Long id, String projectName, String description, Long parentProjectId,
                           Integer depthLevel, PermissionTemplate permissionTemplate, Long ownerId) {
        this.id = id;
        this.projectName = projectName;
        this.description = description;
        this.parentProjectId = parentProjectId;
        this.depthLevel = depthLevel;
        this.permissionTemplate = permissionTemplate;
        this.ownerId = ownerId;
    }

    public Long getId() { return id; }
    public String getProjectName() { return projectName; }
    public String getDescription() { return description; }
    public Long getParentProjectId() { return parentProjectId; }
    public Integer getDepthLevel() { return depthLevel; }
    public PermissionTemplate getPermissionTemplate() { return permissionTemplate; }
    public Long getOwnerId() { return ownerId; }
}
