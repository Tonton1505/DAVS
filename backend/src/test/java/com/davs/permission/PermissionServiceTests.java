package com.davs.permission;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.security.DavsUser;
import com.davs.common.security.SiteRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionServiceTests {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID OTHER_OWNER_ID = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID GROUP_A = UUID.fromString("cccccccc-3333-3333-3333-333333333333");
    private static final UUID GROUP_B = UUID.fromString("dddddddd-4444-4444-4444-444444444444");

    private static final UUID PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID WORKBOOK_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID DASHBOARD_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID DATASOURCE_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");

    private InMemoryRuleStore ruleStore;
    private InMemoryGroupProvider groupProvider;
    private InMemoryMetadataStore metadataStore;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        ruleStore = new InMemoryRuleStore();
        groupProvider = new InMemoryGroupProvider();
        metadataStore = new InMemoryMetadataStore();

        permissionService = new PermissionService(
                ruleStore,
                groupProvider,
                List.of(
                        metadataStore.resolverFor(ResourceType.PROJECT),
                        metadataStore.resolverFor(ResourceType.WORKBOOK),
                        metadataStore.resolverFor(ResourceType.DASHBOARD),
                        metadataStore.resolverFor(ResourceType.DATASOURCE)
                )
        );
    }

    @ParameterizedTest(name = "T006_{index}_{0}")
    @MethodSource("decisionTableCases")
    void T006_decisionTableCombinations(
            String caseName,
            SiteRole role,
            Capability capability,
            boolean isOwner,
            PermissionMode projectMode,
            Effect userRule,
            Effect groupARule,
            Effect groupBRule,
            boolean expectedAllowed
    ) {
        DavsUser user = DavsUser.of(USER_ID, "user@davs.local", "Test User", role);
        groupProvider.setGroups(USER_ID, Set.of(GROUP_A, GROUP_B));

        ResourceRef projectRef = ResourceRef.project(PROJECT_ID);
        ResourceRef workbookRef = ResourceRef.workbook(WORKBOOK_ID);

        metadataStore.put(ResourceMetadata.project(PROJECT_ID, OTHER_OWNER_ID, null, projectMode));
        metadataStore.put(ResourceMetadata.of(
                workbookRef,
                isOwner ? USER_ID : OTHER_OWNER_ID,
                projectRef,
                PermissionMode.INHERIT
        ));

        ResourceRef ruleTarget = projectMode == PermissionMode.LOCKED ? projectRef : workbookRef;
        if (userRule != null) {
            ruleStore.add(PermissionRule.of(SubjectType.USER, USER_ID, ruleTarget, capability, userRule));
        }
        if (groupARule != null) {
            ruleStore.add(PermissionRule.of(SubjectType.GROUP, GROUP_A, ruleTarget, capability, groupARule));
        }
        if (groupBRule != null) {
            ruleStore.add(PermissionRule.of(SubjectType.GROUP, GROUP_B, ruleTarget, capability, groupBRule));
        }

        assertThat(permissionService.can(user, capability, workbookRef))
                .as(caseName)
                .isEqualTo(expectedAllowed);
    }

    private static Arguments row(
            String caseName,
            SiteRole role,
            Capability capability,
            boolean isOwner,
            PermissionMode mode,
            Effect userRule,
            Effect groupARule,
            Effect groupBRule,
            boolean expected
    ) {
        return Arguments.of(caseName, role, capability, isOwner, mode, userRule, groupARule, groupBRule, expected);
    }

    static Stream<Arguments> decisionTableCases() {
        return Stream.of(
                // --- VIEWER ceiling (BUS-PRJ-08): VIEWER never gets WEB_EDIT, SAVE, PUBLISH, CONNECT, DELETE ---
                row("VIEWER_VIEW_USER_ALLOW", SiteRole.VIEWER, Capability.VIEW,
                        false, PermissionMode.INHERIT, Effect.ALLOW, null, null, true),
                row("VIEWER_VIEW_GROUP_ALLOW", SiteRole.VIEWER, Capability.VIEW,
                        false, PermissionMode.INHERIT, null, Effect.ALLOW, null, true),
                row("VIEWER_VIEW_NO_RULE_DENIED", SiteRole.VIEWER, Capability.VIEW,
                        false, PermissionMode.INHERIT, null, null, null, false),
                row("VIEWER_WEB_EDIT_OWNER_AND_ALLOW_STILL_DENIED", SiteRole.VIEWER, Capability.WEB_EDIT,
                        true, PermissionMode.INHERIT, Effect.ALLOW, Effect.ALLOW, null, false),
                row("VIEWER_SAVE_OWNER_STILL_DENIED", SiteRole.VIEWER, Capability.SAVE,
                        true, PermissionMode.INHERIT, Effect.ALLOW, null, null, false),
                row("VIEWER_PUBLISH_ALLOW_STILL_DENIED", SiteRole.VIEWER, Capability.PUBLISH,
                        false, PermissionMode.INHERIT, Effect.ALLOW, null, null, false),
                row("VIEWER_DOWNLOAD_ALLOW", SiteRole.VIEWER, Capability.DOWNLOAD,
                        false, PermissionMode.INHERIT, null, Effect.ALLOW, null, true),
                row("VIEWER_COMMENT_GROUP_ALLOW_VS_USER_DENY", SiteRole.VIEWER, Capability.COMMENT,
                        false, PermissionMode.INHERIT, Effect.DENY, Effect.ALLOW, null, false),

                // --- EXPLORER: can WEB_EDIT/SAVE/SHARE, cannot PUBLISH/CONNECT/CHANGE_PERMISSIONS ---
                row("EXPLORER_WEB_EDIT_GROUP_ALLOW", SiteRole.EXPLORER, Capability.WEB_EDIT,
                        false, PermissionMode.INHERIT, null, Effect.ALLOW, null, true),
                row("EXPLORER_WEB_EDIT_NO_RULE_DENIED", SiteRole.EXPLORER, Capability.WEB_EDIT,
                        false, PermissionMode.INHERIT, null, null, null, false),
                row("EXPLORER_WEB_EDIT_OWNER_ALLOWED", SiteRole.EXPLORER, Capability.WEB_EDIT,
                        true, PermissionMode.INHERIT, null, null, null, true),
                row("EXPLORER_PUBLISH_OWNER_STILL_DENIED", SiteRole.EXPLORER, Capability.PUBLISH,
                        true, PermissionMode.INHERIT, Effect.ALLOW, null, null, false),
                row("EXPLORER_CONNECT_ALLOW_STILL_DENIED", SiteRole.EXPLORER, Capability.CONNECT,
                        false, PermissionMode.INHERIT, Effect.ALLOW, null, null, false),
                row("EXPLORER_GROUP_ALLOW_PLUS_GROUP_DENY_WINS", SiteRole.EXPLORER, Capability.WEB_EDIT,
                        false, PermissionMode.INHERIT, null, Effect.ALLOW, Effect.DENY, false),
                row("EXPLORER_USER_ALLOW_PLUS_GROUP_DENY_WINS", SiteRole.EXPLORER, Capability.SAVE,
                        false, PermissionMode.INHERIT, Effect.ALLOW, Effect.DENY, null, false),

                // --- CREATOR: full capability ceiling, subject to owner / ALLOW / DENY / LOCKED ---
                row("CREATOR_PUBLISH_OWNER_INHERIT_ALLOWED", SiteRole.CREATOR, Capability.PUBLISH,
                        true, PermissionMode.INHERIT, null, null, null, true),
                row("CREATOR_DELETE_OWNER_INHERIT_ALLOWED", SiteRole.CREATOR, Capability.DELETE,
                        true, PermissionMode.INHERIT, null, null, null, true),
                row("CREATOR_CHANGE_PERMS_OWNER_INHERIT_ALLOWED", SiteRole.CREATOR, Capability.CHANGE_PERMISSIONS,
                        true, PermissionMode.INHERIT, null, null, null, true),
                row("CREATOR_NO_RULE_NOT_OWNER_DENIED", SiteRole.CREATOR, Capability.VIEW,
                        false, PermissionMode.INHERIT, null, null, null, false),
                row("CREATOR_GROUP_ALLOW_ALLOWED", SiteRole.CREATOR, Capability.PUBLISH,
                        false, PermissionMode.INHERIT, null, Effect.ALLOW, null, true),
                row("CREATOR_DENY_BEATS_ALLOW", SiteRole.CREATOR, Capability.PUBLISH,
                        false, PermissionMode.INHERIT, Effect.ALLOW, null, Effect.DENY, false),

                // --- LOCKED project: owner loses auto-bypass (BUS-PRJ-09), child CHANGE_PERMISSIONS denied (DIS-PRJ-03) ---
                row("CREATOR_OWNER_IN_LOCKED_PROJECT_NO_RULE_DENIED", SiteRole.CREATOR, Capability.WEB_EDIT,
                        true, PermissionMode.LOCKED, null, null, null, false),
                row("CREATOR_OWNER_IN_LOCKED_PROJECT_WITH_ALLOW", SiteRole.CREATOR, Capability.WEB_EDIT,
                        true, PermissionMode.LOCKED, null, Effect.ALLOW, null, true),
                row("CREATOR_OWNER_IN_LOCKED_PROJECT_CHANGE_PERMS_DENIED", SiteRole.CREATOR, Capability.CHANGE_PERMISSIONS,
                        true, PermissionMode.LOCKED, Effect.ALLOW, Effect.ALLOW, null, false),
                row("EXPLORER_OWNER_IN_LOCKED_PROJECT_DENIED_WITHOUT_RULE", SiteRole.EXPLORER, Capability.SAVE,
                        true, PermissionMode.LOCKED, null, null, null, false),

                // --- SITE_ADMIN: implicit allow unless DENY or child CHANGE_PERMISSIONS in LOCKED project ---
                row("SITE_ADMIN_NO_RULE_ALLOWED", SiteRole.SITE_ADMIN, Capability.PUBLISH,
                        false, PermissionMode.INHERIT, null, null, null, true),
                row("SITE_ADMIN_EXPLICIT_USER_DENY_WINS", SiteRole.SITE_ADMIN, Capability.DELETE,
                        false, PermissionMode.INHERIT, Effect.DENY, null, null, false),
                row("SITE_ADMIN_EXPLICIT_GROUP_DENY_WINS", SiteRole.SITE_ADMIN, Capability.VIEW,
                        false, PermissionMode.INHERIT, Effect.ALLOW, Effect.DENY, null, false),
                row("SITE_ADMIN_LOCKED_PROJECT_CHILD_CHANGE_PERMS_DENIED", SiteRole.SITE_ADMIN, Capability.CHANGE_PERMISSIONS,
                        false, PermissionMode.LOCKED, Effect.ALLOW, null, null, false),
                row("SITE_ADMIN_LOCKED_PROJECT_VIEW_ALLOWED", SiteRole.SITE_ADMIN, Capability.VIEW,
                        false, PermissionMode.LOCKED, null, null, null, true)
        );
    }

    @Test
    void T006_requireAndRequireRoleThrowErrPrj03WhenDenied() {
        DavsUser viewer = DavsUser.of(USER_ID, "viewer@davs.local", "Viewer", SiteRole.VIEWER);
        DavsUser creator = DavsUser.of(USER_ID, "creator@davs.local", "Creator", SiteRole.CREATOR);
        ResourceRef datasource = ResourceRef.datasource(DATASOURCE_ID);

        assertThatThrownBy(() -> permissionService.require(viewer, Capability.WEB_EDIT, datasource))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_03);

        assertThatThrownBy(() -> permissionService.requireRole(viewer, SiteRole.CREATOR))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_03);

        assertThatCode(() -> permissionService.requireRole(creator, SiteRole.EXPLORER))
                .doesNotThrowAnyException();
    }

    @Test
    void T006_effectiveReturnsExactCapabilitiesRespectingSiteRoleAndInheritance() {
        DavsUser viewer = DavsUser.of(USER_ID, "viewer@davs.local", "Viewer", SiteRole.VIEWER);
        ResourceRef project = ResourceRef.project(PROJECT_ID);
        ResourceRef workbook = ResourceRef.workbook(WORKBOOK_ID);
        ResourceRef dashboard = ResourceRef.dashboard(DASHBOARD_ID);

        metadataStore.put(ResourceMetadata.project(PROJECT_ID, OTHER_OWNER_ID, null, PermissionMode.INHERIT));
        metadataStore.put(ResourceMetadata.of(workbook, USER_ID, project, PermissionMode.INHERIT));
        metadataStore.put(ResourceMetadata.of(dashboard, USER_ID, workbook, PermissionMode.INHERIT));

        Set<Capability> effective = permissionService.effective(viewer, dashboard);

        assertThat(effective).containsExactlyInAnyOrder(
                Capability.VIEW,
                Capability.INTERACT,
                Capability.FILTER,
                Capability.DOWNLOAD,
                Capability.COMMENT
        );
        assertThat(effective).doesNotContain(Capability.WEB_EDIT, Capability.SAVE, Capability.PUBLISH);
    }

    private static final class InMemoryRuleStore implements PermissionRuleStore {
        private final List<PermissionRule> rules = new ArrayList<>();

        void add(PermissionRule rule) {
            rules.add(rule);
        }

        @Override
        public List<PermissionRule> findMatchingRules(ResourceRef resource, UUID userId, Set<UUID> groupIds) {
            return rules.stream()
                    .filter(r -> r.resourceType() == resource.type() && r.resourceId().equals(resource.id()))
                    .filter(r -> (r.subjectType() == SubjectType.USER && r.subjectId().equals(userId))
                            || (r.subjectType() == SubjectType.GROUP && groupIds.contains(r.subjectId())))
                    .toList();
        }
    }

    private static final class InMemoryGroupProvider implements GroupMembershipProvider {
        private final Map<UUID, Set<UUID>> memberships = new ConcurrentHashMap<>();

        void setGroups(UUID userId, Set<UUID> groups) {
            memberships.put(userId, groups);
        }

        @Override
        public Set<UUID> findGroupIdsForUser(UUID userId) {
            return memberships.getOrDefault(userId, Set.of());
        }
    }

    private static final class InMemoryMetadataStore {
        private final Map<ResourceRef, ResourceMetadata> store = new ConcurrentHashMap<>();

        void put(ResourceMetadata metadata) {
            store.put(metadata.resource(), metadata);
        }

        ResourceMetadataResolver resolverFor(ResourceType type) {
            return new ResourceMetadataResolver() {
                @Override
                public ResourceType supports() {
                    return type;
                }

                @Override
                public Optional<ResourceMetadata> resolve(UUID resourceId) {
                    return Optional.ofNullable(store.get(new ResourceRef(type, resourceId)));
                }
            };
        }
    }
}
