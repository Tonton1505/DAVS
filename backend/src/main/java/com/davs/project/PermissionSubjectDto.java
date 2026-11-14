package com.davs.project;

import com.davs.permission.SubjectType;
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
