package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.EffectivePermissionsResponse;
import vn.edu.davs.backend.dto.PermissionSubjectDto;
import vn.edu.davs.backend.dto.PermissionTemplateDto;
import vn.edu.davs.backend.dto.ResourcePermissionsResponse;
import vn.edu.davs.backend.dto.UpdateResourcePermissionsRequest;
import vn.edu.davs.backend.dto.UpdateResourcePermissionsResponse;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC07 service for managing resource permission rules, role-ceiling warnings (BUS-PRJ-08),
 * DENY-over-ALLOW conflict resolution (BUS-PRJ-07), locked-project enforcement (DIS-PRJ-03),
 * and effective permission inspection.
 */
public interface ResourcePermissionService {


    @Transactional(readOnly = true)
    List<PermissionTemplateDto> listTemplates();


    @Transactional(readOnly = true)
    ResourcePermissionsResponse getPermissions(String rawType, UUID resourceId, DavsUser actor);


    @Transactional
    UpdateResourcePermissionsResponse updatePermissions(
            String rawType,
            UUID resourceId,
            UpdateResourcePermissionsRequest request,
            DavsUser actor
    );


    @Transactional(readOnly = true)
    EffectivePermissionsResponse getEffectivePermissions(
            String rawType,
            UUID resourceId,
            UUID targetUserId,
            DavsUser actor
    );


    @Transactional(readOnly = true)
    List<PermissionSubjectDto> listSubjects(String rawType, UUID resourceId, DavsUser actor);
}
