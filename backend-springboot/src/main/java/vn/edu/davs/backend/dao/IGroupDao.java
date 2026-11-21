package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.GroupDetailDto;
import vn.edu.davs.backend.dto.GroupDto;
import vn.edu.davs.backend.enu.SiteRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository for UC04 workgroup management ({@code groups}, {@code group_members}).
 */
public interface IGroupDao {


    List<GroupDto> listGroups(String q);


    Optional<GroupDto> findById(UUID groupId);


    List<GroupDetailDto.GroupMemberItem> listGroupMembers(UUID groupId);


    boolean existsNameCaseInsensitive(String name, UUID excludeId);


    UUID insertGroup(String name, String description, SiteRole minRole, UUID createdBy);


    void updateGroup(UUID groupId, String name, String description, SiteRole minRole);


    boolean isMember(UUID groupId, UUID userId);


    void addMember(UUID groupId, UUID userId);


    void removeMember(UUID groupId, UUID userId);


    void deleteGroupAndRevokePermissions(UUID groupId);
}
