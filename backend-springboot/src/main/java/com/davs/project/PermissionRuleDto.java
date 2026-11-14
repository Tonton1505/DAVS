package com.davs.project;

import com.davs.permission.Capability;
import com.davs.permission.Effect;
import com.davs.permission.SubjectType;
import java.util.UUID;

/**
 * Single capability rule entry returned for a resource's permission matrix (UC07).
 */
public record PermissionRuleDto(
        UUID id,
        SubjectType subjectType,
        UUID subjectId,
        String subjectName,
        String subjectDetail,
        Capability capability,
        Effect effect
) {
}
