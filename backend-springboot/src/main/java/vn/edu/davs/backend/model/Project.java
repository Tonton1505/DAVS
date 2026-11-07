package vn.edu.davs.backend.model;

import jakarta.persistence.*;
import vn.edu.davs.backend.enu.PermissionTemplate;
import java.time.Instant;

/**
 * JPA Entity ánh xạ bảng projects phục vụ UC05 (Tạo dự án) & UC06 (Quản lý dự án).
 */
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "parent_project_id")
    private Long parentProjectId;

    @Column(nullable = false)
    private Integer depthLevel = 1; // BUS-PRJ-02: Tối đa 5 cấp

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PermissionTemplate permissionTemplate = PermissionTemplate.INHERIT;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "deleted_at")
    private Instant deletedAt; // BUS-PRJ-05: Xóa mềm 30 ngày

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getParentProjectId() { return parentProjectId; }
    public void setParentProjectId(Long parentProjectId) { this.parentProjectId = parentProjectId; }
    public Integer getDepthLevel() { return depthLevel; }
    public void setDepthLevel(Integer depthLevel) { this.depthLevel = depthLevel; }
    public PermissionTemplate getPermissionTemplate() { return permissionTemplate; }
    public void setPermissionTemplate(PermissionTemplate permissionTemplate) { this.permissionTemplate = permissionTemplate; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}
