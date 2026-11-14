package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.JdbcGroupRepository;
import vn.edu.davs.backend.dao.JdbcUserManagementRepository;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.CreateGroupRequest;
import vn.edu.davs.backend.dto.GroupDetailDto;
import vn.edu.davs.backend.dto.GroupDto;
import vn.edu.davs.backend.dto.UpdateGroupRequest;
import vn.edu.davs.backend.dto.UserManagementDto;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC04 business logic for workgroup creation, editing, deletion, member management,
 * and minimum site role license promotion.
 */
@Service
@RequiredArgsConstructor
public class GroupService {

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final String ALL_USERS_GROUP = "All Users";

    private final JdbcGroupRepository groupRepository;
    private final JdbcUserManagementRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<GroupDto> listGroups(String q) {
        return groupRepository.listGroups(q);
    }

    @Transactional(readOnly = true)
    public GroupDetailDto getGroupDetail(UUID groupId) {
        GroupDto group = groupRepository.findById(groupId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, groupId.toString()));
        List<GroupDetailDto.GroupMemberItem> members = groupRepository.listGroupMembers(groupId);
        return new GroupDetailDto(
                group.id(),
                group.name(),
                group.description(),
                group.minRole(),
                group.isSystem(),
                members.size(),
                members,
                group.createdAt(),
                group.updatedAt()
        );
    }

    @Transactional
    public GroupDetailDto createGroup(CreateGroupRequest request, DavsUser actor) {
        String normalizedName = validateAndNormalizeName(request != null ? request.name() : null, null);
        String description = normalizeDescription(request != null ? request.description() : null);
        SiteRole minRole = request != null ? request.minRole() : null;

        UUID groupId = groupRepository.insertGroup(normalizedName, description, minRole, actor.getId());

        if (request != null && request.userIds() != null && !request.userIds().isEmpty()) {
            addMembersInternal(groupId, minRole, request.userIds());
        }

        GroupDetailDto detail = getGroupDetail(groupId);
        auditService.log(AuditEvent.of("GROUP_CREATE", actor.getId(), actor.getEmail(), "GROUP", groupId.toString())
                .withStates(null, Map.of(
                        "name", detail.name(),
                        "minRole", detail.minRole() != null ? detail.minRole().name() : "NONE",
                        "memberCount", detail.memberCount()
                )));
        return detail;
    }

    @Transactional
    public GroupDetailDto updateGroup(UUID groupId, UpdateGroupRequest request, DavsUser actor) {
        GroupDto existing = groupRepository.findById(groupId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, groupId.toString()));

        String nextName = existing.name();
        if (request.name() != null) {
            if (isAllUsersGroup(existing) && !existing.name().equalsIgnoreCase(request.name().trim())) {
                throw new DavsException(ErrorCode.ERR_PRJ_03);
            }
            nextName = validateAndNormalizeName(request.name(), groupId);
        }

        String nextDesc = request.description() != null
                ? normalizeDescription(request.description())
                : existing.description();
        SiteRole nextMinRole = request.minRole() != null ? request.minRole() : existing.minRole();

        groupRepository.updateGroup(groupId, nextName, nextDesc, nextMinRole);

        // Enforce minRole upgrade on existing members if minRole was raised
        if (nextMinRole != null) {
            List<GroupDetailDto.GroupMemberItem> members = groupRepository.listGroupMembers(groupId);
            for (var member : members) {
                promoteUserRoleIfNeeded(member.userId(), nextMinRole);
            }
        }

        GroupDetailDto updated = getGroupDetail(groupId);
        auditService.log(AuditEvent.of("GROUP_UPDATE", actor.getId(), actor.getEmail(), "GROUP", groupId.toString())
                .withStates(
                        Map.of("name", existing.name()),
                        Map.of("name", updated.name())
                ));
        return updated;
    }

    @Transactional
    public GroupDetailDto addMembers(UUID groupId, List<UUID> userIds, DavsUser actor) {
        GroupDto group = groupRepository.findById(groupId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, groupId.toString()));

        addMembersInternal(groupId, group.minRole(), userIds);

        GroupDetailDto updated = getGroupDetail(groupId);
        auditService.log(AuditEvent.of("GROUP_MEMBERS_ADD", actor.getId(), actor.getEmail(), "GROUP", groupId.toString())
                .withStates(null, Map.of("addedUserCount", userIds != null ? userIds.size() : 0)));
        return updated;
    }

    @Transactional
    public GroupDetailDto removeMember(UUID groupId, UUID userId, DavsUser actor) {
        GroupDto group = groupRepository.findById(groupId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, groupId.toString()));

        // BUS-GRP-02: Cannot remove users from the default "All Users" group
        if (isAllUsersGroup(group)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        groupRepository.removeMember(groupId, userId);

        GroupDetailDto updated = getGroupDetail(groupId);
        auditService.log(AuditEvent.of("GROUP_MEMBER_REMOVE", actor.getId(), actor.getEmail(), "GROUP", groupId.toString())
                .withStates(Map.of("removedUserId", userId.toString()), null));
        return updated;
    }

    @Transactional
    public void deleteGroup(UUID groupId, DavsUser actor) {
        GroupDto group = groupRepository.findById(groupId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, groupId.toString()));

        // BUS-GRP-02: Default "All Users" group cannot be deleted
        if (isAllUsersGroup(group)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        // [5b] Delete group and revoke its permission_rules while preserving user accounts
        groupRepository.deleteGroupAndRevokePermissions(groupId);

        auditService.log(AuditEvent.of("GROUP_DELETE", actor.getId(), actor.getEmail(), "GROUP", groupId.toString())
                .withStates(Map.of("name", group.name()), null));
    }

    private void addMembersInternal(UUID groupId, SiteRole minRole, List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Set<UUID> uniqueIds = new LinkedHashSet<>(userIds);
        for (UUID userId : uniqueIds) {
            if (userId == null) {
                continue;
            }
            if (minRole != null) {
                promoteUserRoleIfNeeded(userId, minRole);
            } else {
                userRepository.findUserById(userId)
                        .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, userId.toString()));
            }
            groupRepository.addMember(groupId, userId);
        }
    }

    private void promoteUserRoleIfNeeded(UUID userId, SiteRole minRole) {
        UserManagementDto user = userRepository.findUserById(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, userId.toString()));
        if (!user.siteRole().isAtLeast(minRole)) {
            // [6] & [6a]: Upgrade role if license pool has capacity, otherwise throw ERR-USR-04
            boolean consumesLicense = !"DISABLED".equalsIgnoreCase(user.status());
            if (consumesLicense) {
                if (!userRepository.reserveLicense(minRole, 1)) {
                    throw new DavsException(ErrorCode.ERR_USR_04, minRole.name());
                }
                userRepository.releaseLicense(user.siteRole(), 1);
            }
            userRepository.updateUserRole(userId, minRole);
        }
    }

    private String validateAndNormalizeName(String rawName, UUID excludeId) {
        if (rawName == null || rawName.trim().length() < MIN_NAME_LENGTH || rawName.trim().length() > MAX_NAME_LENGTH) {
            throw new DavsException(ErrorCode.ERR_GRP_01);
        }
        String trimmed = rawName.trim();
        if (groupRepository.existsNameCaseInsensitive(trimmed, excludeId)) {
            throw new DavsException(ErrorCode.ERR_GRP_01);
        }
        return trimmed;
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > MAX_DESCRIPTION_LENGTH
                ? trimmed.substring(0, MAX_DESCRIPTION_LENGTH)
                : trimmed;
    }

    private static boolean isAllUsersGroup(GroupDto group) {
        return group.isSystem() || ALL_USERS_GROUP.equalsIgnoreCase(group.name());
    }
}
