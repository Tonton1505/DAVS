package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.GroupMembershipProvider;
import vn.edu.davs.backend.dao.PermissionRuleStore;
import vn.edu.davs.backend.dao.ResourceMetadataResolver;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.Effect;
import vn.edu.davs.backend.enu.PermissionMode;
import vn.edu.davs.backend.enu.ResourceType;
import vn.edu.davs.backend.model.PermissionRule;
import vn.edu.davs.backend.model.ResourceMetadata;
import vn.edu.davs.backend.model.ResourceRef;

import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.enu.SiteRole;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Central RBAC + resource-level policy evaluation engine (T-006, context.md section 6).
 *
 * <p>Evaluation order:
 * <ol>
 *   <li>SiteRole capability ceiling (e.g. VIEWER never gets WEB_EDIT per BUS-PRJ-08)</li>
 *   <li>Parent project LOCKED constraints (DIS-PRJ-03 &amp; BUS-PRJ-09)</li>
 *   <li>Resource ownership (unless locked by parent project)</li>
 *   <li>Explicit DENY rules (user + groups) — DENY always beats ALLOW</li>
 *   <li>Explicit ALLOW rules (user + groups) or SITE_ADMIN role</li>
 *   <li>Default deny</li>
 * </ol>
 */
@Service
public class PermissionService {

    private static final int MAX_PARENT_DEPTH = 5;

    private final PermissionRuleStore ruleStore;
    private final GroupMembershipProvider groupMembershipProvider;
    private final Map<ResourceType, ResourceMetadataResolver> resolvers = new ConcurrentHashMap<>();

    @Autowired
    public PermissionService(
            PermissionRuleStore ruleStore,
            GroupMembershipProvider groupMembershipProvider,
            List<ResourceMetadataResolver> resolverBeans
    ) {
        this.ruleStore = ruleStore;
        this.groupMembershipProvider = groupMembershipProvider;
        if (resolverBeans != null) {
            resolverBeans.forEach(this::registerResolver);
        }
    }

    /**
     * Allows domain modules or tests to register a {@link ResourceMetadataResolver} dynamically.
     */
    public void registerResolver(ResourceMetadataResolver resolver) {
        if (resolver != null && resolver.supports() != null) {
            resolvers.put(resolver.supports(), resolver);
        }
    }

    /**
     * Asserts that {@code user} holds {@code capability} on {@code resource}; otherwise throws
     * {@link DavsException} with {@link ErrorCode#ERR_PRJ_03} (HTTP 403).
     */
    public void require(DavsUser user, Capability capability, ResourceRef resource) {
        if (!can(user, capability, resource)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
    }

    /**
     * Asserts that {@code user} has at least {@code minimumRole}; otherwise throws
     * {@link DavsException} with {@link ErrorCode#ERR_PRJ_03} (HTTP 403).
     */
    public void requireRole(DavsUser user, SiteRole minimumRole) {
        if (user == null || minimumRole == null || !user.getSiteRole().isAtLeast(minimumRole)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
    }

    /**
     * Evaluates whether {@code user} has {@code capability} on {@code resource}.
     */
    public boolean can(DavsUser user, Capability capability, ResourceRef resource) {
        if (user == null || capability == null || resource == null) {
            return false;
        }
        if (!user.getSiteRole().supports(capability)) {
            return false;
        }
        EvaluationContext ctx = buildContext(user, resource);
        return evaluateCapability(user, capability, resource, ctx);
    }

    /**
     * Computes the full effective set of {@link Capability} values granted to {@code user} on {@code resource}.
     */
    public Set<Capability> effective(DavsUser user, ResourceRef resource) {
        if (user == null || resource == null) {
            return Set.of();
        }
        EvaluationContext ctx = buildContext(user, resource);
        EnumSet<Capability> granted = EnumSet.noneOf(Capability.class);
        for (Capability capability : Capability.values()) {
            if (user.getSiteRole().supports(capability)
                    && evaluateCapability(user, capability, resource, ctx)) {
                granted.add(capability);
            }
        }
        return Collections.unmodifiableSet(granted);
    }

    private boolean evaluateCapability(
            DavsUser user,
            Capability capability,
            ResourceRef resource,
            EvaluationContext ctx
    ) {
        // DIS-PRJ-03: Child resources inside a LOCKED project cannot have their own permissions changed
        if (ctx.lockedByParentProject
                && resource.type() != ResourceType.PROJECT
                && capability == Capability.CHANGE_PERMISSIONS) {
            return false;
        }

        // BUS-PRJ-09: Owner has full capabilities within SiteRole unless locked by parent project
        if (!ctx.lockedByParentProject && ctx.isOwner) {
            return true;
        }

        List<PermissionRule> matchingRules = ctx.rulesByCapability.getOrDefault(capability, List.of());
        boolean hasDeny = false;
        boolean hasAllow = false;
        for (PermissionRule rule : matchingRules) {
            if (rule.effect() == Effect.DENY) {
                hasDeny = true;
                break;
            }
            if (rule.effect() == Effect.ALLOW) {
                hasAllow = true;
            }
        }

        // BUS-PRJ-07 / BUS-GRP-03: DENY always overrides ALLOW and admin default
        if (hasDeny) {
            return false;
        }
        if (hasAllow) {
            return true;
        }
        return user.getSiteRole() == SiteRole.SITE_ADMIN;
    }

    private EvaluationContext buildContext(DavsUser user, ResourceRef resource) {
        ResourceMetadata targetMeta = resolveMetadata(resource).orElse(null);
        boolean isOwner = targetMeta != null && user.getId().equals(targetMeta.ownerId());

        List<ResourceRef> effectiveScope = new ArrayList<>();
        boolean lockedByParentProject = false;
        ResourceRef lockingProject = null;

        // Walk parent chain to detect LOCKED enclosing project and INHERIT chain
        List<ResourceRef> inheritChain = new ArrayList<>();
        inheritChain.add(resource);

        Set<ResourceRef> visited = new HashSet<>();
        visited.add(resource);

        ResourceMetadata current = targetMeta;
        boolean continueInheriting = targetMeta == null || targetMeta.permissionMode() == PermissionMode.INHERIT;

        for (int depth = 0; depth < MAX_PARENT_DEPTH && current != null && current.parent() != null; depth++) {
            ResourceRef parentRef = current.parent();
            if (!visited.add(parentRef)) {
                break;
            }
            ResourceMetadata parentMeta = resolveMetadata(parentRef).orElse(null);
            if (parentRef.type() == ResourceType.PROJECT
                    && parentMeta != null
                    && parentMeta.permissionMode() == PermissionMode.LOCKED) {
                lockedByParentProject = true;
                lockingProject = parentRef;
            }
            if (continueInheriting) {
                inheritChain.add(parentRef);
                continueInheriting = parentMeta == null || parentMeta.permissionMode() == PermissionMode.INHERIT;
            }
            current = parentMeta;
        }

        if (lockedByParentProject && lockingProject != null) {
            effectiveScope.add(lockingProject);
        } else {
            effectiveScope.addAll(inheritChain);
        }

        Set<UUID> groupIds = groupMembershipProvider.findGroupIdsForUser(user.getId());
        Map<Capability, List<PermissionRule>> rulesByCapability = new EnumMap<>(Capability.class);
        for (ResourceRef scopeResource : effectiveScope) {
            List<PermissionRule> rules = ruleStore.findMatchingRules(scopeResource, user.getId(), groupIds);
            for (PermissionRule rule : rules) {
                rulesByCapability
                        .computeIfAbsent(rule.capability(), ignored -> new ArrayList<>())
                        .add(rule);
            }
        }

        return new EvaluationContext(isOwner, lockedByParentProject, rulesByCapability);
    }

    private Optional<ResourceMetadata> resolveMetadata(ResourceRef resource) {
        ResourceMetadataResolver resolver = resolvers.get(resource.type());
        if (resolver == null) {
            return Optional.empty();
        }
        return resolver.resolve(resource.id());
    }

    private record EvaluationContext(
            boolean isOwner,
            boolean lockedByParentProject,
            Map<Capability, List<PermissionRule>> rulesByCapability
    ) {
    }
}
