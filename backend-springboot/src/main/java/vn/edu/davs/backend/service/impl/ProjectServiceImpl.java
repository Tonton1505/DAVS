package vn.edu.davs.backend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.dao.ProjectRepository;
import vn.edu.davs.backend.dto.request.CreateProjectRequest;
import vn.edu.davs.backend.dto.response.ProjectResponse;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.BusinessException;
import vn.edu.davs.backend.model.Project;
import vn.edu.davs.backend.service.ProjectService;

/**
 * Thực thi nghiệp vụ UC05 (Tạo dự án) & UC06 (Quản lý dự án).
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    private static final int MAX_PROJECT_DEPTH = 5; // BUS-PRJ-02
    private final ProjectRepository projectRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, Long currentUserId) {
        // 1. BUS-PRJ-01: Kiểm tra tên dự án duy nhất trong cùng dự án cha
        if (projectRepository.existsByNameIgnoreCaseAndParentProjectIdAndDeletedAtIsNull(
                request.getProjectName().trim(), request.getParentProjectId())) {
            throw new BusinessException(ErrorCode.ERR_PRJ_01, "Tên đã tồn tại trong thư mục này. Vui lòng chọn tên khác.");
        }

        // 2. BUS-PRJ-02: Kiểm tra tối đa 5 cấp dự án lồng nhau
        int depthLevel = 1;
        if (request.getParentProjectId() != null) {
            Project parent = projectRepository.findById(request.getParentProjectId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PRJ_02, "Dự án cha không tồn tại."));
            if (parent.getDepthLevel() >= MAX_PROJECT_DEPTH) {
                throw new BusinessException(ErrorCode.ERR_PRJ_02, "Vượt quá tối đa 5 cấp dự án lồng nhau (BUS-PRJ-02).");
            }
            depthLevel = parent.getDepthLevel() + 1;
        }

        // 3. Lưu Entity vào PostgreSQL
        Project entity = new Project();
        entity.setName(request.getProjectName().trim());
        entity.setDescription(request.getDescription());
        entity.setParentProjectId(request.getParentProjectId());
        entity.setDepthLevel(depthLevel);
        entity.setPermissionTemplate(request.getPermissionTemplate());
        entity.setOwnerId(currentUserId);

        Project saved = projectRepository.save(entity);

        // 4. Rule #3: Map Entity sang Response DTO, không trả Entity trực tiếp
        return new ProjectResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getParentProjectId(),
                saved.getDepthLevel(),
                saved.getPermissionTemplate(),
                saved.getOwnerId()
        );
    }
}
