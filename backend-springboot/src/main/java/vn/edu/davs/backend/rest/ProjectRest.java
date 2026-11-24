package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.ChangeProjectOwnerRequest;
import vn.edu.davs.backend.dto.CreateProjectRequest;
import vn.edu.davs.backend.dto.DeleteProjectRequest;
import vn.edu.davs.backend.dto.MoveProjectRequest;
import vn.edu.davs.backend.dto.ProjectDto;
import vn.edu.davs.backend.dto.ProjectTreeNodeDto;
import vn.edu.davs.backend.dto.UpdateProjectRequest;
import vn.edu.davs.backend.model.DavsUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST endpoints for UC05 Project Creation &amp; UC06 Project Management ({@code /api/v1/projects}).
 */
public interface ProjectRest {


    @GetMapping
    ResponseEntity<List<ProjectDto>> listProjects();


    @GetMapping("/tree")
    ResponseEntity<List<ProjectTreeNodeDto>> getProjectTree(
            @AuthenticationPrincipal DavsUser actor
    );


    @GetMapping("/{id}")
    ResponseEntity<ProjectDto> getProject(@PathVariable UUID id);


    @PostMapping
    ResponseEntity<ProjectDto> createProject(
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody CreateProjectRequest request
    );


    @PatchMapping("/{id}")
    ResponseEntity<ProjectDto> renameProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody UpdateProjectRequest request
    );


    @PostMapping("/{id}/move")
    ResponseEntity<ProjectDto> moveProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @RequestBody(required = false) MoveProjectRequest request
    );


    @PostMapping("/{id}/owner")
    ResponseEntity<ProjectDto> changeOwner(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @Valid @RequestBody ChangeProjectOwnerRequest request
    );


    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor,
            @RequestParam(value = "confirmName", required = false) String queryConfirmName,
            @RequestBody(required = false) DeleteProjectRequest bodyRequest
    );


    @PostMapping("/{id}/restore")
    ResponseEntity<ProjectDto> restoreProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal DavsUser actor
    );
}
