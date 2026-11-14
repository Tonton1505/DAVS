package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.ChangeProjectOwnerRequest;
import vn.edu.davs.backend.dto.CreateProjectRequest;
import vn.edu.davs.backend.dto.DeleteProjectRequest;
import vn.edu.davs.backend.dto.MoveProjectRequest;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.dto.ProjectTreeNodeDto;
import vn.edu.davs.backend.dto.UpdateProjectRequest;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for UC05 Project Creation &amp; UC06 Project Management ({@code /api/v1/projects}).
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectDto>> listProjects() {
        return ResponseEntity.ok(projectService.listProjects());
    }

    @GetMapping("/tree")
    public ResponseEntity<List<ProjectTreeNodeDto>> getProjectTree(
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(projectService.getProjectTree(actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        ProjectDto created = projectService.createProject(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDto> renameProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ResponseEntity.ok(projectService.renameProject(id, request, actor));
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<ProjectDto> moveProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @RequestBody(required = false) MoveProjectRequest request
    ) {
        return ResponseEntity.ok(projectService.moveProject(id, request, actor));
    }

    @PostMapping("/{id}/owner")
    public ResponseEntity<ProjectDto> changeOwner(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody ChangeProjectOwnerRequest request
    ) {
        return ResponseEntity.ok(projectService.changeOwner(id, request, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "confirmName", required = false) String queryConfirmName,
            @RequestBody(required = false) DeleteProjectRequest bodyRequest
    ) {
        String confirmName = bodyRequest != null && bodyRequest.confirmName() != null
                ? bodyRequest.confirmName()
                : queryConfirmName;
        projectService.deleteProject(id, new DeleteProjectRequest(confirmName), actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ProjectDto> restoreProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(projectService.restoreProject(id, actor));
    }
}
