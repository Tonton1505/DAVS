package com.davs.permission;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Storage abstraction for querying {@code permission_rules} for a user and their groups on a resource.
 */
public interface PermissionRuleStore {

    /**
     * Returns all rules targeting {@code resource} where the subject is either
     * {@code (USER, userId)} or {@code (GROUP, groupId)} for any {@code groupId in groupIds}.
     */
    List<PermissionRule> findMatchingRules(ResourceRef resource, UUID userId, Set<UUID> groupIds);
}
