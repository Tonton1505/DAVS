package vn.edu.davs.backend.dto;

import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.Effect;
import vn.edu.davs.backend.enu.SubjectType;
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
