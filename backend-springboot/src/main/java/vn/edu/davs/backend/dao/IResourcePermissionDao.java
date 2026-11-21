package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.PermissionRuleDto;
import vn.edu.davs.backend.dto.PermissionSubjectDto;
import vn.edu.davs.backend.dto.UpdateResourcePermissionsRequest;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.enu.ResourceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository for UC07 permission rule management and active subject lookups.
 */
public interface IResourcePermissionDao {


    List<PermissionRuleDto> findRulesByResource(ResourceType type, UUID resourceId);


    void replaceRules(
            ResourceType type,
            UUID resourceId,
            List<UpdateResourcePermissionsRequest.RuleInput> rules,
            UUID actorId
    );


    Optional<UserSubjectRow> findActiveUser(UUID userId);


    boolean existsActiveGroup(UUID groupId);


    List<PermissionSubjectDto> listActiveSubjects();


    /**
     * Projection of an active user row for permission validation and effective-permission lookup.
     */
    public record UserSubjectRow(UUID id, String email, String displayName, SiteRole siteRole) {
    }
}
