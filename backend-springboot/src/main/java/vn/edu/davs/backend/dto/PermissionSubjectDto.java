package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.SubjectType;
import java.util.UUID;

/**
 * Active user or group selectable in the SCR-07 permissions dialog.
 */
public record PermissionSubjectDto(
        SubjectType subjectType,
        UUID id,
        String name,
        String detail
) {
}
