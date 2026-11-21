package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.CreateGroupRequest;
import vn.edu.davs.backend.dto.GroupDetailDto;
import vn.edu.davs.backend.dto.GroupDto;
import vn.edu.davs.backend.dto.UpdateGroupRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC04 business logic for workgroup creation, editing, deletion, member management,
 * and minimum site role license promotion.
 */
public interface GroupService {


    @Transactional(readOnly = true)
    List<GroupDto> listGroups(String q);


    @Transactional(readOnly = true)
    GroupDetailDto getGroupDetail(UUID groupId);


    @Transactional
    GroupDetailDto createGroup(CreateGroupRequest request, DavsUser actor);


    @Transactional
    GroupDetailDto updateGroup(UUID groupId, UpdateGroupRequest request, DavsUser actor);


    @Transactional
    GroupDetailDto addMembers(UUID groupId, List<UUID> userIds, DavsUser actor);


    @Transactional
    GroupDetailDto removeMember(UUID groupId, UUID userId, DavsUser actor);


    @Transactional
    void deleteGroup(UUID groupId, DavsUser actor);
}
