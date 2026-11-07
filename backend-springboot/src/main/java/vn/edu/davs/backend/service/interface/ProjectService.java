package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.request.CreateProjectRequest;
import vn.edu.davs.backend.dto.response.ProjectResponse;

public interface ProjectService {
    ProjectResponse createProject(CreateProjectRequest request, Long currentUserId);
}
