package vn.edu.davs.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.davs.backend.Wrapper.ApiResponse;
import vn.edu.davs.backend.dto.request.CreateProjectRequest;
import vn.edu.davs.backend.dto.response.ProjectResponse;
import vn.edu.davs.backend.service.ProjectService;

/**
 * REST Controller cho UC05 & UC06.
 * Tuân thủ Rule #1: Chỉ Parse request -> Gọi Service -> Return ApiResponse.success(data).
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId) {
        ProjectResponse data = projectService.createProject(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo dự án mới thành công", data));
    }
}
