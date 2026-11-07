package vn.edu.davs.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.davs.backend.enu.PermissionTemplate;

/**
 * DTO Request cho UC05 – Tạo dự án mới (Màn hình SCR-05).
 */
public class CreateProjectRequest {

    @NotBlank(message = "ERR-PRJ-02: Tên dự án không được để trống")
    @Size(min = 1, max = 128, message = "ERR-PRJ-02: Tên gồm 1–128 ký tự")
    @Pattern(regexp = "^[^/\\\\:*?"<>|]+$", message = "ERR-PRJ-02: Tên không được chứa / \ : * ? " < > |")
    private String projectName;

    @Size(max = 500, message = "VAL-PRJ-02: Mô tả tối đa 500 ký tự")
    private String description;

    private Long parentProjectId;

    private PermissionTemplate permissionTemplate = PermissionTemplate.INHERIT;

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getParentProjectId() { return parentProjectId; }
    public void setParentProjectId(Long parentProjectId) { this.parentProjectId = parentProjectId; }
    public PermissionTemplate getPermissionTemplate() { return permissionTemplate; }
    public void setPermissionTemplate(PermissionTemplate permissionTemplate) { this.permissionTemplate = permissionTemplate; }
}
